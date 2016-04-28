package com.michaelflisar.rxbus.queued;

import com.michaelflisar.rxbus.RXBus;
import com.michaelflisar.rxbus.interfaces.IRXBusIsResumedProvider;
import com.michaelflisar.rxbus.rx.QueueOperator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by flisar on 22.04.2016.
 */
public class RXQueueBus
{
    private static RXQueueBus INSTANCE = null;

    public static RXQueueBus get()
    {
        if (INSTANCE == null)
            INSTANCE = new RXQueueBus();
        return INSTANCE;
    }

    private HashMap<Class<?>, QueueOperator<?>> mQueingData = new HashMap<>();

    // ----------------------
    // Events
    // ----------------------

    public synchronized <T, PauseResumeClass> Observable<RXQueueEvent<T>> observeEventOnResume(Class<T> eventClass, PauseResumeClass boundClass)
    {
        // 1) get default observable from RXBus
        final Observable<T> observableMain = RXBus.get().observeEvent(eventClass);

        // 2) Create custom operator, it will queue events!
        QueueOperator<T> queueOperator = null;
        if (mQueingData.containsKey(boundClass.getClass()))
            queueOperator = (QueueOperator<T>)mQueingData.get(boundClass.getClass());
        else
        {
            queueOperator = new QueueOperator<>();
            mQueingData.put(boundClass.getClass(), queueOperator);
        }

        Observable<RXQueueEvent<T>> observable = observableMain
                .map(data -> new RXQueueEvent<T>(true, data))
                .lift(queueOperator);

        // we just let the bus be synchronous...
//        observable = observable
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread());

        return observable;
    }
    public synchronized <S extends IRXBusIsResumedProvider> void resume(S pauseResumeProvider)
    {
        if (mQueingData.containsKey(pauseResumeProvider.getClass()))
            mQueingData.get(pauseResumeProvider.getClass()).onResume();
    }

    public synchronized <S extends IRXBusIsResumedProvider> void clean(S pauseResumeProvider)
    {
        mQueingData.remove(pauseResumeProvider.getClass());
    }

    public <T> Subscription subscribe(Observable<RXQueueEvent<T>> observable, IRXBusIsResumedProvider pauseResumeProvider, Observer<T> observer)
    {
        return observable.subscribe(new RXQueueObserver<T>(pauseResumeProvider)
        {
            @Override
            protected void onNextResumeAlreadyChecked(T data)
            {
                observer.onNext(data);
            }

            @Override
            public void onCompleted()
            {
                observer.onCompleted();
            }

            @Override
            public void onError(Throwable e)
            {
                observer.onError(e);
            }
        });
    }

    public <T, PauseResumeClass> void queue(T data, PauseResumeClass boundClass)
    {
        QueueOperator<T> queueOperator = (QueueOperator<T>)mQueingData.get(boundClass.getClass());
        if (queueOperator != null)
            queueOperator.addToQueue(data);
    }
}
