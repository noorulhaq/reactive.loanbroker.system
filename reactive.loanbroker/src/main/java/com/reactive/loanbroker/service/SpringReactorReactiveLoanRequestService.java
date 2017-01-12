package com.reactive.loanbroker.service;


import com.reactive.loanbroker.model.Quotation;
import static net.javacrumbs.futureconverter.java8rx.FutureConverter.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.HystrixWebClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import rx.Observable;

import java.util.concurrent.TimeoutException;

/**
 * Created by noor on 12/26/16.
 */

@Service
public class SpringReactorReactiveLoanRequestService implements ReactiveLoanRequestService {


    private WebClient webClient;

    private HystrixWebClient hystrixWebClient;

    @Autowired
    public SpringReactorReactiveLoanRequestService(WebClient webClient){
        this.webClient = webClient;
        hystrixWebClient = HystrixWebClient.create(webClient);
    }

    @Override
    public Mono<Quotation> requestForQuotation(String bankURL, Double loanAmount) {
        return requestForQuotationWithHystrixWebClient(bankURL,loanAmount);
    }

    private Mono<Quotation> requestForQuotationWithHystrixWebClient(String bankURL, Double loanAmount) {

        ClientRequest<Void> request = ClientRequest.GET(bankURL.concat(REQUEST_QUOTATION_PATH), loanAmount)
                .accept(MediaType.APPLICATION_JSON)
                .build();

        return hystrixWebClient.exchangeCommandBuilder()
                .withCommandName(bankURL)
                .withErrorThresholdPercentage(50)
                .withRequestVolumeThreshold(5)
                .withSleepWindowInMilliseconds(5000)
                .withExecutionTimeoutInMilliseconds(1500)
                .exchange(request)
                .then(response -> response.bodyToMono(Quotation.class))
                .otherwiseReturn(OFFER_IN_CASE_OF_ERROR);
    }


    private Mono<Quotation> requestForQuotationWithoutCB(String bankURL, Double loanAmount) {

        ClientRequest<Void> request = ClientRequest.GET(bankURL.concat(REQUEST_QUOTATION_PATH), loanAmount)
                .accept(MediaType.APPLICATION_JSON)
                .build();

        Mono<Quotation> offer = webClient.exchange(request)
                .timeout(REQUEST_TIMEOUT) // Response should come back within REQUEST_TIMEOUT
                .then(response -> response.bodyToMono(Quotation.class))
                .otherwiseReturn(OFFER_IN_CASE_OF_ERROR); // Fallback strategy

        return offer;
    }

    private Mono<Quotation> requestForQuotationWithCB(String bankURL, Double loanAmount) {
        Command command = new Command(bankURL,loanAmount);
        return Mono.fromFuture(toCompletableFuture(command.toObservable().toSingle()));
    }


    class Command extends LoanQuotationCommand {

        Command(String bankURL, Double loanAmount) {
            super(bankURL, loanAmount ,OFFER_IN_CASE_OF_ERROR);
        }

        @Override
        protected Observable<Quotation> construct() {

            ClientRequest<Void> request = ClientRequest.GET(getBankURL().concat(REQUEST_QUOTATION_PATH), getLoanAmount())
                    .accept(MediaType.APPLICATION_JSON)
                    .build();

            Mono<Quotation> offer = webClient
                    .exchange(request)
                    .timeout(REQUEST_TIMEOUT) // Response should come back within REQUEST_TIMEOUT
                    .then(response -> response.bodyToMono(Quotation.class))
                    .otherwiseReturn(throwable -> !(throwable instanceof TimeoutException), OFFER_IN_CASE_OF_ERROR); // Fallback strategy

            return toSingle(offer.toFuture()).toObservable();
        }
    }
}
