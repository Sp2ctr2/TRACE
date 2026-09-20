package app.saeon.trace

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputBoundaryTest : UiHarness() {
    private fun imeVisible(): Boolean = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
        ?.isVisible(WindowInsetsCompat.Type.ime()) == true

    @Test fun keyboardDoesNotCoverConsentAndSystemBackClearsUnsubmittedText() {
        fresh(); navigate("manual")
        tap("shared_text_input", scroll = true)
        compose.onNodeWithTag("shared_text_input").performTextInput("검찰입니다. 지금 이체하세요.")
        compose.waitUntil(10_000) { imeVisible() }
        val consent = compose.onNodeWithTag("share_consent")
        var ancestor = consent.fetchSemanticsNode().parent
        var canScroll = false
        while (ancestor != null) {
            if (ancestor.config.contains(SemanticsActions.ScrollBy)) canScroll = true
            ancestor = ancestor.parent
        }
        if (canScroll) consent.performScrollTo()
        consent.assertIsDisplayed().assertIsEnabled()
        assertTrue("No analysis before explicit consent", state.events.isEmpty())
        capture("manual_input_ime")
        device.pressBack()
        compose.waitUntil(10_000) { !imeVisible() }
        device.pressBack()
        waitScreen("home")
        assertEquals("", compose.activity.safetyModel.state.value.text)
        assertNull(compose.activity.safetyModel.state.value.result)
        assertTrue(state.events.isEmpty())
        assertTrue(state.receipts.none { !it.seed })
    }
}
