package com.reactive.loanbroker.handler;

import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.handler.error.Errors;
import com.reactive.loanbroker.model.BestQuotationResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
/**
 * Created by husainbasrawala on 1/8/17.
 */
public class LoanBrokerHandler {


    private ReactiveLoanBrokerAgent reactorLoanBrokerAgent;

    public LoanBrokerHandler(ReactiveLoanBrokerAgent reactorLoanBrokerAgent) {
        this.reactorLoanBrokerAgent = reactorLoanBrokerAgent;
    }

    public Mono<ServerResponse> bestQuotation(ServerRequest request) {

        return Mono.justOrEmpty(request.queryParam("loanAmount"))
                .map(Double::valueOf)
                .then((Double loanAmount) -> Mono.from(reactorLoanBrokerAgent.findBestQuotation(loanAmount))
                        .filter(quotation -> quotation.getRequestedLoanAmount().equals(loanAmount))
                        .otherwiseIfEmpty(Mono.error(new IllegalStateException("Returned amount does not match with the best offer"))))
                .flatMap(bestQuotationResponse -> ServerResponse.ok().body(Mono.just(bestQuotationResponse), BestQuotationResponse.class))
                .switchIfEmpty(Errors.loanAmountRequired())
                .onErrorResumeWith(IllegalStateException.class, Errors::mapException)
                .onErrorResumeWith(Errors::unknownException)
                .next();
    }
}

