package com.reactive.loanbroker.handler;

import com.reactive.loanbroker.api.ReactiveLoanBrokerAgent;
import com.reactive.loanbroker.handler.error.Errors;
import com.reactive.loanbroker.model.BestQuotationResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import java.util.concurrent.TimeoutException;

/**
 * Created by Noor on 1/8/17.
 */
public class LoanBrokerHandler {


private ReactiveLoanBrokerAgent loanBrokerAgent;

public LoanBrokerHandler(ReactiveLoanBrokerAgent reactorLoanBrokerAgent) {
    this.loanBrokerAgent = reactorLoanBrokerAgent;
}

public Mono<ServerResponse> bestQuotation(ServerRequest request) {

    return request.queryParam("loanAmount").map((loanAmountParam) -> Mono.just(loanAmountParam)
            .map(Double::valueOf)
            .then((loanAmount) ->
                    Mono.from( loanBrokerAgent.findBestQuotation(loanAmount))
                            .then((quotation -> isValid(quotation,loanAmount))))
            .flatMap(bestQuotationResponse ->
                    ServerResponse.ok()
                            .body(Mono.just(bestQuotationResponse), BestQuotationResponse.class))
            .switchIfEmpty(Errors.noBankServiceAvailable())
            .onErrorResumeWith(IllegalStateException.class, Errors::mapException)
            .onErrorResumeWith(Errors::unknownException)
            .next()).orElseGet(()->Errors.loanAmountRequired());
}

private Mono<BestQuotationResponse> isValid(BestQuotationResponse quotation,Double loanAmount){
    return quotation.getRequestedLoanAmount().equals(loanAmount) ?
            Mono.just(quotation) : Mono.error(new IllegalStateException("Returned amount does not match with the best offer"));
}
}

