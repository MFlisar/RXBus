package com.michaelflisar.rxbus.demo;

import android.os.Bundle;
import android.util.Log;

import com.michaelflisar.rxbus.RXBus;
import com.michaelflisar.rxbus.interfaces.IRXBusIsResumedProvider;
import com.michaelflisar.rxbus.interfaces.IRXBusResumedListener;
import com.michaelflisar.rxbus.rx.RXUtil;
import com.michaelflisar.rxbus.rx.RxValve;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by flisar on 28.04.2016.
 */
public class DemoActivity extends PauseAwareActivity
{
    private static final String TAG = PauseAwareActivity.class.getSimpleName();

    private static List<Subscription> mSubscriptions = new ArrayList<>();

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null)
        {
            Observable<Boolean> observableIsResumed = RXUtil.createResumeStateObservable(this, new IRXBusResumedListener()
            {
                @Override
                public void onResumedChanged(boolean resumed) {
                    Log.d(TAG, "onResumedChanged - resumed=" + resumed);
                }
            });

            createSimpleObserver(true);
            createSimpleObserver(false);

            createQueueObserver(true, observableIsResumed);
            createQueueObserver(false, observableIsResumed);


            // lets send some sync events
            for (int i = 0; i < 5; i++)
                RXBus.get().sendEvent(getLogMessage("onCreate", "main thread i=" + i));

            // lets say another thread is currently emitting events => send some async events
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.d(TAG, "Thread startet...");
                    for (int i = 0; i < 5; i++)
                        RXBus.get().sendEvent(getLogMessage("onCreate", "some thread i=" + i));
                }
            }).start();
        }

        // to see the difference between the different buses and the threads they are observing, we make sure onCreate needs some time
        // so that the async events have some time to be queued
        try
        {
            Thread.sleep(5000);
        }
        catch (InterruptedException e)
        {

        }
    }

    @Override
    public void onPause()
    {
        RXBus.get().sendEvent(getLogMessage("onPause", "BEFORE on pause"));
        super.onPause();
        Log.d(TAG, "ACTIVITY PAUSED");
        RXBus.get().sendEvent(getLogMessage("onPause", "AFTER on pause"));
    }

    @Override
    public void onResume()
    {
        RXBus.get().sendEvent(getLogMessage("onResume", "BEFORE on resume"));
        super.onResume();
        Log.d(TAG, "ACTIVITY RESUMED");
        RXBus.get().sendEvent(getLogMessage("onResume", "AFTER on resume"));
    }

    @Override
    public void onDestroy()
    {
        // unsubscribe
        for (int i = 0; i < mSubscriptions.size(); i++)
            mSubscriptions.get(i).unsubscribe();
        super.onDestroy();
    }

    // -----------------------------
    // Bus observables
    // -----------------------------

    private void createSimpleObserver(final boolean observeOnBackground)
    {
        Observable<String> observable = RXBus.get()
                .observeEvent(String.class);
        if (observeOnBackground)
            observable = observable.compose(RXUtil.<String>applyBackgroundSchedulers());
        else
            observable = observable.compose(RXUtil.<String>applySchedulars());

        mSubscriptions.add(observable.subscribe(new Action1<String>()
        {
            @Override
            public void call(String s)
            {
                // this is called on the background!
                Log.d(TAG, "SIMPLE BUS (observeOnBackground=" + observeOnBackground + "): " + s + " | " + getIsResumedMessage());
            }
        }));
    }

    private void createQueueObserver(final boolean observeOnBackground, Observable<Boolean> observableIsResumed)
    {
        Observable<String> observable = RXBus.get()
                .observeEvent(String.class);

        observable = observable.lift(new RxValve<String>(observableIsResumed, 1000, isRXBusResumed()));

        if (observeOnBackground)
            observable = observable.compose(RXUtil.<String>applyBackgroundSchedulers());
        else
            observable = observable.compose(RXUtil.<String>applySchedulars());

        mSubscriptions.add(observable.subscribe(new Observer<String>()
        {
            @Override
            public void onCompleted() {
                Log.d(TAG, "QUEUED BUS (observeOnBackground=" + observeOnBackground + "): onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "QUEUED BUS (observeOnBackground=" + observeOnBackground + "): error=" + e.getMessage());
            }

            @Override
            public void onNext(String s) {
                if (isRXBusResumed())
                    Log.d(TAG, "QUEUED BUS (observeOnBackground=" + observeOnBackground + "): " + s + " | " + getIsResumedMessage());
                else
                    // resend it to bus, we don't want to loose it and we can't use it now. Only happens, if event was send somewhere in onPause shortly before activity was paused!
                    // will only be resend once, as currently the activity is paused
                    RXBus.get().sendEvent(s + " | POSTPONED");
            }
        }));
    }

    // -----------------------------
    // Logging
    // -----------------------------

    private String getLogMessage(String method, String msg)
    {
        return "[" + method + "] {" + Thread.currentThread().getName() + "} : " + msg;
    }

    private String getIsResumedMessage()
    {
        return "isResumed=" + isRXBusResumed();
    }

}