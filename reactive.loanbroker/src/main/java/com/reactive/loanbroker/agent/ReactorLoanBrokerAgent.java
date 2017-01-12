package com.reactive.loanbroker.agent;

import com.reactive.loanbroker.api.ReactiveBankServiceLocator;
import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.model.BestQuotationResponse;
import com.reactive.loanbroker.model.Quotation;
import com.reactive.loanbroker.service.ReactiveLoanRequestService;
import com.reactive.loanbroker.service.SpringReactorReactiveLoanRequestService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.HystrixWebClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;


@Service
public class ReactorLoanBrokerAgent extends ReactiveLoanBrokerAgent{

    private SpringReactorReactiveLoanRequestService loanRequestService;
    private HystrixWebClient hystrixWebClient;
    private static final String REQUEST_QUOTATION_PATH = "/quotation?loanAmount={loanAmount}";


    @Autowired
    public ReactorLoanBrokerAgent(ReactiveBankServiceLocator bankServiceLocator,
                                  WebClient webClient){
        super(bankServiceLocator);
        hystrixWebClient = HystrixWebClient.create(webClient);
    }

    public Mono<BestQuotationResponse> findBestQuotation(Publisher<String> banksURL,Double loanAmount){

        return Flux.from(banksURL)
                .flatMap(bankURL -> requestForQuotation(bankURL, loanAmount)
                        .otherwiseReturn(ReactiveLoanRequestService.OFFER_IN_CASE_OF_ERROR)) // Scatter
                .filter(offer -> !offer.equals(ReactiveLoanRequestService.OFFER_IN_CASE_OF_ERROR))
                .collect(()->new BestQuotationResponse(loanAmount), BestQuotationResponse::offer) // Gather
                .doOnSuccess(BestQuotationResponse::finish)
                .flatMap(bqr -> Mono.justOrEmpty(selectBestQuotation(bqr.getOffers()))
                        .map(bestQuotation -> { bqr.bestOffer(bestQuotation); return bqr;}))
                .timeout(Duration.ofMillis(3000))
                .singleOrEmpty();
    }

    @Override
    protected Mono<Quotation> requestForQuotation(String bankURL, Double loanAmount) {
        ClientRequest<Void> request = ClientRequest.GET(bankURL.concat(REQUEST_QUOTATION_PATH), loanAmount)
                .accept(MediaType.APPLICATION_JSON)
                .build();

        return hystrixWebClient.exchangeCommandBuilder()
                .withCommandName(bankURL)
                .withErrorThresholdPercentage(50)
                .withRequestVolumeThreshold(5)
                .withSleepWindowInMilliseconds(5000)
                .withExecutionTimeoutInMilliseconds(Long.valueOf(REQUEST_TIMEOUT.toMillis()).intValue())
                .exchange(request)
                .then(response -> response.bodyToMono(Quotation.class));
    }
}
