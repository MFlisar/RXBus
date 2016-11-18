package com.michaelflisar.rxbus;

import com.michaelflisar.rxbus.exceptions.RXBusEventIsNullException;
import com.michaelflisar.rxbus.exceptions.RXBusKeyIsNullException;
import com.michaelflisar.rxbus.interfaces.IRXBusQueue;
import com.michaelflisar.rxbus.rx.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.internal.util.InternalObservableUtils;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

/**
 * Created by Michael on 22.04.2016.
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
        sendEvent(event, key, false);
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
        sendEvent(event, key, false);
    }

    /**
     * Sends an event to the bus
     * <p>
     * @param  event  the event that should be broadcasted to the bus
     * @param  key  the key this event should be broadcasted to
     * @param  sendToDefaultBusAsWell  if true, all observers of the event class will receive this event as well
     */
    public synchronized <T> void sendEvent(T event, Integer key, boolean sendToDefaultBusAsWell)
    {
        RXBusEventIsNullException.checkEvent(event);
        RXBusKeyIsNullException.checkKey(key);

        // 1) send to key bound bus
        SerializedSubject subject = getSubject(new RXQueueKey(event.getClass(), key), false);
        // only send event, if subject exists => this means someone has at least once subscribed to it
        if (subject != null)
            subject.onNext(event);

        // 2) send to unbound bus
        if (sendToDefaultBusAsWell)
            sendEvent(event);
    }

    /**
     * Sends an event to the bus
     * <p>
     * @param  event  the event that should be broadcasted to the bus
     * @param  key  the key this event should be broadcasted to
     * @param  sendToDefaultBusAsWell  if true, all observers of the event class will receive this event as well
     */
    public synchronized <T> void sendEvent(T event, String key, boolean sendToDefaultBusAsWell)
    {
        RXBusEventIsNullException.checkEvent(event);
        RXBusKeyIsNullException.checkKey(key);

        // 1) send to key bound bus
        SerializedSubject subject = getSubject(new RXQueueKey(event.getClass(), key), false);
        // only send event, if subject exists => this means someone has at least once subscribed to it
        if (subject != null)
            subject.onNext(event);

        // 2) send to unbound bus
        if (sendToDefaultBusAsWell)
            sendEvent(event);
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

    // ---------------------------
    // Builder
    // ---------------------------

    public static class Builder<T>
    {
        private Class<T> mEventClass;
        private List<RXQueueKey<T>> mKeys = null;

        private RXBusMode mBusMode = null;

        private IRXBusQueue mQueuer = null;
        private int mValvePrefetch = 1000;
        private boolean mBackpressureBeforeValve = true;
        private boolean mQueueSubscriptionSafetyCheckEnabled = true;

        private Object mBoundObject = null;

        public static <T> Builder<T> create(Class<T> eventClass)
        {
            return new Builder<T>(eventClass);
        }

        public Builder(Class<T> eventClass)
        {
            mEventClass = eventClass;
        }

        public Builder<T> withMode(RXBusMode mode)
        {
            mBusMode = mode;
            return this;
        }

        public Builder<T> withQueuing(IRXBusQueue queuer)
        {
            mQueuer = queuer;
            return this;
        }

        public Builder<T> withQueuing(IRXBusQueue queuer, int valvePrefetch)
        {
            mQueuer = queuer;
            mValvePrefetch = valvePrefetch;
            return this;
        }

        public Builder<T> withBackpressure(boolean enabled)
        {
            mBackpressureBeforeValve = enabled;
            return this;
        }

        public Builder<T> withSafetyCheck(boolean enabled)
        {
            mQueueSubscriptionSafetyCheckEnabled = enabled;
            return this;
        }

        public Builder<T> withKeys(RXQueueKey<T>... key)
        {
            if (key.length > 0)
            {
                mKeys = new ArrayList<>();
                for (int i = 0; i < key.length; i++)
                    mKeys.add(key[i]);
            }
            else
                mKeys = null;
            return this;
        }
        public Builder<T> withKey(int... key)
        {
            if (key.length > 0)
            {
                mKeys = new ArrayList<>();
                for (int i = 0; i < key.length; i++)
                    mKeys.add( new RXQueueKey(mEventClass, key[i]));
            }
            else
                mKeys = null;
            return this;
        }

        public Builder<T> withKey(String... key)
        {
            if (key.length > 0)
            {
                mKeys = new ArrayList<>();
                for (int i = 0; i < key.length; i++)
                    mKeys.add( new RXQueueKey(mEventClass, key[i]));
            }
            else
                mKeys = null;
            return this;
        }

        public Builder<T> withBound(Object boundObject)
        {
            mBoundObject = boundObject;
            return this;
        }

        // ---------------------------
        // build
        // ---------------------------

        public Observable<T> build()
        {
            return build(true);
        }

        private Observable<T> build(boolean applySchedular)
        {
            Observable<T> observable = null;
            if (mKeys != null)
            {
                for (int i = 0; i < mKeys.size(); i++)
                {
                    if (i == 0)
                        observable = RXBus.get().observeEvent(mKeys.get(i));
                    else
                        observable = observable.mergeWith(RXBus.get().observeEvent(mKeys.get(i)));
                }
            }
            else
                observable = RXBus.get().observeEvent(mEventClass);

            if (mBackpressureBeforeValve)
                observable = observable.onBackpressureBuffer();

            if (mQueuer != null)
                observable = observable.lift(new RxValve<T>(mQueuer.getResumeObservable(), mValvePrefetch, mQueuer.isBusResumed()));

            if (applySchedular)
                observable = applySchedular(observable);

            return observable;
        }

        // ---------------------------
        // subscribe - variants
        // ---------------------------

        public Subscription subscribe(Action1<T> onNext)
        {
            return subscribe(onNext, null, null, null);
        }

        public <R> Subscription subscribe(Action1<R> onNext, Observable.Transformer<T, R> transformer)
        {
            return subscribe(onNext, null, null, transformer);
        }

        public Subscription subscribe(Action1<T> onNext, Action1<Throwable> onError)
        {
            return subscribe(onNext, onError, null, null);
        }

        public <R> Subscription subscribe(Action1<R> onNext, Action1<Throwable> onError, Observable.Transformer<T, R> transformer)
        {
            return subscribe(onNext, onError, null, transformer);
        }

        public Subscription subscribe(Action1<T> onNext, Action1<Throwable> onError,  Action0 onCompleted)
        {
            return subscribe(onNext, onError, onCompleted, null);
        }

        // ---------------------------
        // subscribe implementations
        // ---------------------------

        public <R> Subscription subscribe(Action1<R> onNext, Action1<Throwable> onError,  Action0 onCompleted, Observable.Transformer<T, R> transformer)
        {
            Observable observable = build(false);
            if (transformer != null)
                observable = observable.compose(transformer);

            if (onNext == null)
                onNext = Actions.empty();
            if (onError == null)
                onError = InternalObservableUtils.ERROR_NOT_IMPLEMENTED;
            if (onCompleted == null)
                onCompleted = Actions.empty();

            Action1<R> actualOnNext = onNext;
            if (mQueuer != null && mQueueSubscriptionSafetyCheckEnabled)
                actualOnNext = RXBusUtil.wrapQueueAction(onNext, mQueuer);

            Subscription subscription = applySchedular(observable).subscribe(actualOnNext, onError, onCompleted);
            if (mBoundObject != null)
                RXSubscriptionManager.addSubscription(mBoundObject, subscription);
            return subscription;
        }

        public <R> Subscription subscribe(Observer<R> observer, Observable.Transformer<T, R> transformer)
        {
            Observable observable = build(false);
            if (transformer != null)
                observable = observable.compose(transformer);

            Observer<R> actualObserver = observer;
            if (mQueuer != null && mQueueSubscriptionSafetyCheckEnabled)
                actualObserver = RXBusUtil.wrapObserver(observer, mQueuer);

            Subscription subscription = applySchedular(observable).subscribe(actualObserver);
            if (mBoundObject != null)
                RXSubscriptionManager.addSubscription(mBoundObject, subscription);
            return subscription;
        }

        public <R> Subscription subscribe(Subscriber<R> subscriber, Observable.Transformer<T, R> transformer)
        {
            Observable observable = build(false);
            if (transformer != null)
                observable = observable.compose(transformer);

            Observer<R> actualSubscriber = subscriber;
            if (mQueuer != null && mQueueSubscriptionSafetyCheckEnabled)
                actualSubscriber = RXBusUtil.wrapSubscriber(subscriber, mQueuer);

            Subscription subscription = applySchedular(observable).subscribe(actualSubscriber);
            if (mBoundObject != null)
                RXSubscriptionManager.addSubscription(mBoundObject, subscription);
            return subscription;
        }

        private <X> Observable<X> applySchedular(Observable<X> observable)
        {
            if (mBusMode == RXBusMode.Background)
                return observable.compose(RXUtil.<X>applyBackgroundSchedulers());
            else if (mBusMode == RXBusMode.Main)
                return observable.compose(RXUtil.<X>applySchedulars());
            return observable;
        }
    }
}