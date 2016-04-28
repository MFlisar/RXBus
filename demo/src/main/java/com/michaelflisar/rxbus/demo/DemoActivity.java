package com.michaelflisar.rxbus.demo;

import android.os.Bundle;
import android.util.Log;

import com.michaelflisar.rxbus.RXBus;
import com.michaelflisar.rxbus.queued.RXQueueBus;
import com.michaelflisar.rxbus.queued.RXQueueEvent;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by flisar on 28.04.2016.
 */
public class DemoActivity extends PauseAwareActivity
{
    private static final String TAG = PauseAwareActivity.class.getSimpleName();

    private Subscription mSubscription1 = null;
    private Subscription mSubscription2 = null;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null)
        {
            // TEST observable, which will retrieve ALL events until this activity is destroyed!
            Observable<String> observable = RXBus.get().observeEvent(String.class);
            mSubscription1 = observable.subscribe(new Action1<String>()
            {
                @Override
                public void call(String s)
                {
                    Log.d(TAG, "SIMPLE BUS onNext: " + s + " | " + isRXBusResumed());
                }
            });
            Observable<RXQueueEvent<String>> observableTest = RXQueueBus.get().observeEventOnResume(String.class, this);
            mSubscription2 = RXQueueBus.get().subscribe(observableTest, this, new Observer<String>()
            {
                @Override
                public void onCompleted()
                {

                }

                @Override
                public void onError(Throwable e)
                {

                }

                @Override
                public void onNext(String data)
                {
                    Log.d(TAG, "QUEUED BUS onNext: " + data + " | " + isRXBusResumed());
                }
            });

            for (int i = 0; i < 10; i++)
                RXBus.get().sendEvent("Send in FIRST onCreate: i=" + i);
        }
    }

    @Override
    public void onPause()
    {
        RXBus.get().sendEvent("PAUSE 1 - Send BEFORE onPause");
        super.onPause();
        Log.d(TAG, "ACTIVITY PAUSED");
        RXBus.get().sendEvent("PAUSE 2 - Send AFTER onPause");
    }

    @Override
    public void onResume()
    {
        RXBus.get().sendEvent("RESUME 1 - Send BEFORE onResume");
        super.onResume();
        Log.d(TAG, "ACTIVITY RESUMED");
        // tell the queued bus, that this activity was resumed, so that it can try to resend all events that are queued based on this activities resume state
        RXQueueBus.get().resume(this);
        RXBus.get().sendEvent("RESUME 1 - Send AFTER onResume");
    }

    @Override
    public void onDestroy()
    {
        // unsubscribe
        mSubscription1.unsubscribe();
        mSubscription2.unsubscribe();

        // clean all class dependant variables and make sure any reference to this class is removed
        RXQueueBus.get().clean(this);

        super.onDestroy();
    }
}