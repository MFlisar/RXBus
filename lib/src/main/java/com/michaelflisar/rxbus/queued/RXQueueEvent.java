package com.michaelflisar.rxbus.queued;

/**
 * Created by flisar on 28.04.2016.
 */
public class RXQueueEvent<T>
{
    private boolean mResumed;
    private T mData;

    public RXQueueEvent(boolean resumed, T data)
    {
        mResumed = resumed;
        mData = data;
    }

    public RXQueueEvent<T> setResumed(boolean resumed)
    {
        mResumed = resumed;
        return this;
    }

    public boolean isResumed()
    {
        return mResumed;
    }

    public T getData()
    {
        return mData;
    }
}
