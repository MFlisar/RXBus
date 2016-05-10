package com.michaelflisar.rxbus;

import com.michaelflisar.rxbus.interfaces.IRXBusIsResumedProvider;
import com.michaelflisar.rxbus.interfaces.IRXBusObservableProcessor;
import com.michaelflisar.rxbus.rx.RXBusMode;
import com.michaelflisar.rxbus.rx.RXBusUtil;
import com.michaelflisar.rxbus.rx.RXQueueKey;
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

/**
 * Created by Prometheus on 01.05.2016.
 */
public class RXBusBuilder<T, O>
{
    private List<RXQueueKey<T>> mKey = null;
    private Class<T> mKeyClass;

    private boolean mQueueEvents;
    private RXBusMode mBusMode;
    private Observable<Boolean> mObservableIsResumed;
    private IRXBusIsResumedProvider mIsResumedProvider;
    private int mValvePrefetch = 1000;
    private boolean mQueueSubscriptionSafetyCheckEnabled = true;
    private boolean mBackpressureObservableEnabled = true;

    private Action1<? super O> mActionNext;
    private Action1<Throwable> mActionError;
    private Action0 mActionOnComplete;
    private Observer<? super O> mSubscriptionObserver;
    private Subscriber<? super O> mSubscriber;

    private IRXBusObservableProcessor<T, O> mObservableProcessor = null;

    // -----------------------------
    // public create functions
    // -----------------------------

    public static <T> RXBusBuilder<T, T> create(Class<T> eventClass) {
        return new RXBusBuilder<T, T>(eventClass);
    }

    public static <T, O> RXBusBuilder<T, O> create(Class<T> eventClass, Class<O> observableClass, IRXBusObservableProcessor<T, O> observableProcessor) {
        return new RXBusBuilder<T, O>(eventClass, observableClass, observableProcessor);
    }

    // -----------------------------
    // private constructors
    // -----------------------------


    private RXBusBuilder(Class<T> eventClass)
    {
        this(eventClass, (Class<O>)eventClass);
    }

    private RXBusBuilder(Class<T> eventClass, Class<O> observableClass)
    {
        this(eventClass, (Class<O>)eventClass, null);
    }

    private RXBusBuilder(Class<T> eventClass, Class<O> observableClass, IRXBusObservableProcessor<T, O> observableProcessor)
    {
        mKeyClass = eventClass;
        mObservableProcessor = observableProcessor;
        init();
    }

    // -----------------------------
    // init and builder functions
    // -----------------------------

    private void init()
    {
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

    public RXBusBuilder<T, O> withKey(RXQueueKey<T>... key)
    {
        if (key.length > 0)
        {
            mKey = new ArrayList<>();
            for (int i = 0; i < key.length; i++)
                mKey.add(key[i]);
        }
        else
            mKey = null;
        return this;
    }

    public RXBusBuilder<T, O> withKey(int... key)
    {
        if (key.length > 0)
        {
            mKey = new ArrayList<>();
            for (int i = 0; i < key.length; i++)
                mKey.add( new RXQueueKey(mKeyClass, key[i]));
        }
        else
            mKey = null;
        return this;
    }

    public RXBusBuilder<T, O> withKey(String... key)
    {
        if (key.length > 0)
        {
            mKey = new ArrayList<>();
            for (int i = 0; i < key.length; i++)
                mKey.add( new RXQueueKey(mKeyClass, key[i]));
        }
        else
            mKey = null;
        return this;
    }

    public RXBusBuilder<T, O> withBusMode(RXBusMode mode)
    {
        mBusMode = mode;
        return this;
    }

    public RXBusBuilder<T, O> queue(Observable<Boolean> isResumedObservable, IRXBusIsResumedProvider isResumedProvider)
    {
        mQueueEvents = true;
        mObservableIsResumed = isResumedObservable;
        mIsResumedProvider = isResumedProvider;
        return this;
    }

    public RXBusBuilder<T, O> withQueueSubscriptionSafetyCheckEnabled(boolean enabled)
    {
        mQueueSubscriptionSafetyCheckEnabled = enabled;
        return this;
    }

    public RXBusBuilder<T, O> withBackpressureObservableEnabled(boolean enabled)
    {
        mBackpressureObservableEnabled = enabled;
        return this;
    }

    public RXBusBuilder<T, O> withValvePrefetch(int prefetch)
    {
        mValvePrefetch = prefetch;
        return this;
    }

    public RXBusBuilder<T, O> withOnNext(Action1<O> actionNext)
    {
        mActionNext = actionNext;
        return this;
    }

    public RXBusBuilder<T, O> withOnError(Action1<Throwable> actionerror)
    {
        mActionError = actionerror;
        return this;
    }

    public RXBusBuilder<T, O> withOnComplete(Action0 actionComplete)
    {
        mActionOnComplete= actionComplete;
        return this;
    }

    public RXBusBuilder<T, O> withObserver(Observer<O> subscriptionObserver)
    {
        mSubscriptionObserver = subscriptionObserver;
        return this;
    }

    public RXBusBuilder<T, O> withSubscriber(Subscriber<? super O> subscriber)
    {
        mSubscriber = subscriber;
        return this;
    }

    public RXBusBuilder<T, O> withObservableReadyListener(IRXBusObservableProcessor observableReadyListener)
    {
        mObservableProcessor = observableReadyListener;
        return this;
    }

    // -----------------------------
    // build functions
    // -----------------------------

    public Observable<O> buildObservable()
    {
        Observable<T> observable = null;
        if (mKey != null)
        {
            for (int i = 0; i < mKey.size(); i++)
            {
                if (i == 0)
                    observable = RXBus.get().observeEvent(mKey.get(i));
                else
                    observable = observable.mergeWith(RXBus.get().observeEvent(mKey.get(i)));
            }
        }
        else
            observable = RXBus.get().observeEvent(mKeyClass);

        if (mBackpressureObservableEnabled)
            observable = observable.onBackpressureBuffer();

        if (mQueueEvents)
            observable = observable.lift(new RxValve<T>(mObservableIsResumed, mValvePrefetch, mIsResumedProvider.isRXBusResumed()));

        Observable<O> processedObservable = null;
        if (mObservableProcessor == null)
            processedObservable = (Observable<O>) observable;
        else
            processedObservable = mObservableProcessor.onObservableReady(observable);

        processedObservable = applySchedular(processedObservable);
        return processedObservable;
    }

    public Subscription buildSubscription()
    {
        Observable<O> observable = buildObservable();

        if (mSubscriber == null && mSubscriptionObserver == null && mActionNext == null)
            throw new RuntimeException("Subscription can't be build, because no next action, subscriber nor observable was set!");

        if (mSubscriber == null && mSubscriptionObserver == null)
        {
            Action1<? super O> actionNext = mActionNext;
            if (mQueueEvents && mQueueSubscriptionSafetyCheckEnabled)
                actionNext = RXBusUtil.wrapQueueAction(mActionNext, mIsResumedProvider);

            if (mActionError != null && mActionOnComplete != null)
                return observable.subscribe(actionNext, mActionError, mActionOnComplete);
            else if (mActionError != null)
                return observable.subscribe(actionNext, mActionError);
            return observable.subscribe(actionNext);
        }
        else if (mSubscriber == null)
        {
            Observer<? super O> subscriptionObserver = mSubscriptionObserver;
            if (mQueueEvents && mQueueSubscriptionSafetyCheckEnabled)
                subscriptionObserver = RXBusUtil.wrapObserver(mSubscriptionObserver, mIsResumedProvider);
            return observable.subscribe(subscriptionObserver);
        }
        else if (mSubscriptionObserver == null)
        {
            Subscriber<? super O> subscriber = mSubscriber;
            if (mQueueEvents && mQueueSubscriptionSafetyCheckEnabled)
                subscriber = RXBusUtil.wrapSubscriber(mSubscriber, mIsResumedProvider);
            return observable.subscribe(subscriber);
        }
        else
            throw new RuntimeException("Subscription can't be build, because you have set more than one of following: nnext action, subscriber or observable!");
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
