package com.reactive.loanbroker.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.ObservableExecutionMode;
import com.reactive.loanbroker.model.Quotation;
import io.reactivex.Flowable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Created by husainbasrawala on 12/26/16.
 */

@Service
public class NettyRxReactiveLoanRequestService implements ReactiveLoanRequestService {

    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    @Override
    public Flowable<Quotation> requestForQuotation(String bankUrl, Double loanAmount) {
        return Flowable.fromFuture(requestForQuotation2(bankUrl,loanAmount).toBlocking().toFuture());
    }

    @HystrixCommand(observableExecutionMode = ObservableExecutionMode.LAZY, fallbackMethod = "fallbackQuotation")
    public rx.Observable<Quotation> requestForQuotation2(String bankUrl, Double loanAmount) {

        //System.err.println(OFFER_IN_CASE_OF_ERROR);
        //return new CommandHelloWorld(bankUrl,loanAmount).toObservable();

        return rx.Single.from(restRequestForQuotation(bankUrl,loanAmount))
                .timeout(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .map(responseEntity -> responseEntity.getBody()).toObservable();
    }

    private rx.Observable<Quotation> fallbackQuotation(String bankUrl, Double loanAmount) {
        //System.err.println(OFFER_IN_CASE_OF_ERROR);
        return rx.Observable.just(OFFER_IN_CASE_OF_ERROR);
    }

//    public class CommandHelloWorld extends HystrixObservableCommand<Quotation> {
//
//        private String bankUrl;
//        private Double loanAmount;
//
//        protected CommandHelloWorld(String bankUrl, Double loanAmount) {
//            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup")).andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
//                    .withCircuitBreakerEnabled(true).withExecutionTimeoutInMilliseconds(1000).withExecutionIsolationSemaphoreMaxConcurrentRequests(2)));
//            this.bankUrl = bankUrl;
//            this.loanAmount = loanAmount;
//        }
//
//        @Override
//        protected rx.Observable<Quotation> construct() {
//            return rx.Single.from(restRequestForQuotation(bankUrl,loanAmount))
//                    .timeout(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
//                    .map(responseEntity -> responseEntity.getBody()).toObservable();
//        }
//
//
//
//        @Override
//        protected rx.Observable<Quotation> resumeWithFallback() {
//            System.err.println(OFFER_IN_CASE_OF_ERROR);
//            return rx.Observable.just(OFFER_IN_CASE_OF_ERROR);
//        }
//    }



    private ListenableFuture<ResponseEntity<Quotation>> restRequestForQuotation(String bankUrl, Double loanAmount){

        RequestEntity requestEntity =  RequestEntity.get(URI.create(bankUrl)).accept(MediaType.APPLICATION_JSON).build();
        return asyncRestTemplate.exchange(bankUrl.concat(REQUEST_QUOTATION_PATH), requestEntity.getMethod(),requestEntity, Quotation.class,loanAmount);
    }


}
