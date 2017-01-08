package com.reactive.loanbroker.util;

/**
 * Created by husainbasrawala on 12/26/16.
 */


import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import net.javacrumbs.futureconverter.common.internal.ValueSource;

import java.util.function.Consumer;

public class RxJava2FutureUtils {
    public static <T> Single<T> createSingle(ValueSource<T> valueSource) {
        if (valueSource instanceof SingleBackedValueSource) {
            return ((SingleBackedValueSource<T>) valueSource).getSingle();
        }
        return new ValueSourceBackedSingle<>(valueSource);
    }

    public static <T> ValueSource<T> createValueSource(Single<T> single) {
        if (single instanceof ValueSourceBackedSingle) {
            return ((ValueSourceBackedSingle<T>) single).getValueSource();
        } else {
            return new SingleBackedValueSource<>(single);
        }
    }

    private static class SingleBackedValueSource<T> implements ValueSource<T> {
        private final Single<T> single;
        private Disposable disposable;

        private SingleBackedValueSource(Single<T> single) {
            this.single = single;
        }

        @Override
        public void addCallbacks(Consumer<T> successCallback, Consumer<Throwable> failureCallback) {
            if (disposable == null) {
                disposable = single.subscribe(successCallback::accept, failureCallback::accept);
            } else {
                throw new IllegalStateException("add callbacks can be called only once");
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            disposable.dispose();
            return true;
        }

        private Single<T> getSingle() {
            return single;
        }
    }

    private static class ValueSourceBackedSingle<T> extends Single<T> {
        private final ValueSource<T> valueSource;

        ValueSourceBackedSingle(ValueSource<T> valueSource) {
            this.valueSource = valueSource;
        }

        @Override
        protected void subscribeActual(SingleObserver<? super T> observer) {
            ValueSourceDisposable disposable = new ValueSourceDisposable();
            valueSource.addCallbacks(
                    result -> {
                        try {
                            observer.onSuccess(result);
                        } catch (Throwable e) {
                            observer.onError(e);
                        }
                    },
                    ex -> {
                        if (!disposable.isDisposed()) {
                            observer.onError(ex);
                        }
                    }
            );
            observer.onSubscribe(disposable);
        }

        private ValueSource<T> getValueSource() {
            return valueSource;
        }

        private class ValueSourceDisposable implements Disposable {
            private volatile boolean disposed = false;

            @Override
            public void dispose() {
                disposed = true;
                valueSource.cancel(true);
            }

            @Override
            public boolean isDisposed() {
                return disposed;
            }
        }
    }
}
