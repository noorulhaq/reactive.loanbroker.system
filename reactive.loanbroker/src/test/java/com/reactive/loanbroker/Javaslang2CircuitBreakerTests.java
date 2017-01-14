package com.reactive.loanbroker;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Observable;
import javaslang.control.Try;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by Noor on 1/1/17.
 */
public class Javaslang2CircuitBreakerTests {


    @Test
    public void testCircuitBreaker() {


        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(25)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .ringBufferSizeInHalfOpenState(1)
                .ringBufferSizeInClosedState(2)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("test");

        Observable.interval(500,TimeUnit.MILLISECONDS).map(i -> {
            if(1==1) throw new RuntimeException("BAM");
            return "result"+i;
        })
                .lift(CircuitBreakerOperator.of(circuitBreaker)).map(result -> result)
                .subscribe(System.out::println);


        LockSupport.park();

    }


    @Test
    public void testCircuitBreakerWithTimeout() {


        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(25)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .ringBufferSizeInHalfOpenState(1)
                .ringBufferSizeInClosedState(2)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("test");

        Try.CheckedSupplier<String> decoratedSupplier = CircuitBreaker
                .decorateCheckedSupplier(circuitBreaker, () -> "This can be any method which returns: 'Hello");





        Observable.interval(500,TimeUnit.MILLISECONDS).map(i -> {

            return Try.of(() -> {
                Thread.sleep(1000);
                return "result"+i;
            } ).recover(throwable -> "recovered"+i);

        }).timeout(500,TimeUnit.MILLISECONDS)
                .lift(CircuitBreakerOperator.of(circuitBreaker))
                .subscribe(System.out::println);


        LockSupport.park();

    }

}
