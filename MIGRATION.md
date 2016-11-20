###Upgrade Notes

#### v0.9

* Remove all usages of `IRXBusObservableProcessor`, you can now pass a native rx java `Transformer` to the builder instead
* Renamings:
  * `RXBusBuilder.withBusMode` => `RXBusBuilder.withMode`
  * `RXBusBuilder.queue` => `RXBusBuilder.withQueuing`
  
  * `RXBusBuilder.withOnNext` and similar => `RXBusBuilder.subscribe` and this will now directly return a Subscription`
  
* NEW:
  * `RXBusBuilder.withBound(objectToBind)` => this will now directly call `RXSubscriptionManager.addSubscription(objectToBind, subscription);` when you call `RXBusBuilder.subscribe`

#### v0.6
* Remove all usages of `IRXBusIsResumedProvider` and `IRXBusResumedListener`
* for using queuing, the "queuer" now must implement the `IRXBusQueue` interface, see the `PauseAwareActivity` for an example:  [`PauseAwareActivity`](https://github.com/MFlisar/RXBus/blob/master/demo/src/main/java/com/michaelflisar/rxbus/demo/PauseAwareActivity.java)
* the before mentioned "queuer" must be passed to the `RXBusBuilder.queue` function now
