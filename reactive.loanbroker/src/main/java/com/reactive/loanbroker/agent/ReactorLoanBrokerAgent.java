package com.reactive.loanbroker.agent;

import com.reactive.loanbroker.api.ReactiveBankServiceLocator;
import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.model.BestQuotationResponse;
import com.reactive.loanbroker.service.LoanRequestService;
import com.reactive.loanbroker.service.SpringReactorLoanRequestService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class ReactorLoanBrokerAgent extends ReactiveLoanBrokerAgent {


    private SpringReactorLoanRequestService loanRequestService;

    @Autowired
    public ReactorLoanBrokerAgent(SpringReactorLoanRequestService loanRequestService,
                                  ReactiveBankServiceLocator bankServiceLocator){
        super(bankServiceLocator);
        this.loanRequestService = loanRequestService;
    }

    public Mono<BestQuotationResponse> findBestQuotation(Publisher<String> banksURL,Double loanAmount){

        BestQuotationResponse bestQuotationResponse = new BestQuotationResponse(loanAmount);
        bestQuotationResponse.bestOffer(LoanRequestService.OFFER_IN_CASE_OF_ERROR);

        return Flux.from(banksURL)
                .flatMap(bankURL -> loanRequestService.requestForQuotation(bankURL, loanAmount)) // Scatter
                .collect(()->new BestQuotationResponse(loanAmount), BestQuotationResponse::offer) // Gather
                .doOnSuccess(BestQuotationResponse::finish)
                .flatMap(bqr -> Mono.justOrEmpty(selectBestQuotation(bqr.getOffers())).map(bestQuotation -> { bqr.bestOffer(bestQuotation); return bqr;}))
                .onErrorReturn(bestQuotationResponse).singleOrEmpty();
    }
}
