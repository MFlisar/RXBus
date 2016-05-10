package com.michaelflisar.rxbus.rx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import rx.Subscription;

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

    private static HashMap<Object, HashSet<Subscription>> mSubscriptions = new HashMap<>();

    // ---------------------------
    // public bus functions
    // ---------------------------

    public static void addSubscription(Object boundObject, Subscription subscription)
    {
        HashSet<Subscription> subscriptions = mSubscriptions.get(boundObject);
        if (subscriptions == null)
        {
            subscriptions = new HashSet<>();
            subscriptions.add(subscription);
            mSubscriptions.put(boundObject, subscriptions);
        }
        else
            subscriptions.add(subscription);
    }

    public static void unsubscribe(Object boundObject)
    {
        HashSet<Subscription> subscriptions = mSubscriptions.get(boundObject);
        if (subscriptions != null)
        {
            Iterator<Subscription> iterator = subscriptions.iterator();
            while (iterator.hasNext())
                iterator.next().unsubscribe();
            mSubscriptions.remove(boundObject);
        }
    }
}
