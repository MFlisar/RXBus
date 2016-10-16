###Upgrade Notes

#### v0.6
* Remove all usages of `IRXBusIsResumedProvider` and `IRXBusResumedListener`
* for using queuing, the "queuer" now must implement the `IRXBusQueue` interface, see the `PauseAwareActivity` for an example
