
package com.michaelflisar.rxbus.interfaces;

import rx.Observable;

/**
 * Created by flisar on 09.05.2016.
 */
@Deprecated
public interface IRXBusObservableProcessor<T, O>
{
    Observable<O> onObservableReady(Observable<T> observable);
}