package com.reactive.loanbroker.handler;

import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.handler.error.Errors;
import com.reactive.loanbroker.model.BestQuotationResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import java.util.concurrent.TimeoutException;

/**
 * Created by husainbasrawala on 1/8/17.
 */
public class LoanBrokerHandler {


    private ReactiveLoanBrokerAgent reactorLoanBrokerAgent;

    public LoanBrokerHandler(ReactiveLoanBrokerAgent reactorLoanBrokerAgent) {
        this.reactorLoanBrokerAgent = reactorLoanBrokerAgent;
    }

    public Mono<ServerResponse> bestQuotation(ServerRequest request) {

        return request.queryParam("loanAmount").map((loanAmountParam) -> Mono.just(loanAmountParam)
                .map(Double::valueOf)
                .then((loanAmount) -> Mono.from( reactorLoanBrokerAgent.findBestQuotation(loanAmount))
                        .then(quotation -> quotation.getRequestedLoanAmount().equals(loanAmount) ?
                                Mono.just(quotation) : Mono.error(new IllegalStateException("Returned amount does not match with the best offer"))).log()
                )
                .flatMap(bestQuotationResponse -> ServerResponse.ok().body(Mono.just(bestQuotationResponse), BestQuotationResponse.class)).log()
                .switchIfEmpty(Errors.noBankServiceAvailable())
                .onErrorResumeWith(IllegalStateException.class, Errors::mapException).log()
                .onErrorResumeWith(TimeoutException.class, (e)->Errors.serviceTookMoreTime()).log()
                .onErrorResumeWith(Errors::unknownException).log()
                .next().log()).orElseGet(()->Errors.loanAmountRequired());
    }
}

