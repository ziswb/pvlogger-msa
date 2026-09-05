# 0019 — Live KPI fan-out is a shared flow, not a hand-rolled scheduler

- Status: accepted
- Date: 2026-09-05

## Context

`pvlogger-frontend` pushes the latest KPI row to every browser over the
`/livedata` WebSocket (CONTEXT.md, "Live data"). The first implementation built
the fan-out by hand: a `MutableSharedFlow` without replay, a
`NotificationProducer` polling the data accessor, and a
`NotificationProducerScheduler` that reference-counted subscribers with an
`AtomicInteger` behind a `synchronized` block so that an idle dashboard caused
no frontend → data-accessor traffic.

Three defects followed from doing it by hand:

- The shared polling job was launched with `scope.launch` on the **first**
  subscriber's `DefaultWebSocketServerSession`. When that client disconnected
  while another was still connected, the counter stayed above zero (so nothing
  restarted the job) but the session scope was cancelled anyway — the job died
  and every remaining browser went silent until the last one left.
- No replay plus a 60 s initial delay meant a freshly connected browser saw no
  live values for a full minute, and a session shorter than that received
  nothing at all.
- An exception from the data accessor ended the producer flow permanently, and
  a subscriber that could not keep up applied backpressure to the shared
  `emit`, stalling every other session.

## Decision

The fan-out is expressed with the standard kotlinx.coroutines idiom in
`service/LiveKpiPublisher`:

```kotlin
poll().retryWhen { … }.shareIn(
    scope = application,
    started = SharingStarted.WhileSubscribed(stopTimeout = 5 s, replayExpiration = 60 s),
    replay = 1,
)
```

- `WhileSubscribed` does the reference counting — no `AtomicInteger`, no lock.
  Polling starts with the first session and stops 5 s after the last one, so the
  "no clients, no traffic" property is kept, and a page reload does not restart
  the loop.
- The sharing scope is the **`Application`**, never a session, so no client can
  take the loop down with it.
- `replay = 1` serves a joining or reconnecting browser the last known value
  immediately, and there is no initial delay before the first poll.
  `replayExpiration` bounds how stale that cached value may be.
- `retryWhen` keeps a data-accessor outage from ending the flow for good.
- Each session collects `.conflate()`d, so a slow client drops frames instead of
  starving the others.
- The data accessor is reached through the `LatestKpiSource` port, which makes
  the publisher testable on virtual time without a mocking framework.

The per-request timeout for the latest-KPI read lives in
`DataAccessorRestProxy` (15 s): the shared `PvHttpClient` allows 10 minutes for
the collectors' bulk writes, which is far past the next poll.

## Consequences

- Hand-rolled subscriber counting, scheduling or broadcast does not come back in
  this repo; the flow operators cover it, and their semantics are tested by
  kotlinx, not by us.
- `NotificationProducer`/`NotificationProducerScheduler` and their tests are
  gone; `LiveKpiPublisherTest` covers the cadence, the ref-counting, the two
  regressions above and the failure paths on `runTest` virtual time.
- Log shape changed: a `/livedata` session logs `session opened` / `session
  closed` at INFO, each pushed value stays `WebSocket: KPI values sent!` at
  INFO, and the per-poll REST call dropped to DEBUG. Note that Ktor's
  CallLogging emits its `101 Switching Protocols … in <n>ms` line only when a
  session *ends*, with `<n>` being the session's lifetime — an open WebSocket
  produces no such line.
- The `Application` scope is now load-bearing for a background loop. A second
  such consumer should follow the same shape rather than growing its own scope.
