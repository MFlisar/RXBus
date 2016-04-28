package com.michaelflisar.rxbus.rx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

/**
 * Created by flisar on 28.04.2016.
 */
public class SubscriptionManager
{
    // ---------------------------
    // Singleton
    // ---------------------------

    private static SubscriptionManager INSTANCE = null;

    public static SubscriptionManager get()
    {
        if (INSTANCE == null)
            INSTANCE = new SubscriptionManager();
        return INSTANCE;
    }

    private static HashMap<Class<?>, HashSet<Subscription>> mSubscriptions = new HashMap<>();

    // ---------------------------
    // public bus functions
    // ---------------------------

    public static void addSubscription(Object boundObject, Subscription subscription)
    {
        HashSet<Subscription> subscriptions = mSubscriptions.get(boundObject.getClass());
        if (subscriptions == null)
        {
            subscriptions = new HashSet<>();
            subscriptions.add(subscription);
            mSubscriptions.put(boundObject.getClass(), subscriptions);
        }
        else
            subscriptions.add(subscription);
    }

    public static void unsubscribe(Object boundObject)
    {
        HashSet<Subscription> subscriptions = mSubscriptions.get(boundObject.getClass());
        if (subscriptions != null)
        {
            Iterator<Subscription> iterator = subscriptions.iterator();
            while (iterator.hasNext())
                iterator.next().unsubscribe();
            mSubscriptions.remove(boundObject.getClass());
        }
    }
}
