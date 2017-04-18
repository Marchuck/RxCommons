package pl.marchuck.rxcommons.rx;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * Created by Lukasz Marczak on 16.03.17.
 * lukasz@next42.net
 */

public class RetryWithDelay implements Function<Observable<? extends Throwable>, Observable<?>> {

    private final int retryDelayMillis;
    private final Runnable doOnNext;
    private final int maxRetries;
    private int retryCount;

    public RetryWithDelay(final int maxRetries, final int retryDelayMillis) {
        this(maxRetries, retryDelayMillis, null);
    }

    public RetryWithDelay(final int maxRetries, final int retryDelayMillis, Runnable doOnNext) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.retryCount = 0;
        this.doOnNext = doOnNext;
    }

    @Override
    public Observable<?> apply(Observable<? extends Throwable> attempts) throws Exception {
        return attempts.flatMap(new Function<Throwable, ObservableSource<?>>() {
            @Override
            public ObservableSource<?> apply(Throwable throwable) throws Exception {
                if (++retryCount < maxRetries) {
                    // When this Observable calls onNext, the original
                    // Observable will be retried (i.e. re-subscribed).
                    if (doOnNext != null) doOnNext.run();
                    return Observable.timer(retryDelayMillis,
                            TimeUnit.MILLISECONDS);
                }
                // Max retries hit. Just pass the error along.
                return Observable.error(throwable);
            }
        });
    }
}

