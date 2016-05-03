package com.michaelflisar.rxbus.demo;

import android.os.Bundle;
import android.util.Log;

import com.michaelflisar.rxbus.RXBus;
import com.michaelflisar.rxbus.RXBusBuilder;
import com.michaelflisar.rxbus.interfaces.IRXBusResumedListener;
import com.michaelflisar.rxbus.rx.RXUtil;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by flisar on 28.04.2016.
 */
public class DemoActivity extends PauseAwareActivity
{
    private static final String TAG = PauseAwareActivity.class.getSimpleName();

    // for demo purposes we use a static list and only add items to it when activity is created
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

            // -----------------
            // Simple bus
            // -----------------

            // Variant 1: use the RXBus class directly
//            Observable<String> simpleObservable1 = RXBus.get().observeEvent(String.class);

            // Variant 2: use the RXBusBuilder
//            Observable<String> simpleObservable2 = new RXBusBuilder<>(String.class)
//                    .buildObservable();

            // Variant 3: use the RXBuilder and create a subscription
            Subscription simpleSubscription = new RXBusBuilder(String.class)
                    // OPTIONAL: define on which thread you want the bus to emit items
//                    .withBusMode(RXBusMode.Background || RXBusMode.Main || RXBusMode.None)
                    // REQUIRED: add an observer, add actions (next, error, complete) or add an subscriber, but only on of those 3!
                    .withOnNext(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            Log.d(TAG, "SIMPLE BUS: " + s + " | " + getIsResumedMessage());
                        }
                    })
                    .buildSubscription();
            mSubscriptions.add(simpleSubscription);

            // -----------------
            // Queued bus
            // -----------------

            // Explanation this will retrieve all String events, if they are not exclusively bound to a key as well
            Subscription queuedSubscription = new RXBusBuilder<>(String.class)
                    // this enables the queuing mode!
                    .queue(observableIsResumed, this)
                    .withOnNext(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            // activity IS resumed, you can safely update your UI for example
                            Log.d(TAG, "QUEUED BUS: " + s + " | " + getIsResumedMessage());
                        }
                    })
                    .buildSubscription();
            mSubscriptions.add(queuedSubscription);

            // -----------------
            // Usage with keys
            // you can use Integer and String keys!
            // -----------------

            // Explanation: this will retrieve all String events that are bound to the key passed to the builder
            Subscription queuedSubscriptionKey1 = new RXBusBuilder<>(String.class)
                    // this enables the key bound mode
                    .withKey(R.id.custom_event_id_1)
                    .queue(observableIsResumed, this)
                    .withOnNext(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            // activity IS resumed, you can safely update your UI for example
                            Log.d(TAG, "QUEUED BUS - KEY 1: " + s + " | " + getIsResumedMessage());
                        }
                    })
                    .buildSubscription();
            mSubscriptions.add(queuedSubscriptionKey1);

            // Explanation: this will retrieve all String events that are bound to the key passed to the builder
            Subscription queuedSubscriptionKey2 = new RXBusBuilder<>(String.class)
                    .withKey(R.id.custom_event_id_2)
                    .queue(observableIsResumed, this)
                    .withOnNext(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            // activity IS resumed, you can safely update your UI for example
                            Log.d(TAG, "QUEUED BUS - KEY 2: " + s + " | " + getIsResumedMessage());
                        }
                    })
                    .buildSubscription();
            mSubscriptions.add(queuedSubscriptionKey2);

            // -----------------
            // Send some events
            // -----------------

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

            // lets send some events bound to a key (can be a string or an integer)
            // 1 loop: sends events to the given key ONLY
            // 2 loop: sends events to all observers of the key AND to all simple String event observer
            for (int i = 0; i < 5; i++)
                RXBus.get().sendEvent(getLogMessage("onCreate", "KEY 1 main thread i=" + i), R.id.custom_event_id_1);
            for (int i = 0; i < 5; i++)
                RXBus.get().sendEvent(getLogMessage("onCreate", "KEY 2 (AND ALL String listeners) main thread i=" + i), R.id.custom_event_id_2, true);

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