package com.michaelflisar.rxbus;

import java.util.HashMap;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

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

    private HashMap<Class<?>, SerializedSubject> mSubjects = new HashMap<>();

    // ---------------------------
    // public bus functions
    // ---------------------------

    public synchronized <T> void sendEvent(T event)
    {
        SerializedSubject subject = getSubject((Class<T>)event.getClass());
        subject.onNext(event);
    }

    public synchronized <T> Observable<T> observeEvent(Class<T> eventClass)
    {
        SerializedSubject subject = getSubject(eventClass);
        return subject;
    }

    // ---------------------------
    // private helper functions
    // ---------------------------

    private synchronized <T> SerializedSubject getSubject(Class<T> eventClass)
    {
        // 1) look if class already has a publisher subject, if so, return it
        if (mSubjects.containsKey(eventClass))
            return mSubjects.get(eventClass);
        // 2) else, create a new one and put it into the map
        else
        {
            SerializedSubject subject = new SerializedSubject(PublishSubject.create());
            mSubjects.put(eventClass, subject);
            return subject;
        }
    }
}