package com.reactive.loanbroker.controller;

import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.model.BestQuotationResponse;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
public class LoanBrokerController {


    @Autowired
    @Qualifier("reactorLoanBrokerAgent")
    private ReactiveLoanBrokerAgent reactorLoanBrokerAgent;

    @Autowired
    @Qualifier("hybridLoanBrokerAgent")
    private ReactiveLoanBrokerAgent hybridLoanBrokerAgent;

    @Autowired
    @Qualifier("rxLoanBrokerAgent")
    private ReactiveLoanBrokerAgent rxLoanBrokerAgent;


    @RequestMapping("/reactor/quotation")
    public Mono<BestQuotationResponse> reactorBestQuotation(final @RequestParam(value="loanAmount", required=true) Double loanAmount){

       return  Mono.from(reactorLoanBrokerAgent.findBestQuotation(loanAmount))
                .filter(quotation ->
                        {
                           return quotation.getRequestedLoanAmount().equals(loanAmount);
                        }
                )
                .otherwiseIfEmpty(Mono.error(new IllegalStateException("Returned amount does not match with best offer")));
    }


    @RequestMapping("/hybrid/quotation")
    public Mono<BestQuotationResponse> hybridBestQuotation(final @RequestParam(value="loanAmount", required=true) Double loanAmount){

        return Mono.from(hybridLoanBrokerAgent.findBestQuotation(loanAmount))
                .filter(quotation ->
                        {
                            return quotation.getRequestedLoanAmount().equals(loanAmount);
                        }
                )
                .otherwiseIfEmpty(Mono.error(new IllegalStateException("Returned amount does not match with best offer")));
    }


    @RequestMapping("/rx/quotation")
    public Single<BestQuotationResponse> rxBestQuotation(final @RequestParam(value="loanAmount", required=true) Double loanAmount){

        return  Flowable.fromPublisher(rxLoanBrokerAgent.findBestQuotation(loanAmount))
                .filter(quotation ->
                        {
                            return quotation.getRequestedLoanAmount().equals(loanAmount);
                        }
                ).singleOrError();
    }



}
