package com.reactive.loanbroker.agent;

import com.reactive.loanbroker.api.ReactiveBankServiceLocator;
import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.model.BestQuotationResponse;
import com.reactive.loanbroker.model.Quotation;
import com.reactive.loanbroker.service.NettyRxReactiveLoanRequestService;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;


@Service
public class RxLoanBrokerAgent extends ReactiveLoanBrokerAgent {

    @Autowired
    private NettyRxReactiveLoanRequestService nettyRxLoanRequestService;

    @Autowired
    public RxLoanBrokerAgent(ReactiveBankServiceLocator bankServiceLocator){
        super(bankServiceLocator);
    }

    public Flowable<BestQuotationResponse> findBestQuotation(Publisher<String> banksURL, Double loanAmount){

          return Flowable.fromPublisher(banksURL).flatMap(bankUrl -> nettyRxLoanRequestService.requestForQuotation(bankUrl,loanAmount))
                 .collect(()->new BestQuotationResponse(loanAmount), BestQuotationResponse::offer)
                 .doOnSuccess(BestQuotationResponse::finish)
                 .flatMap(bqr -> justOrEmpty(selectBestQuotation(bqr.getOffers()).map(bestQuotation -> { bqr.bestOffer(bestQuotation); return bqr;})))
                 .toFlowable();

    }

    @Override
    protected Publisher<Quotation> requestForQuotation(String bankURL, Double loanAmount) {
        return null;
    }

    private Single justOrEmpty(Optional data){
         return data != null && data.isPresent() ? Single.just(data.get()) : Single.never();
    }

}
