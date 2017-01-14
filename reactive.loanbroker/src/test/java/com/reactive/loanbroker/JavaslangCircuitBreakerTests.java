package com.reactive.loanbroker;

import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.circuitbreaker.CircuitBreakerConfig;
import io.github.robwin.circuitbreaker.operator.CircuitBreakerOperator;
import io.reactivex.Flowable;
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
public class JavaslangCircuitBreakerTests {

    private Try<String> function (ExecutorService executor, Boolean flag){
        return Try.of(() -> {
            if(flag) {
                Thread.sleep(4000);
                return "Good night";
            }
            else
                return "Good morning";
        });
    }


    private Try<String> functionWithTimeout (ExecutorService executor, Boolean flag){

        return Try.of(() -> {
            Future<String> result = executor.submit(()->{
                if(flag) {
                    Thread.sleep(2000);
                    return "Good night";
                }
                else
                    return "Good morning";
            });
            return result.get(1000,TimeUnit.MILLISECONDS);
        }).recover(throwable -> "bye");
    }

    @Test
    public void testCircuitBreaker() {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(25)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .ringBufferSizeInHalfOpenState(2)
                .ringBufferSizeInClosedState(2)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("test",circuitBreakerConfig);

        Observable.interval(500, TimeUnit.MILLISECONDS).map((i)->{

            Try<String> result = null;

            if(i>10)
                result = functionWithTimeout(executor,true);
            else
                result = functionWithTimeout(executor,false);

            return result.get()+" : "+i;

        }).timeout(1500,TimeUnit.MILLISECONDS).lift(CircuitBreakerOperator.of(circuitBreaker)).subscribe(System.out::println);


        LockSupport.park();

    }

}
