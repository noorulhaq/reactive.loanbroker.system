package com.reactive.loanbroker.agent;

import com.reactive.loanbroker.api.ReactiveBankServiceLocator;
import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.model.BestQuotationResponse;
import com.reactive.loanbroker.model.Quotation;
import com.reactive.loanbroker.service.NettyRxReactiveLoanRequestService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Created by Noor on 12/26/16.
 */

@Service
public class HybridLoanBrokerAgent extends ReactiveLoanBrokerAgent {

    @Autowired
    private NettyRxReactiveLoanRequestService loanRequestService;


    @Autowired
    public HybridLoanBrokerAgent(ReactiveBankServiceLocator bankServiceLocator){
        super(bankServiceLocator);
    }


    @Override
    public Mono<BestQuotationResponse> findBestQuotation(Publisher<String> banksURL, Double loanAmount){

        return Flux.from(banksURL)
                .flatMap(bankURL -> loanRequestService.requestForQuotation(bankURL, loanAmount))  // Scatter
                .collect(()->new BestQuotationResponse(loanAmount), BestQuotationResponse::offer) // Gather
                .doOnSuccess(BestQuotationResponse::finish)
                .flatMap(bqr -> Mono.justOrEmpty(selectBestQuotation(bqr.getOffers())).map(bestQuotation -> { bqr.bestOffer(bestQuotation); return bqr;})).singleOrEmpty();

    }

    @Override
    protected Publisher<Quotation> requestForQuotation(String bankURL, Double loanAmount) {
        return null;
    }


}
