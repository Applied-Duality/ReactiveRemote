ReactiveRemote
==============

Observables in the Cloud

Task[T] is an explicit representation of an Observable that returns a single result.
We don't use Future[T] since we want to cancel onComplete (subscribe).

The signatures and structure of the other types mirror those in regular Rx
except that all return types are wrapped in Task[...].

Schedulers are much simplified, since we only need to schedule
work on a remote scheduler once, and there the regular scheduler is invoked.