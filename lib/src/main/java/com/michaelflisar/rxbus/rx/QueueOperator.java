package com.michaelflisar.rxbus.rx;

import com.michaelflisar.rxbus.queued.RXQueueEvent;

import java.util.LinkedList;
import java.util.Queue;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by flisar on 28.04.2016.
 */
public class QueueOperator<T> implements Observable.Operator<RXQueueEvent<T>, RXQueueEvent<T>>
{
    private Queue<T> mQueue = new LinkedList<>();
    private Subscriber<RXQueueEvent<T>> mSubscriber = null;

    public QueueOperator()
    {
        super();
    }

    public void onResume()
    {
        while (!mQueue.isEmpty())
            mSubscriber.onNext(new RXQueueEvent<T>(true, mQueue.poll()));
    }

    public void addToQueue(T data)
    {
        mQueue.add(data);
    }

    @Override
    public Subscriber<? super RXQueueEvent<T>> call(Subscriber<? super RXQueueEvent<T>> subscriber)
    {
        mSubscriber = new Subscriber<RXQueueEvent<T>>()
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
            public void onNext(RXQueueEvent<T> t)
            {
                if (t.isResumed())
                    subscriber.onNext(t);
                else
                    mQueue.add(t.getData());
            }
        };
        return mSubscriber;
    }
}