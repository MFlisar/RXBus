package com.michaelflisar.rxbus.queued;

import com.michaelflisar.rxbus.RXBus;
import com.michaelflisar.rxbus.interfaces.IRXBusIsResumedProvider;
import com.michaelflisar.rxbus.rx.QueueOperator;

import rx.Observer;

/**
 * Created by flisar on 28.04.2016.
 */
public abstract class RXQueueObserver<T> implements Observer<RXQueueEvent<T>>
{
    private IRXBusIsResumedProvider mPauseResumeProvider = null;

    public RXQueueObserver(IRXBusIsResumedProvider pauseResumeProvider)
    {
        mPauseResumeProvider = pauseResumeProvider;
    }

    @Override
    public void onCompleted()
    {
        // empty default handler
    }

    @Override
    public void onError(Throwable e)
    {
        // empty default handler
    }

    @Override
    public void onNext(RXQueueEvent<T> e)
    {
        if (!mPauseResumeProvider.isRXBusResumed())
            RXQueueBus.get().queue(e.getData(), mPauseResumeProvider);
        else
            onNextResumeAlreadyChecked(e.getData());
    }

    protected abstract void onNextResumeAlreadyChecked(T data);
}
