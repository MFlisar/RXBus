package com.michaelflisar.rxbus;

import com.michaelflisar.rxbus.interfaces.IRXBusQueue;
import com.michaelflisar.rxbus.rx.RXUtil;

import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * Created by flisar on 02.05.2016.
 */
public class RXBusUtil
{
    protected static <T> Action1<T> wrapQueueAction(Action1<T> action, IRXBusQueue isResumedProvider)
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

    protected static <T> Observer<T> wrapObserver(Observer<T> observer, IRXBusQueue isResumedProvider)
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

    protected static <T> Subscriber<T> wrapSubscriber(Subscriber<T> subscriber, IRXBusQueue isResumedProvider)
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
