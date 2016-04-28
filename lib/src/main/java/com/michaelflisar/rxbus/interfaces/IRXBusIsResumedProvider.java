package com.michaelflisar.rxbus.interfaces;

/**
 * Created by Prometheus on 22.04.2016.
 */
public interface IRXBusIsResumedProvider
{
    boolean isRXBusResumed();
    void addResumedListener(IRXBusResumedListener listener, boolean callListener);
    void removeResumedListener(IRXBusResumedListener listener);
}
