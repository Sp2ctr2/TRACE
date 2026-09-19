package app.saeon.trace.data

import android.os.SystemClock
import app.saeon.trace.core.Fixtures

/** A presentation-date clock with real elapsed-time freshness. It survives a
 * restart using the last persisted wall-time anchor; monotonic while running.
 */
class DemoClock {
    private var base: Long = Fixtures.epoch
    private var anchor: Long = SystemClock.elapsedRealtime()
    @Synchronized fun now(): Long = base + (SystemClock.elapsedRealtime() - anchor).coerceAtLeast(0)
    @Synchronized fun restore(demoTime: Long, wallTime: Long) {
        base = demoTime + (System.currentTimeMillis() - wallTime).coerceAtLeast(0)
        anchor = SystemClock.elapsedRealtime()
    }
    @Synchronized fun reset() { base = Fixtures.epoch; anchor = SystemClock.elapsedRealtime() }
    @Synchronized fun advance(millis: Long) { require(millis >= 0); base += millis }
}
