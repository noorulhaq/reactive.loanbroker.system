package com.reactive.loanbroker;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import io.reactivex.Observable;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Created by husainbasrawala on 1/1/17.
 */

public class HystrixTests {


    @Test
    public void testHystrixObservableCommand(){


        class TestCommand extends HystrixObservableCommand<Long>{


          public TestCommand(){
              super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup")).andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                      .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                      .withCircuitBreakerRequestVolumeThreshold(5)
                      .withCircuitBreakerErrorThresholdPercentage(25)
                      .withCircuitBreakerSleepWindowInMilliseconds(10000)
                      .withFallbackEnabled(true)));
          }

            @Override
            protected rx.Observable<Long> construct() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return    rx.Observable.just(2000l);//.doOnNext((index)->{if(0==(index%2))throw new RuntimeException("blow up");});
            }

            @Override
            protected rx.Observable<Long> resumeWithFallback() {
                System.err.println("resumeWithFallback");
                return rx.Observable.just(1000l);
            }

        };


        rx.Observable.interval(1000,TimeUnit.MILLISECONDS).doOnNext((i)->{
            TestCommand command = new TestCommand();
            command.observe().toBlocking().subscribe(System.out::println);
            System.err.println(command.isCircuitBreakerOpen());
        }).toBlocking().subscribe();
    }



    @Test
    public void testHystrixCommand(){


        class TestCommand extends HystrixCommand<Long> {


            public TestCommand(){
                super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup")).andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
                        .withCircuitBreakerRequestVolumeThreshold(5)
                        .withCircuitBreakerErrorThresholdPercentage(25)
                        .withCircuitBreakerSleepWindowInMilliseconds(10000)
                        .withRequestLogEnabled(false)
                        .withFallbackEnabled(true)));
            }

            @Override
            protected Long run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return   2000l;//.doOnNext((index)->{if(0==(index%2))throw new RuntimeException("blow up");});
            }

            @Override
            protected Long getFallback() {
                return 5000l;
            }
        };


        rx.Observable.interval(1000,TimeUnit.MILLISECONDS).doOnNext((i)->{
            TestCommand command = new TestCommand();
            System.out.println(command.execute());
            System.err.println(command.isCircuitBreakerOpen());
        }).toBlocking().subscribe();
    }

}
