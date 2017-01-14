package com.reactive.loanbroker.service;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import com.reactive.loanbroker.model.Quotation;
import rx.Observable;

/**
 * Created by Noor on 1/7/17.
 */
public abstract class LoanQuotationCommand extends HystrixObservableCommand<Quotation> {

    private String bankURL;
    private Double loanAmount;
    private Quotation fallbackQuotation;

    LoanQuotationCommand(String bankURL, Double loanAmount,Quotation fallbackQuotation) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("loanBrokerGroup"))
                .andCommandKey(HystrixCommandKey.Factory.asKey(bankURL))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
                        .withCircuitBreakerRequestVolumeThreshold(5)
                .withCircuitBreakerErrorThresholdPercentage(25)
                .withCircuitBreakerSleepWindowInMilliseconds(20000)
                .withFallbackEnabled(true)));
        System.err.println(bankURL);
        this.bankURL = bankURL;
        this.loanAmount = loanAmount;
        this.fallbackQuotation = fallbackQuotation;
    }

    public String getBankURL() {
        return bankURL;
    }

    public Double getLoanAmount() {
        return loanAmount;
    }

    @Override
    protected Observable<Quotation> resumeWithFallback() {
        return Observable.just(this.fallbackQuotation);
    }
}