package com.michaelflisar.rxbus.rx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by flisar on 28.04.2016.
 */
public class RXSubscriptionManager
{
    // ---------------------------
    // Singleton
    // ---------------------------

    private static RXSubscriptionManager INSTANCE = null;

    public static RXSubscriptionManager get()
    {
        if (INSTANCE == null)
            INSTANCE = new RXSubscriptionManager();
        return INSTANCE;
    }

    private static HashMap<Class<?>, CompositeSubscription> mSubscriptions = new HashMap<>();

    // ---------------------------
    // public bus functions
    // ---------------------------

    public void addSubscription(Object boundObject, Subscription subscription)
    {
        CompositeSubscription subscriptions = mSubscriptions.get(boundObject.getClass());
        if (subscriptions == null)
        {
            subscriptions = new CompositeSubscription();
            subscriptions.add(subscription);
            mSubscriptions.put(boundObject.getClass(), subscriptions);
        }
        else
            subscriptions.add(subscription);
    }

    public void unsubscribe(Object boundObject)
    {
        CompositeSubscription subscriptions = mSubscriptions.get(boundObject.getClass());
        if (subscriptions != null)
        {
            subscriptions.clear();
            mSubscriptions.remove(boundObject.getClass());
        }
    }
}
