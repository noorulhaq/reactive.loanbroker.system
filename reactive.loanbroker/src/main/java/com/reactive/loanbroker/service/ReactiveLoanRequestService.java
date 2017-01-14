package com.reactive.loanbroker.service;

import com.reactive.loanbroker.model.Quotation;
import org.reactivestreams.Publisher;

import java.time.Duration;

/**
 * Created by Noor on 12/26/16.
 */
public interface ReactiveLoanRequestService {

    Quotation OFFER_IN_CASE_OF_ERROR = new Quotation("Pseudo-Bank",Double.MAX_VALUE);

    String REQUEST_QUOTATION_PATH = "/quotation?loanAmount={loanAmount}";

    Duration REQUEST_TIMEOUT = Duration.ofMillis(1000);

    Publisher<Quotation> requestForQuotation(String bank, Double loanAmount);

}
