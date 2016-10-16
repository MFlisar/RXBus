package com.michaelflisar.rxbus.demo;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.michaelflisar.rxbus.interfaces.IRXBusQueue;

import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Created by flisar on 28.04.2016.
 */
public class PauseAwareActivity extends AppCompatActivity implements IRXBusQueue
{
    private static final String TAG = PauseAwareActivity.class.getSimpleName();

    private final BehaviorSubject<Boolean> mResumedObject = BehaviorSubject.create(false);

    @Override
    protected void onResume()
    {
        super.onResume();
        mResumedObject.onNext(true);
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause()
    {
        mResumedObject.onNext(false);
        Log.d(TAG, "onPause");
        super.onPause();
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
