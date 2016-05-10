package com.michaelflisar.rxbus.rx;

import com.michaelflisar.rxbus.RXBusBuilder;
import com.michaelflisar.rxbus.interfaces.IRXBusIsResumedProvider;

import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by flisar on 02.05.2016.
 */
public class RXBusUtil
{
    public static <T> Action1<T> wrapQueueAction(Action1<T> action, IRXBusIsResumedProvider isResumedProvider)
    {
        return new Action1<T>()
        {
            @Override
            public void call(T t)
            {
                if (RXUtil.safetyQueueCheck(t, isResumedProvider))
                    action.call(t);
            }
        };
    }

    public static <T> Observer<T> wrapObserver(Observer<T> observer, IRXBusIsResumedProvider isResumedProvider)
    {
        return new Observer<T>()
        {
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

            @Override
            public void onNext(T t)
            {
                if (RXUtil.safetyQueueCheck(t, isResumedProvider))
                    observer.onNext(t);
            }
        };
    }

    public static <T> Subscriber<T> wrapSubscriber(Subscriber<T> subscriber, IRXBusIsResumedProvider isResumedProvider)
    {
        return new Subscriber<T>()
        {
            @Override
            public void onCompleted()
            {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e)
            {
                subscriber.onError(e);
            }

            @Override
            public void onNext(T t)
            {
                if (RXUtil.safetyQueueCheck(t, isResumedProvider))
                    subscriber.onNext(t);
            }
        };
    }}
