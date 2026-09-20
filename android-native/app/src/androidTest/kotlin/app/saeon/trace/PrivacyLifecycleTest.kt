package app.saeon.trace

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacyLifecycleTest : UiHarness() {
    @Test fun systemBackClearsUnconfirmedText() {
        fresh(); share("검찰이라고 주장하며 지금 송금하라는 요청_BACK_PRIVATE")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitScreen("home")
        assertEquals("", compose.activity.safetyModel.state.value.text)
        assertTrue(state.events.isEmpty())
        val row = runBlocking { graph.database.snapshots().read()!! }
        assertFalse(row.payload.contains("BACK_PRIVATE"))
    }
    @Test fun cancelledAnalysisCannotOverwriteTheNextShare() {
        fresh(); navigate("manual")
        compose.runOnIdle {
            val model = compose.activity.safetyModel
            model.receive("경찰입니다. 지금 http://old.invalid 에서 확인하고 입금하세요.")
            model.analyze()
            model.clear()
            model.receive("어제 저녁값 정산할게요. NEW_PRIVATE")
        }
        waitScreen("shared_text_review")
        compose.waitForIdle()
        assertEquals("어제 저녁값 정산할게요. NEW_PRIVATE", compose.activity.safetyModel.state.value.text)
        assertNull(compose.activity.safetyModel.state.value.result)
        assertTrue(state.events.isEmpty())
        assertFalse(compose.activity.safetyModel.state.value.busy)
    }
    @Test fun navigationAwayClearsInputWithoutCreatingSignals() {
        fresh(); share("외부 원문_NAV_PRIVATE")
        navigate("safety")
        waitScreen("safety_center")
        assertEquals("", compose.activity.safetyModel.state.value.text)
        assertTrue(state.events.isEmpty())
    }
}
