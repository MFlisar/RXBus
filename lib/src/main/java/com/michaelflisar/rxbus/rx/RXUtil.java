package com.michaelflisar.rxbus.rx;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Prometheus on 28.04.2016.
 */
public class RXUtil
{
//    private static final Observable.Transformer schedulersTransformer = observable -> observable
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread());

    private static final Observable.Transformer schedulersTransformer = new Observable.Transformer() {
            @Override
            public Object call(Object observable) {
                return ((Observable)observable).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };

    @SuppressWarnings("unchecked")
    public static <T> Observable.Transformer<T, T> applyBackgroundWorkForegroundObserveSchedulers() {
        return (Observable.Transformer<T, T>) schedulersTransformer;
    }

    public static <T> Observable<T> applySchedulars(Observable<T> observable)
    {
        return observable.compose(applyBackgroundWorkForegroundObserveSchedulers());
    }
}
