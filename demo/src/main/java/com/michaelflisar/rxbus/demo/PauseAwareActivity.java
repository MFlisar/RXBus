package com.michaelflisar.rxbus.demo;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.michaelflisar.rxbus.RXBusBuilder;
import com.michaelflisar.rxbus.interfaces.IRXBusQueue;
import com.michaelflisar.rxbus.rx.RXBusMode;
import com.michaelflisar.rxbus.rx.RXSubscriptionManager;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

/**
 * Created by flisar on 28.04.2016.
 */
public class PauseAwareActivity extends AppCompatActivity implements IRXBusQueue
{
    private static final String TAG = "RXBus - " + PauseAwareActivity.class.getSimpleName();

    private final BehaviorSubject<Boolean> mResumedObject = BehaviorSubject.create(false);

    public PauseAwareActivity()
    {
        super();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG, "BASE BEFORE BUS onResume");
        mResumedObject.onNext(true);
        Log.d(TAG, "BASE AFTER BUS onResume");
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "BASE BEFORE BUS onPause");
        mResumedObject.onNext(false);
        Log.d(TAG, "BASE AFTER BUS onPause");
        super.onPause();
    }

    @Override
    public void onDestroy()
    {
        RXSubscriptionManager.unsubscribe(this);
        super.onDestroy();
    }

    // --------------
    // Interface RXBus
    // --------------

    @Override
    public boolean isBusResumed()
    {
        return mResumedObject.getValue();
    }

    @Override
    public Observable<Boolean> getResumeObservable()
    {
        return mResumedObject;
    }
}
