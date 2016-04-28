package com.michaelflisar.rxbus.demo;

import android.support.v7.app.AppCompatActivity;

import com.michaelflisar.rxbus.interfaces.IRXBusIsResumedProvider;
import com.michaelflisar.rxbus.interfaces.IRXBusResumedListener;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by flisar on 28.04.2016.
 */
public class PauseAwareActivity extends AppCompatActivity implements IRXBusIsResumedProvider
{
    private boolean mPaused = true;
    private HashSet<IRXBusResumedListener> mListeners = new HashSet<>();

    @Override
    protected void onResume()
    {
        super.onResume();
        mPaused = false;
        Iterator<IRXBusResumedListener> iterator = mListeners.iterator();
        while (iterator.hasNext())
            iterator.next().onResumedChanged(true);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mPaused = true;
        Iterator<IRXBusResumedListener> iterator = mListeners.iterator();
        while (iterator.hasNext())
            iterator.next().onResumedChanged(false);
    }

    // --------------
    // Interface RXBus
    // --------------

    @Override
    public boolean isRXBusResumed()
    {
        return mPaused;
    }

    @Override
    public void addResumedListener(IRXBusResumedListener listener, boolean callListener) {
        mListeners.add(listener);
        if (callListener)
            listener.onResumedChanged(isRXBusResumed());
    }

    @Override
    public void removeResumedListener(IRXBusResumedListener listener) {
        mListeners.remove(listener);
    }
}
