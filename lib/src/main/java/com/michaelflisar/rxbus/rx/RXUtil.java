package com.michaelflisar.rxbus.rx;

import android.util.Log;

import com.michaelflisar.rxbus.RXBus;
import com.michaelflisar.rxbus.interfaces.IRXBusIsResumedProvider;
import com.michaelflisar.rxbus.interfaces.IRXBusResumedListener;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by Prometheus on 28.04.2016.
 */
public class RXUtil
{
    private static final Observable.Transformer schedulersTransformer = new Observable.Transformer() {
            @Override
            public Object call(Object observable) {
                return ((Observable)observable)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };

    private static final Observable.Transformer schedulersTransformerBackground = new Observable.Transformer() {
        @Override
        public Object call(Object observable) {
            return ((Observable)observable)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io());
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> Observable.Transformer<T, T> applyBackgroundSchedulers() {
        return (Observable.Transformer<T, T>) schedulersTransformerBackground;
    }

    // default background subscription, foreground observation schedulars
    @SuppressWarnings("unchecked")
    public static <T> Observable.Transformer<T, T> applySchedulars() {
        return (Observable.Transformer<T, T>) schedulersTransformer;
    }

    public static Observable<Boolean> createResumeStateObservable(IRXBusIsResumedProvider provider)
    {
        return createResumeStateObservable(provider, null);
    }

    public static Observable<Boolean> createResumeStateObservable(IRXBusIsResumedProvider provider, final IRXBusResumedListener listener)
    {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(final Subscriber<? super Boolean> subscriber) {
                IRXBusResumedListener resumedListener = new IRXBusResumedListener() {

                    @Override
                    public void onResumedChanged(boolean resumed) {
                        if (subscriber.isUnsubscribed()) {
                            provider.removeResumedListener(this);
                        } else {
                            subscriber.onNext(resumed);
                        }
                        // foreward event to outer listener
                        if (listener != null)
                            listener.onResumedChanged(resumed);
                    }
                };

                provider.addResumedListener(resumedListener, false);
            }
        });
    }

    public static <T> boolean safetyQueueCheck(T event, IRXBusIsResumedProvider isResumedProvider)
    {
        if (isResumedProvider.isRXBusResumed())
            return true;
        else
        {
            RXBus.get().sendEvent(event);
            return false;
        }
    }

}
