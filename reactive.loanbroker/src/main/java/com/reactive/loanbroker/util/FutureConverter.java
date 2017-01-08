package com.reactive.loanbroker.util;


import io.reactivex.Single;
import net.javacrumbs.futureconverter.springcommon.SpringFutureUtils;
import org.springframework.util.concurrent.ListenableFuture;

public class FutureConverter {

    /**
     * Converts {@link ListenableFuture} to  {@link io.reactivex.Single}.
     * The original future is canceled upon unsubscribe.
     */
    public static <T> Single<T> toSingle(ListenableFuture<T> listenableFuture) {
        return RxJava2FutureUtils.createSingle(SpringFutureUtils.createValueSource(listenableFuture));
    }

    /**
     * Converts  {@link io.reactivex.Single} to {@link ListenableFuture}.
     */
    public static <T> ListenableFuture<T> toListenableFuture(Single<T> single) {
        return SpringFutureUtils.createListenableFuture(RxJava2FutureUtils.createValueSource(single));
    }

}
