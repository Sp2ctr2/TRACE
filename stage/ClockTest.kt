package app.saeon.trace
import app.saeon.trace.ui.RehearsalClock
import org.junit.Test
import org.junit.Assert.*
class ClockTest {
 @Test fun startsAtThreeMinutes(){assertEquals(180,RehearsalClock().remaining(5000))}
 @Test fun usesMonotonicElapsed(){assertEquals(175,RehearsalClock().start(1000).remaining(6000))}
 @Test fun pauseDoesNotDrift(){val c=RehearsalClock().start(1000).pause(6000);assertEquals(175,c.remaining(100000))}
 @Test fun resumeAccumulates(){val c=RehearsalClock().start(1000).pause(6000).start(20000);assertEquals(170,c.remaining(25000))}
 @Test fun repeatedStartIsIdempotent(){val c=RehearsalClock().start(1000).start(5000);assertEquals(175,c.remaining(6000))}
 @Test fun demoKeepsOriginalScriptOffset(){val c=RehearsalClock(55,75).start(1000);assertEquals(91,c.scriptSecond(17000));assertEquals(39,c.remaining(17000))}
 @Test fun overrunIsNotAForcedSceneChange(){val c=RehearsalClock().start(1000);assertEquals(-5,c.remaining(186000));assertEquals(185,c.scriptSecond(186000))}
 @Test fun backwardClockCannotSubtractElapsed(){assertEquals(0L,RehearsalClock().start(5000).elapsed(1000))}
}
