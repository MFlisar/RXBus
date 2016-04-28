package com.michaelflisar.rxbus;

import java.util.HashMap;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by Prometheus on 22.04.2016.
 */
public class RXBus
{
    // ---------------------------
    // Singleton
    // ---------------------------

    private static RXBus INSTANCE = null;

    public static RXBus get()
    {
        if (INSTANCE == null)
            INSTANCE = new RXBus();
        return INSTANCE;
    }

    private HashMap<Class<?>, PublishSubject<?>> mSubjects = new HashMap<>();

    // ---------------------------
    // public bus functions
    // ---------------------------

    public synchronized <T> void sendEvent(T event)
    {
        PublishSubject<T> subject = getSubject((Class<T>)event.getClass());
        subject.onNext(event);
    }

    public synchronized <T> Observable<T> observeEvent(Class<T> eventClass)
    {
        PublishSubject<T> subject = getSubject(eventClass);
        return subject;
    }

    // ---------------------------
    // private helper functions
    // ---------------------------

    private synchronized <T> PublishSubject<T> getSubject(Class<T> eventClass)
    {
        // 1) look if class already has a publisher subject, if so, return it
        if (mSubjects.containsKey(eventClass))
            return (PublishSubject<T>)mSubjects.get(eventClass);
        // 2) else, create a new one and put it into the map
        else
        {
            PublishSubject<T> subject = PublishSubject.create();
            mSubjects.put(eventClass, subject);
            return subject;
        }
    }
}