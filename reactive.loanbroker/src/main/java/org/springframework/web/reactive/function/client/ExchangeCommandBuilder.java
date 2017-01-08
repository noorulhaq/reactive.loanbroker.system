package org.springframework.web.reactive.function.client;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import reactor.core.publisher.Mono;

/**
 * Created by noor on 1/7/17.
 */
public class ExchangeCommandBuilder {

    private HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
    private String groupKeyName = "webClientGroup";
    private String commandName = "exchangeCommand";
    private HystrixWebClient hystrixWebClient;

    ExchangeCommandBuilder(HystrixWebClient hystrixWebClient){
        this.hystrixWebClient = hystrixWebClient;
    }

    public ExchangeCommandBuilder withGroupKeyName(String groupKeyName){
        this.groupKeyName = groupKeyName;
        return this;
    }

    public ExchangeCommandBuilder withCommandName(String commandName){
        this.commandName = commandName;
        return this;
    }

    public ExchangeCommandBuilder withRequestVolumeThreshold(int requestVolumeThreshold) {
        commandProperties.withCircuitBreakerRequestVolumeThreshold(requestVolumeThreshold);
        return this;
    }

    public ExchangeCommandBuilder withErrorThresholdPercentage(int errorThresholdPercentage) {
        commandProperties.withCircuitBreakerErrorThresholdPercentage(errorThresholdPercentage);
        return this;
    }

    public ExchangeCommandBuilder withSleepWindowInMilliseconds(int sleepWindowInMilliseconds) {
        commandProperties.withCircuitBreakerSleepWindowInMilliseconds(sleepWindowInMilliseconds);
        return this;
    }

    public ExchangeCommandBuilder withExecutionTimeoutInMilliseconds(int requestTimeoutMilliseconds) {
        commandProperties.withExecutionTimeoutInMilliseconds(requestTimeoutMilliseconds);
        return this;
    }

    protected ExchangeCommand build(ClientRequest<?> request){

        HystrixObservableCommand.Setter setter =  HystrixObservableCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKeyName))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandName))
                .andCommandPropertiesDefaults(commandProperties
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
                        .withFallbackEnabled(false));

        return new ExchangeCommand(hystrixWebClient.getDelegate(),request,setter);
    }

    public Mono<ClientResponse> exchange(ClientRequest<?> request){
        return hystrixWebClient.exchange(build(request));
    }
}
