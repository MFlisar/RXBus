# RXBus
RX based bus with lifecycle based queuing support

#What does it do?

* it allows you to post events to a bus
* it allows you to subscribe to special events whereever you want
* it allows you to queue events until an activity is resumed (to make sure views are accessable for example)
* it allows you to queue events as soon as activity is paused and emit events as soon soon as it is resumed
* it's very **lightweight**
 
#Usage (simple)

Just use the `RXBus` class and subscribe to a special event, that's it. Just like following:

    Observable<TestEvent> observableTest = RXBus.get().observeEvent(TestEvent.class);
    // do whatever you want with this observable => you'll get every event until you unsubscribe!
    observableTest1.subscribe(...)
   
    // Send an event to the bus
    RXBus.get().sendEvent(new TestEvent());
    
#Usage (advanced)

You can use this library to subscribe to events and only get them when your activity is resumed, so that you can be sure views are available, for example. Just like following:

    Observable<RXQueueEvent<TestEvent>> observableTest = RXQueueBus.get().observeEventOnResume(TestEvent.class, this);
    // subscribe to this observable whenever you want, from the point where you subscribed, you will get ALL
    // events until you unsubscribe, but you won't get any event BEFORE the activity is resumed
    // and not after the activity is paused! unsubscribe in your activity's onDestroy method!
    // See how to do that in the simple demo activity
    RXQueueBus.get().subscribe(observableTest, this, new Observer<TestEvent>()
    {
        @Override
        public void onCompleted() { }

        @Override
        public void onError(Throwable e) { }

        @Override
        public void onNext(TestEvent data)
        {
            // activity IS resumed!!! update your views...
        }
	   });

