package com.michaelflisar.rxbus;

import com.michaelflisar.rxbus.interfaces.IRXBusIsResumedProvider;
import com.michaelflisar.rxbus.rx.RXBusMode;
import com.michaelflisar.rxbus.rx.RXUtil;
import com.michaelflisar.rxbus.rx.RxValve;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Created by Prometheus on 01.05.2016.
 */
public class RXBusBuilder<T>
{
    private boolean mQueueEvents;
    private RXBusMode mBusMode;
    private Class<T> mEventClass;
    private Observable<Boolean> mObservableIsResumed;
    private IRXBusIsResumedProvider mIsResumedProvider;
    private int mValvePrefetch = 1000;
    private boolean mQueueSubscriptionSafetyCheckEnabled = true;

    private Action1<? super T> mActionNext;
    private Action1<Throwable> mActionError;
    private Action0 mActionOnComplete;
    private Observer<? super T> mSubscriptionObserver;
    private Subscriber<? super T> mSubscriber;

    public RXBusBuilder(Class<T> eventClass)
    {
        mEventClass = eventClass;

        mQueueEvents = false;
        mBusMode = RXBusMode.Background;
        mObservableIsResumed = null;
        mIsResumedProvider = null;

        mActionNext = null;
        mActionError = null;
        mActionOnComplete = null;
        mSubscriptionObserver = null;
        mSubscriber = null;
    }

    public RXBusBuilder<T> withBusMode(RXBusMode mode)
    {
        mBusMode = mode;
        return this;
    }

    public RXBusBuilder<T> queue(Observable<Boolean> isResumedObservable, IRXBusIsResumedProvider isResumedProvider)
    {
        mQueueEvents = true;
        mObservableIsResumed = isResumedObservable;
        mIsResumedProvider = isResumedProvider;
        return this;
    }

    public RXBusBuilder<T> withQueueSubscriptionSafetyCheckEnabled(boolean enabled)
    {
        mQueueSubscriptionSafetyCheckEnabled = enabled;
        return this;
    }


    public RXBusBuilder<T> withValvePrefetch(int prefetch)
    {
        mValvePrefetch = prefetch;
        return this;
    }

    public RXBusBuilder<T> withOnNext(Action1<T> actionNext)
    {
        mActionNext = actionNext;
        return this;
    }

    public RXBusBuilder<T> withOnError(Action1<Throwable> actionerror)
    {
        mActionError = actionerror;
        return this;
    }

    public RXBusBuilder<T> withOnComplete(Action0 actionComplete)
    {
        mActionOnComplete= actionComplete;
        return this;
    }

    public RXBusBuilder<T> withObserver(Observer<T> subscriptionObserver)
    {
        mSubscriptionObserver = subscriptionObserver;
        return this;
    }

    public RXBusBuilder<T> withSubscriber(Subscriber<? super T> subscriber)
    {
        mSubscriber = subscriber;
        return this;
    }

    // -----------------------------
    // build functions
    // -----------------------------

    public Observable<T> buildObservable()
    {
        Observable<T> observable = RXBus.get().observeEvent(mEventClass);
        if (mBusMode == RXBusMode.Background)
            observable = observable.compose(RXUtil.<T>applyBackgroundSchedulers());
        else if (mBusMode == RXBusMode.Main)
            observable = observable.compose(RXUtil.<T>applySchedulars());

        if (mQueueEvents)
            observable = observable.lift(new RxValve<T>(mObservableIsResumed, mValvePrefetch, mIsResumedProvider.isRXBusResumed()));

        return observable;
    }

    public Subscription buildSubscription()
    {
        Observable<T> observable = buildObservable();

        if (mSubscriber == null && mSubscriptionObserver == null && mActionNext == null)
            throw new RuntimeException("Subscription can't be build, because no next action, subscriber nor observable was set!");

        if (mSubscriber == null && mSubscriptionObserver == null)
        {
            Action1<? super T> actionNext = mActionNext;
            if (mQueueSubscriptionSafetyCheckEnabled)
                actionNext = InternalRXBusUtil.wrapQueueAction(mActionNext, mIsResumedProvider);

            if (mActionError != null && mActionOnComplete != null)
                return observable.subscribe(actionNext, mActionError, mActionOnComplete);
            else if (mActionError != null)
                return observable.subscribe(actionNext, mActionError);
            return observable.subscribe(actionNext);
        }
        else if (mSubscriber == null)
        {
            Observer<? super T> subscriptionObserver = mSubscriptionObserver;
            if (mQueueSubscriptionSafetyCheckEnabled)
                subscriptionObserver = InternalRXBusUtil.wrapObserver(mSubscriptionObserver, mIsResumedProvider);
            return observable.subscribe(subscriptionObserver);
        }
        else if (mSubscriptionObserver == null)
        {
            Subscriber<? super T> subscriber = mSubscriber;
            if (mQueueSubscriptionSafetyCheckEnabled)
                subscriber = InternalRXBusUtil.wrapSubscriber(mSubscriber, mIsResumedProvider);
            return observable.subscribe(subscriber);
        }
        else
            throw new RuntimeException("Subscription can't be build, because you have set more than one of following: nnext action, subscriber or observable!");
    }
}
