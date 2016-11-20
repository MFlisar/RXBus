package com.michaelflisar.rxbus;

import com.michaelflisar.rxbus.interfaces.IRXBusQueue;
import com.michaelflisar.rxbus.rx.RXBusMode;
import com.michaelflisar.rxbus.rx.RXQueueKey;
import com.michaelflisar.rxbus.rx.RXSubscriptionManager;
import com.michaelflisar.rxbus.rx.RXUtil;
import com.michaelflisar.rxbus.rx.RxValve;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.internal.util.InternalObservableUtils;

/**
 * Created by Michael on 01.05.2016.
 */

public class RXBusBuilder<T>
{
    private Class<T> mEventClass;
    private List<RXQueueKey<T>> mKeys = null;

    private RXBusMode mBusMode = null;

    private IRXBusQueue mQueuer = null;
    private int mValvePrefetch = 1000;
    private boolean mBackpressureBeforeValve = true;
    private boolean mQueueSubscriptionSafetyCheckEnabled = true;

    private Object mBoundObject = null;

    public static <T> RXBusBuilder<T> create(Class<T> eventClass)
    {
        return new RXBusBuilder<T>(eventClass);
    }

    public RXBusBuilder(Class<T> eventClass)
    {
        mEventClass = eventClass;
    }

    public RXBusBuilder<T> withMode(RXBusMode mode)
    {
        mBusMode = mode;
        return this;
    }

    public RXBusBuilder<T> withQueuing(IRXBusQueue queuer)
    {
        mQueuer = queuer;
        return this;
    }

    public RXBusBuilder<T> withQueuing(IRXBusQueue queuer, int valvePrefetch)
    {
        mQueuer = queuer;
        mValvePrefetch = valvePrefetch;
        return this;
    }

    public RXBusBuilder<T> withBackpressure(boolean enabled)
    {
        mBackpressureBeforeValve = enabled;
        return this;
    }

    public RXBusBuilder<T> withSafetyCheck(boolean enabled)
    {
        mQueueSubscriptionSafetyCheckEnabled = enabled;
        return this;
    }

    public RXBusBuilder<T> withKeys(RXQueueKey<T>... key)
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
    public RXBusBuilder<T> withKey(int... key)
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

    public RXBusBuilder<T> withKey(String... key)
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

    public RXBusBuilder<T> withBound(Object boundObject)
    {
        mBoundObject = boundObject;
        return this;
    }

    // ---------------------------
    // observable - build
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

        Subscription subscription = observable.subscribe(actualOnNext, onError, onCompleted);
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

        Subscription subscription = observable.subscribe(actualObserver);
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

        Subscription subscription = observable.subscribe(actualSubscriber);
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
