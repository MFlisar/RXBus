[![Release](https://jitpack.io/v/MFlisar/RXBus.svg)](https://jitpack.io/#MFlisar/RXBus)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RXBus-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/3520)

### What does it do?

* it allows you to *post events* to a bus
* it allows you to *subscribe to special events* whereever you want
* it allows you to *queue events* until an activity is resumed (to make sure views are accessable for example)
* it allows you to queue events as soon as activity is paused and emit events as soon soon as it is resumed
* it's very **lightweight**
 
### Gradle (via [JitPack.io](https://jitpack.io/))

1. add jitpack to your project's `build.gradle`:
```groovy
repositories {
    maven { url "https://jitpack.io" }
}
```
2. add the compile statement to your module's `build.gradle`:
```groovy
dependencies {
    compile 'com.github.MFlisar:RXBus:0.5'
}
```
### Usage

*Content*

- [Demo](#demo)
- [Simple usage](#simple-usage)
- [Sending an event](#sending-an-event)
- [Advanced usage - QUEUING](#advanced-usage---queuing)
- [Advanced usage - KEYS](#advanced-usage---keys)
- [Advanced usage - bus observable processor](#advanced-usage---bus-observable-processor)
- [Helper class - `RXSubscriptionManager`](#helper-class---rxsubscriptionmanager)

#####Demo

Just check out the [DemoActivity](https://github.com/MFlisar/RXBus/blob/master/demo/src/main/java/com/michaelflisar/rxbus/demo/DemoActivity.java), it will show the base usage and the difference between the default and the queued `RXBus`

#####Simple usage

Just use the `RXBus` class and subscribe to a special event, that's it. Or use the `RXBusBuilder` for more flexible usage. Just like following:
```java
// Variant 1 - create observable only:
Observable<TestEvent> simpleObservable1 = RXBus.get().observeEvent(TestEvent.class);

// Variant 2 - create observable with the BUILDER:
Observable<TestEvent> simpleObservable2 = RXBusBuilder.create(TestEvent.class).buildObservable();

// Variant 3 - subscribe with the BUILDER:
Subscription simpleSubscription1 = RXBusBuilder.create(TestEvent.class)
    .withOnNext(new Action1<TestEvent>() {
        @Override
        public void call(TestEvent event) {
            // handle event...
            
            // event MUST have been send with either of following:
            // RXBus.get().sendEvent(new TestEvent()); => class bound bus usage
            // RXBus.get().sendEvent(new TestEvent(), R.id.observer_key_1, true); => key bound bus usage, with sendToDefaultBusAsWell = true, which will result in that all class bound observers (like this one) retrieve this event as well
        }
    })
    .buildSubscription();
```
#####Sending an event
```java
// Send an event to the bus - all observers that observe this class WITHOUT a key will receive this event
RXBus.get().sendEvent(new TestEvent());
// Send an event to the bus - only observers that observe the class AND key will receive this event
RXBus.get().sendEvent(new TestEvent(), R.id.observer_key_1);
// Send an event to the bus - all observers that either observe the class or the class AND key will receive this event
RXBus.get().sendEvent(new TestEvent(), R.id.observer_key_1, true);
```
#####Advanced usage - QUEUING

You can use this library to subscribe to events and only get them when your activity is resumed, so that you can be sure views are available, for example. Just like following:
```java
Subscription queuedSubscription = RXBusBuilder.create(TestEvent.class)
    // this enables the queuing mode!
    .queue(observableIsResumed, busIsResumedProvider)
    .withOnNext(new Action1<TestEvent>() {
        @Override
        public void call(TestEvent s) {
            // activity IS resumed, you can safely update your UI for example
            
            // event MUST have been send with either of following:
            // RXBus.get().sendEvent(new TestEvent()); => class bound bus usage
            // RXBus.get().sendEvent(new TestEvent(), R.id.observer_key_1, true); => key bound bus usage, with sendToDefaultBusAsWell = true, which will result in that all class bound observers (like this one) retrieve this event as well
        }
    })
    .buildSubscription();
```

#####Advanced usage - KEYS

You can use this library to subscribe to events of a typ and ONLY get them when it was send to the bus with a special key (and only when your activity is resumed, as this example shows via `.queue()`), so that you can distinct event subscriptions of the same class based on a key (the key can be an `Integer` or a `String`). Just like following:
```java
Subscription queuedSubscription = RXBusBuilder.create(String.class)
    // this enables the binding to the key
    .withKey(R.id.observer_key_1) // you can provide multiple keys as well
    .queue(observableIsResumed, busIsResumedProvider)
    .withOnNext(new Action1<String>() {
        @Override
        public void call(String s) {
            // activity IS resumed, you can safely update your UI for example
            
            // event MUST have been with either of those:
            // RXBus.get().sendEvent(new TestEvent(), R.id.observer_key_1); => key bound bus usage, class bound observers WON't retrieve this event as well!
            // RXBus.get().sendEvent(new TestEvent(), R.id.observer_key_1, true); => key bound bus usage, with sendToDefaultBusAsWell = true, resulting in class bound observers WILL retrieve this event as well!
        }
    })
    .buildSubscription();
```

#####Advanced usage - bus observable processor

Use this if you want to process the observed event before you emit it.

```java
// for example, instead of observing a string, you observe the hash of the string
IRXBusObservableProcessor observableProcessor = new IRXBusObservableProcessor<String, Integer>()
{
    @Override
    public Observable<Integer> onObservableReady(Observable<String> observable)
    {
        // do anything with the observable and return on of the processed type in the end
        return observable.map(new Func1<String, Integer>()
        {
            @Override
            public Integer call(String s)
            {
                    return s.hashCode();
            }
        });
    }
};
RXBusBuilder.create(String.class, Integer.class, observableProcessor)
    .withOnNext(new Action1<Integer>()
    {
        @Override
        public void call(Integer integer)
        {
            // here you get the hash instead of the string
        }
    })
    .buildSubscription();
```

#####Helper class - `RXSubscriptionManager`

This class helps to bind `subscriptions` to objects and offers an easy way to unsubscribe all `subscriptions` that are bound to an object at once.

```java
Subscription subscription = RXBusBuilder.create(...).buildSubscription();
RXSubscriptionManager.addSubscription(activity, subscription);
```

Now you only have to make sure to unsubscribe again like following:
```java
RXSubscriptionManager.unsubscribe(activity);
```

This will remove ANY subscription that is bound to `activity` and therefore this can be used in your `activity's` `onDestroy` method to make sure ALL subscriptions are unsubscribed at once and that you don't leak the activity.


### Credits

The `RxValve` class is from this gist: https://gist.github.com/akarnokd/1c54e5a4f64f9b1e46bdcf62b4222f08
