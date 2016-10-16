package com.michaelflisar.rxbus.rx;

import com.michaelflisar.rxbus.RXBus;
import com.michaelflisar.rxbus.interfaces.IRXBusQueue;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Michael on 28.04.2016.
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

    public static <T> boolean safetyQueueCheck(T event, IRXBusQueue isResumedProvider)
    {
        if (isResumedProvider.isBusResumed())
            return true;
        else
        {
            RXBus.get().sendEvent(event);
            return false;
        }
    }

}
