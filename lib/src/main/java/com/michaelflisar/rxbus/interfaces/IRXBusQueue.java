package com.michaelflisar.rxbus.interfaces;

import rx.Observable;

/**
 * Created by Michael on 22.04.2016.
 */
public interface IRXBusQueue
{
    boolean isBusResumed();
    Observable<Boolean> getResumeObservable();
}
