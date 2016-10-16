###Upgrade Notes

#### v0.6
* Remove all usages of `IRXBusIsResumedProvider` and `IRXBusResumedListener`
* for using queuing, the "queuer" now must implement the `IRXBusQueue` interface, see the `PauseAwareActivity` for an example:  [`PauseAwareActivity`](https://github.com/MFlisar/RXBus/blob/master/demo/src/main/java/com/michaelflisar/rxbus/demo/PauseAwareActivity.java)
* the before mentioned "queuer" must be pased to the `RXBusBuilder.queue` function now
