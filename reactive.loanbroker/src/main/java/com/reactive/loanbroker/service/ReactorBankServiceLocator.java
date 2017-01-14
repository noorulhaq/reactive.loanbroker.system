package com.reactive.loanbroker.service;

import com.reactive.loanbroker.api.ReactiveBankServiceLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.stream.Stream;

/**
 * Created by Noor on 12/30/16.
 */

@Service
public class ReactorBankServiceLocator implements ReactiveBankServiceLocator {

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private DiscoveryClient discoveryClient;


    private Stream<String> banksServiceIdStream(){
        return discoveryClient.getServices().stream().filter(service -> service.startsWith("bank"));
    }

    public Flux<String> banksServiceId(){
        return Flux.defer(() -> Flux.fromStream(banksServiceIdStream()));
    }

    @Override
    public Flux<String> banksURL(){

        return Flux.defer(() -> Flux.fromStream(banksServiceIdStream().map(service -> loadBalancerClient.choose(service))
                        .map(serviceInstance->serviceInstance.getUri().toString()+"/"+serviceInstance.getServiceId())));
    }


}
