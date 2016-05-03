package com.michaelflisar.rxbus;

import com.michaelflisar.rxbus.exceptions.RXBusEventIsNullException;
import com.michaelflisar.rxbus.exceptions.RXBusKeyIsNullException;

import java.util.HashMap;

import rx.Observable;
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

    // for better speed, we use different maps => no wrapper key generation needed if you just want to use a default class based bus
    private HashMap<Class<?>, SerializedSubject> mSubjectsClasses = new HashMap<>();
    private HashMap<RXQueueKey, SerializedSubject> mSubjectsKeys = new HashMap<>();

    // ---------------------------
    // public bus functions - send events
    // ---------------------------

    /**
     * Sends an event to the bus
     * ATTENTION: all observers that are observing the class of the event will retrieve it
     * <p>
     * @param  event  the event that should be broadcasted to the bus
     */
    public synchronized <T> void sendEvent(T event)
    {
        RXBusEventIsNullException.checkEvent(event);

        SerializedSubject subject = getSubject(event.getClass(), false);
        // only send event, if subject exists => this means someone has at least once subscribed to it
        if (subject != null)
            subject.onNext(event);
    }

    /**
     * Sends an event to the bus
     * ATTENTION: ONLY observers that are observing the key AND the same class of the event will retrieve it
     * <p>
     * @param  event  the event that should be broadcasted to the bus
     * @param  key  the key this event should be broadcasted to
     */
    public synchronized <T> void sendEvent(T event, Integer key)
    {
        RXBusEventIsNullException.checkEvent(event);
        RXBusKeyIsNullException.checkKey(key);

        SerializedSubject subject = getSubject(new RXQueueKey(event.getClass(), key), false);
        // only send event, if subject exists => this means someone has at least once subscribed to it
        if (subject != null)
            subject.onNext(event);
    }

    /**
     * Sends an event to the bus
     * ATTENTION: ONLY observers that are observing the key AND the same class of the event will retrieve it
     * <p>
     * @param  event  the event that should be broadcasted to the bus
     * @param  key  the key this event should be broadcasted to
     */
    public synchronized <T> void sendEvent(T event, String key)
    {
        RXBusEventIsNullException.checkEvent(event);
        RXBusKeyIsNullException.checkKey(key);

        SerializedSubject subject = getSubject(new RXQueueKey(event.getClass(), key), false);
        // only send event, if subject exists => this means someone has at least once subscribed to it
        if (subject != null)
            subject.onNext(event);
    }

    // ---------------------------
    // public bus functions - observe events
    // ---------------------------

    /**
     * Get an observable that observes all events of the the class the
     * <p>
     * @param  eventClass  the class of event you want to observe
     * @return      an Observable, that will observe all events of the @param key class
     */
    public synchronized <T> Observable<T> observeEvent(Class<T> eventClass)
    {
        RXBusEventIsNullException.checkEvent(eventClass);

        SerializedSubject subject = getSubject(eventClass, true);
        return subject;
    }

    /**
     * Get an observable that observes all events that are send with the key and are of the type of the event class
     * <p>
     * @param  eventClass  the class of event you want to observe
     * @param  key  the event key you want to observe
     * @return      an Observable, that will observe all events of the @param key class
     */
    public synchronized <T> Observable<T> observeEvent(Class<T> eventClass, Integer key)
    {
        return observeEvent(new RXQueueKey(eventClass, key));
    }

    /**
     * Get an observable that observes all events that are send with the key and are of the type of the event class
     * <p>
     * @param  eventClass  the class of event you want to observe
     * @param  key  the event key you want to observe
     * @return      an Observable, that will observe all events of the @param key class
     */
    public synchronized <T> Observable<T> observeEvent(Class<T> eventClass, String key)
    {
        return observeEvent(new RXQueueKey(eventClass, key));
    }

    /**
     * Get an observable that observes all events that are send with the key and are of the type of the event class
     * <p>
     * @param  key  the event key you want to observe
     * @return      an Observable, that will observe all events of the @param key class
     */
    public synchronized <T> Observable<T> observeEvent(RXQueueKey key)
    {
        if (key == null)
            throw new RuntimeException("You can't use a null key");

        SerializedSubject subject = getSubject(key, true);
        return subject;
    }

    // ---------------------------
    // private helper functions
    // ---------------------------

    private synchronized SerializedSubject getSubject(Class<?> key, boolean createIfMissing)
    {
        // 1) look if key already has a publisher subject, if so, return it
        if (mSubjectsClasses.containsKey(key))
            return mSubjectsClasses.get(key);
        // 2) else, create a new one and put it into the map
        else if (createIfMissing)
        {
            SerializedSubject subject = new SerializedSubject(PublishSubject.create());
            mSubjectsClasses.put(key, subject);
            return subject;
        }
        else
            return null;
    }

    private synchronized SerializedSubject getSubject(RXQueueKey key, boolean createIfMissing)
    {
        // 1) look if key already has a publisher subject, if so, return it
        if (mSubjectsKeys.containsKey(key))
            return mSubjectsKeys.get(key);
            // 2) else, create a new one and put it into the map
        else if (createIfMissing)
        {
            SerializedSubject subject = new SerializedSubject(PublishSubject.create());
            mSubjectsKeys.put(key, subject);
            return subject;
        }
        else
            return null;
    }
}