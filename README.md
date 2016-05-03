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
    compile 'com.github.MFlisar:RXBus:0.3'
}
```
### Usage

**Demo**

Just check out the [DemoActivity](https://github.com/MFlisar/RXBus/blob/master/demo/src/main/java/com/michaelflisar/rxbus/demo/DemoActivity.java), it will show the base usage and the difference between the default and the queued `RXBus`

**Simple usage**

Just use the `RXBus` class and subscribe to a special event, that's it. Or use the `RXBusBuilder` for more flexible usage. Just like following:
```java
// Variant 1:
Observable<TestEvent> simpleObservable1 = RXBus.get().observeEvent(TestEvent.class);

// Variant 2:
Observable<TestEvent> simpleObservable2 = new RXBusBuilder<>(TestEvent.class).buildObservable();

// Variant 3 - with the BUILDER:
Subscription simpleSubscription1 = new RXBusBuilder(TestEvent.class)
    .withOnNext(new Action1<TestEvent>() {
        @Override
        public void call(TestEvent event) {
            // handle event...
        }
    })
    .buildSubscription();
```
**Sending an event**
```java
// Send an event to the bus
RXBus.get().sendEvent(new TestEvent());
```
**Advanced usage** 

You can use this library to subscribe to events and only get them when your activity is resumed, so that you can be sure views are available, for example. Just like following:
```java
Subscription queuedSubscription = new RXBusBuilder<>(String.class)
    // this enables the queuing mode!
    .queue(observableIsResumed, busIsResumedProvider)
    .withOnNext(new Action1<String>() {
        @Override
        public void call(String s) {
            // activity IS resumed, you can safely update your UI for example
        }
    })
    .buildSubscription();
```

### TODO

* instead of "listening" to class bound events, an additional key based bus should be added to make it possible to selectively listen to events of one kind...
* 
### Credits

The `RxValve` class is from this gist: https://gist.github.com/akarnokd/1c54e5a4f64f9b1e46bdcf62b4222f08
