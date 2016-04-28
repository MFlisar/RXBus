# RXBus
RX based bus with lifecycle based queuing support

#What does it do?

* it allows you to post events to a bus
* it allows you to subscribe to special events whereever you want
* it allows you to queue events until an activity is resumed (to make sure views are accessable for example)
* it's very **lightweight**
 
#Usage (simple)

Just use the `RXBus` class and subscribe to a special event, that's it. Just like following:

    Observable<TestEvent> observable = RXBus.get().subscribe(TestEvent.class);
    // do whatever you want with this observable
    
#Usage (advanced)

You can use this library to subscribe to events and only get them when your activity is resumed, so that you can be sure views are available, for example. Just like following:

    Observable<TestEvent> observable = RXBus.get().subscribe(TestEvent.class, Activity.this);
    // subscribe to this observable whenever you want, from the point where you subscribed, you will get ALL
    // events until you unsubscribe, but you won't get any event BEFORE the activity is resumed
    // and not after the activity is paused! unsubscribe in your activity's onDestroy method!
