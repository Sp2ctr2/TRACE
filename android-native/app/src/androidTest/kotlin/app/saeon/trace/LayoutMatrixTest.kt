package app.saeon.trace

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.DemoScenario
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutMatrixTest : UiHarness() {
    @Test fun homeReviewAndHoldRespectCurrentConfiguration() {
        fresh(); capture("matrix_Home")
        createReview(DemoScenario.NORMAL); capture("matrix_Review")
        evaluated(DemoScenario.IMPERSONATION); capture("matrix_HOLD")
        evaluated(DemoScenario.EASY); capture("matrix_Easy_HOLD")
    }
}
