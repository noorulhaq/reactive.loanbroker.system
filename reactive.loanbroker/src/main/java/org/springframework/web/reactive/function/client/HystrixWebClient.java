package org.springframework.web.reactive.function.client;

import reactor.core.publisher.Mono;
import static net.javacrumbs.futureconverter.java8rx.FutureConverter.*;

/**
 * Created by noor on 1/7/17.
 */
public class HystrixWebClient implements WebClient {

    private WebClient delegate;

    private HystrixWebClient(WebClient webClient){
        this.delegate = webClient;
    }

    public static HystrixWebClient create(WebClient webClient){
        return new HystrixWebClient(webClient);
    }

    protected WebClient getDelegate() {
        return delegate;
    }

    @Override
    public Mono<ClientResponse> exchange(ClientRequest<?> request) {
        ExchangeCommandBuilder commandBuilder = new ExchangeCommandBuilder(this);
        return exchange(commandBuilder.build(request));
    }

    public ExchangeCommandBuilder exchangeCommandBuilder() {
        return new ExchangeCommandBuilder(this);
    }

    protected Mono<ClientResponse> exchange(ExchangeCommand command) {
        return Mono.fromFuture(toCompletableFuture(command.toObservable().toSingle()));
    }

    @Override
    public WebClient filter(ExchangeFilterFunction filterFunction) {
        return delegate.filter(filterFunction);
    }
}


