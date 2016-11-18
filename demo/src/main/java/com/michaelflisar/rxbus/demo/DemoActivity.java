package com.michaelflisar.rxbus.demo;

import android.os.Bundle;
import android.util.Log;

import com.michaelflisar.rxbus.RXBus;
import com.michaelflisar.rxbus.RXBusBuilder;
import com.michaelflisar.rxbus.rx.RXBusMode;
import com.michaelflisar.rxbus.rx.RXSubscriptionManager;

import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by flisar on 28.04.2016.
 */
public class DemoActivity extends PauseAwareActivity
{
    private static final String TAG = DemoActivity.class.getSimpleName();
    private static final String TAG_NEW = "NEW|" + DemoActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // -----------------
        // Simple bus
        // -----------------

        testGeneral();
        testWithKeys();
        testAdvanced();

        // Variant 1: use the RXBus class directly
//            Observable<String> simpleObservable1 = RXBus.get().observeEvent(String.class);

        // Variant 2: use the RXBusBuilder
//            Observable<String> simpleObservable2 = new RXBusBuilder<>(String.class)
//                    .buildObservable();

        // Variant 3: use the RXBuilder and create a subscription
        Subscription simpleSubscription = RXBusBuilder.create(String.class)
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
        // optional: add subscription to the RXSubscriptionManager for convenience
        // this "bind" the subscription to the boundObject and offers a onliner function to unsubscribe all added subscriptions again
        RXSubscriptionManager.addSubscription(this, simpleSubscription);

        // -----------------
        // Queued bus
        // -----------------

        // Explanation this will retrieve all String events, if they are not exclusively bound to a key as well
        Subscription queuedSubscription = RXBusBuilder.create(String.class)
                // this enables the queuing mode!
                .queue(this)
                .withOnNext(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        // activity IS resumed, you can safely update your UI for example
                        Log.d(TAG, "QUEUED BUS: " + s + " | " + getIsResumedMessage());
                    }
                })
                .buildSubscription();
        // optional: add subscription to the RXSubscriptionManager for convenience
        // this "bind" the subscription to the boundObject and offers a onliner function to unsubscribe all added subscriptions again
        RXSubscriptionManager.addSubscription(this, queuedSubscription);

        Subscription queuedSubscriptionNEW = new RXBus.Builder<>(String.class)
                .withQueuing(this)
                .build()
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        // activity IS resumed, you can safely update your UI for example
                        Log.d(TAG_NEW, "QUEUED BUS: " + s + " | " + getIsResumedMessage());
                    }
                });
        RXSubscriptionManager.addSubscription(this, queuedSubscriptionNEW);

        // -----------------
        // Usage with keys
        // you can use Integer and String keys!
        // -----------------

        // Explanation: this will retrieve all String events that are bound to the key passed to the builder
        Subscription queuedSubscriptionKey1 = RXBusBuilder.create(String.class)
                // this enables the key bound mode
                .withKey(R.id.custom_event_id_1)
                .queue(this)
                .withOnNext(new Action1<String>()
                {
                    @Override
                    public void call(String s)
                    {
                        // activity IS resumed, you can safely update your UI for example
                        Log.d(TAG, "QUEUED BUS - KEY 1: " + s + " | " + getIsResumedMessage());
                    }
                })
                .buildSubscription();
        // optional: add subscription to the RXSubscriptionManager for convenience
        // this "bind" the subscription to the boundObject and offers a onliner function to unsubscribe all added subscriptions again
        RXSubscriptionManager.addSubscription(this, queuedSubscriptionKey1);

        Subscription queuedSubscriptionKey1NEW = new RXBus.Builder<>(String.class)
                .withQueuing(this)
                .withKey(R.id.custom_event_id_1)
                .build()
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        // activity IS resumed, you can safely update your UI for example
                        Log.d(TAG_NEW, "QUEUED BUS: " + s + " | " + getIsResumedMessage());
                    }
                });
        RXSubscriptionManager.addSubscription(this, queuedSubscriptionKey1NEW);

        // Explanation: this will retrieve all String events that are bound to the key passed to the builder
        Subscription queuedSubscriptionKey2 = RXBusBuilder.create(String.class)
                .withKey(R.id.custom_event_id_2)
                .queue(this)
                .withOnNext(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        // activity IS resumed, you can safely update your UI for example
                        Log.d(TAG, "QUEUED BUS - KEY 2: " + s + " | " + getIsResumedMessage());
                    }
                })
                .buildSubscription();
        // optional: add subscription to the RXSubscriptionManager for convenience
        // this "bind" the subscription to the boundObject and offers a onliner function to unsubscribe all added subscriptions again
        RXSubscriptionManager.addSubscription(this, queuedSubscriptionKey2);

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

        // lets send some events to the subscription made in the base activity
        sendTestIssue6Messages("onCreate");
    }

    @Override
    public void onPause()
    {
        sendTestIssue6Messages("before onPause");

        RXBus.get().sendEvent(getLogMessage("onPause", "BEFORE on pause"));
        super.onPause();
        Log.d(TAG, "ACTIVITY PAUSED");
        RXBus.get().sendEvent(getLogMessage("onPause", "AFTER on pause"));

        // lets send some events to the subscription made in the base activity
        sendTestIssue6Messages("after onPause");
    }

    @Override
    public void onResume()
    {
        sendTestIssue6Messages("before onResume");

        RXBus.get().sendEvent(getLogMessage("onResume", "BEFORE on resume"));
        super.onResume();
        Log.d(TAG, "ACTIVITY RESUMED");
        RXBus.get().sendEvent(getLogMessage("onResume", "AFTER on resume"));

        sendTestIssue6Messages("after onResume");
    }

    @Override
    public void onDestroy()
    {
        // unsubscribe - we used the RXSubscriptionManager for every subscription and bound all subscriptions to this class,
        // so following will safely unsubscribe every subscription
        RXSubscriptionManager.unsubscribe(this);
        super.onDestroy();
    }

    private void sendTestIssue6Messages(String tag)
    {
        for (int i = 0; i < 5; i++)
            RXBus.get().sendEvent("Test ISSUE 6 A [" + tag + "] message " + i, "TEST_ISSUE_6_A");
        for (int i = 0; i < 5; i++)
            RXBus.get().sendEvent("Test ISSUE 6 B [" + tag + "]message " + i, "TEST_ISSUE_6_B");
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
        return "isResumed=" + isBusResumed();
    }

    // -----------------------------
    // Tests
    // -----------------------------

    private void testGeneral()
    {
        // 1) Just subscribe to a bus event => use the builders subscribe overload for this!
        Subscription subscriptionManual = RXBus.Builder.create(String.class)
                .subscribe(new Action1<String>(){
                    @Override
                    public void call(String s) {
                        Log.d(TAG_NEW, "SIMPLE BUS: " + s + " | " + getIsResumedMessage());
                    }
                });
        // ATTENTION: this subscription MUST be handled by you, unsubscribe whenever you want!
        // currently it will leak the activity!!!

        // 2) Subscribe to an event and let RXSubscriptionManager manage your subscription - you just need to call
        // RXSubscriptionManager.unsubscribe(boundObject); to unsubscribe ALL subscriptions for a bound object
        // additionally this here enable queuing + emits items on the main thread
        Subscription subscriptionManaged = RXBus.Builder.create(String.class)
                .withQueuing(this)          // optional: if enabled, events will be queued while the IRXBusQueue is paused!
                .withBound(this)                // optional: this binds the subcritpion to this object and you can unsubscribe all bound subscriptions at once
                .withMode(RXBusMode.Main)   // optional: set the thread to main or background if wanted, events will be emitted on the corresponding thread
                .subscribe(new Action1<String>(){
                    @Override
                    public void call(String s) {
                        Log.d(TAG_NEW, "SIMPLE BUS (BOUND): " + s + " | " + getIsResumedMessage());
                    }
                });

        // 3) Get a simple observable and do whatever you want with it
        // all RXBus options like queuing and keys are available here as well!!!
        Observable<String> observable = RXBus.Builder.create(String.class)
                // optional:
//                .withQueuing(this)
//                .withKey(...)
                .build();
        // do something with this observable...
    }

    private void testWithKeys()
    {
        // you can use everything that is shown in testGeneral here as well, example will not show all possible combinations!

        // 1) Subscribe to a string event and only listen to a special key (+ queuing is enabled as well)
        // Subscription is managed automatically as well by RXSubscriptionManager
        RXBus.Builder.create(String.class)
                // all optional!!!
                .withQueuing(this)
                .withBound(this)
                .withKey(R.id.custom_event_id_1) // you may add multiple keys as well!
                .withMode(RXBusMode.Main)
                .subscribe(new Action1<String>(){
                    @Override
                    public void call(String s) {
                        Log.d(TAG_NEW, "KEY BUS (QUEUED and BOUND): " + s + " | " + getIsResumedMessage());
                    }
                });

        Observable<String> observable = RXBus.Builder.create(String.class)
                .withQueuing(this)
                .withKey(R.id.custom_event_id_1) // you may add multiple keys as well!
                .build();
    }

    private void testAdvanced()
    {
        // 1) subscribe to a string event but emit integers => just pass in a transformer to the subcribe function!
        RXBus.Builder.create(String.class)
                .withQueuing(this)
                .withBound(this)
                .withKey(R.id.custom_event_id_1) // you may add multiple keys as well!
                .withMode(RXBusMode.Main)
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer s) {
                        Log.d(TAG_NEW, "KEY BUS (QUEUED and BOUND): " + s + " | " + getIsResumedMessage());
                    }
                }, new Observable.Transformer<String, Integer>() {
                    @Override
                    public Observable<Integer> call(Observable<String> observable) {
                        return observable
                                .map(new Func1<String, Integer>() {
                                    @Override
                                    public Integer call(String s) {
                                        return s.hashCode();
                                    }
                                });
                    }
                });

        // 2) You need more control or dont want to use the transformer to compose a new observable? Then create an observable only and do the rest yourself!
        Observable<String> observable = RXBus.Builder.create(String.class)
                .withQueuing(this)
                .withKey(R.id.custom_event_id_1) // you may add multiple keys as well!
                .build();

        // do whatever youn want with the observable
        Observable result = observable
//                ....
//                .toList(...)
//                .flatMap(...)
//                .map(...)
        ;
        Subscription subscription = result.subscribe(new Action1() {
            @Override
            public void call(Object o) {
                // ...
            }
        });
        // Don't forget to manage the subcription!! If you want you can use the RXSubscriptionManager manually here:
        RXSubscriptionManager.addSubscription(this, subscription);

    }
}