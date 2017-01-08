package com.reactive.loanbroker.api;

import com.reactive.loanbroker.model.Quotation;
import com.reactive.loanbroker.model.BestQuotationResponse;
import org.reactivestreams.Publisher;
import java.util.List;
import java.util.Optional;


/*

Loan broker

*/

public abstract class ReactiveLoanBrokerAgent {

    private ReactiveBankServiceLocator bankServiceLocator;

    public ReactiveLoanBrokerAgent(ReactiveBankServiceLocator bankServiceLocator){
        this.bankServiceLocator = bankServiceLocator;
    }

    public Publisher<BestQuotationResponse> findBestQuotation(Double loanAmount){
        return findBestQuotation(bankServiceLocator.banksURL(),loanAmount);
    }

    protected Optional<Quotation> selectBestQuotation(List<Quotation> quotations){
        return  Optional.ofNullable(quotations)
                .flatMap( _quotations -> _quotations.stream().sorted((q1, q2) -> (q1.getOffer() > q2.getOffer() ? 1:-1))
                        .findFirst());
    }

    protected abstract Publisher<BestQuotationResponse> findBestQuotation(Publisher<String> banksURL, Double loanAmount);

}
