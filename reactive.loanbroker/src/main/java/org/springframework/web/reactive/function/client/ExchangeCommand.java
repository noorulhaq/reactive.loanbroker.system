package org.springframework.web.reactive.function.client;

import com.netflix.hystrix.HystrixObservableCommand;
import reactor.core.publisher.Mono;
import rx.Observable;
import static net.javacrumbs.futureconverter.java8rx.FutureConverter.toSingle;

/**
 * Created by noor on 1/7/17.
 */
class ExchangeCommand extends HystrixObservableCommand<ClientResponse> {

    private ClientRequest<?> request;
    private WebClient webClient;

    protected ExchangeCommand(WebClient webClient,ClientRequest<?> request, Setter setter) {
        super(setter);
        this.request = request;
        this.webClient = webClient;
    }

    @Override
    protected Observable<ClientResponse> construct() {

        Mono offer = webClient
                .exchange(request)
                .timeoutMillis(getProperties().executionTimeoutInMilliseconds().get()); // Response should come back within executionTimeoutInMilliseconds

        return toSingle(offer.toFuture()).toObservable();
    }
}
