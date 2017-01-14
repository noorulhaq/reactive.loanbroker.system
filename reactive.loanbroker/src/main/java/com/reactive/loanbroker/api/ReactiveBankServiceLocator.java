package com.reactive.loanbroker.api;

import org.reactivestreams.Publisher;

/**
 * Created by Noor on 12/31/16.
 */

public interface ReactiveBankServiceLocator {

    public Publisher<String> banksURL();

}
