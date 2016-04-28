package com.michaelflisar.rxbus.demo;

import android.app.Activity;
import android.util.Log;

import com.michaelflisar.rxbus.RXBus;
import com.michaelflisar.rxbus.interfaces.IRXBusIsResumedProvider;
import com.michaelflisar.rxbus.queued.RXQueueBus;
import com.michaelflisar.rxbus.queued.RXQueueEvent;

import rx.Observable;

/**
 * Created by flisar on 28.04.2016.
 */
public class PauseAwareActivity extends Activity implements IRXBusIsResumedProvider
{
    private boolean mPaused = true;

    @Override
    protected void onResume()
    {
        super.onResume();
        mPaused = false;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mPaused = true;
    }

    // --------------
    // Interface RXBus
    // --------------

    @Override
    public boolean isRXBusResumed()
    {
        return mPaused;
    }
}
