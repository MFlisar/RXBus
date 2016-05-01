# RXBus
RX based bus with lifecycle based queuing support

##What does it do?

* it allows you to post events to a bus
* it allows you to subscribe to special events whereever you want
* it allows you to queue events until an activity is resumed (to make sure views are accessable for example)
* it allows you to queue events as soon as activity is paused and emit events as soon soon as it is resumed
* it's very **lightweight**

##Demo

Just check out the DemoActivity, it will show the base usage and the difference between the `RXBus` and the `RXQueueBus`
 
##Gradle

1. add jitpack to your project's build.gradle:

   `maven { url "https://jitpack.io" }`
   
2. add the compile statement to your module (currently only snapshot exists):

   `compile 'com.github.MFlisar:RXBus:-SNAPSHOT'`
   
More about jitpack can be found here: https://jitpack.io/#MFlisar/RXBus/

##Usage (simple)

Just use the `RXBus` class and subscribe to a special event, that's it. Or use the `RXBusBuilder` for more flexible usage. Just like following:

* Variant 1:

    `Observable<TestEvent> simpleObservable1 = RXBus.get().observeEvent(TestEvent.class);`

* Variant 2:

    `Observable<TestEvent> simpleObservable2 = new RXBusBuilder<>(TestEvent.class).buildObservable();`
    
* Variant 3:

        Subscription simpleSubscription1 = new RXBusBuilder(TestEvent.class)
            .withOnNext(new Action1<TestEvent>() {
                @Override
                public void call(TestEvent event) {
                    // handle event...
                }
            })
            .buildSubscription();

####Sending an event
   
    // Send an event to the bus
    RXBus.get().sendEvent(new TestEvent());
    
##Usage (advanced)

You can use this library to subscribe to events and only get them when your activity is resumed, so that you can be sure views are available, for example. Just like following:

    Subscription queuedSubscription = new RXBusBuilder<>(String.class)
        // this enables the queuing mode!
        .queue(observableIsResumed, busIsResumedProvider)
        .withOnNext(new Action1<String>() {
            @Override
            public void call(String s) {
                // security check: it may happen that the valve evaluates the is resumed state while activity is resumed and that the event may be emited
                // when the activity is already paused => we just repost the event, this will only happen once, as the activity is currently paused
                // this is only the current workaround!!!

                if (RXUtil.safetyQueueCheck(s, DemoActivity.this))
                {
                    // handle the event, the activity is resumed!
                    // if activity is not resumed, the safetyQueueCheck function will resend the event when activity is resumed
                }
            }
        })
        .buildSubscription();

##Credits

The `RxValve` class is from this gist: https://gist.github.com/akarnokd/1c54e5a4f64f9b1e46bdcf62b4222f08
