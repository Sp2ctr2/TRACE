package app.saeon.trace

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

abstract class UiHarness {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val graph: AppGraph get() = (compose.activity.application as SaeonApplication).graph
    val repository get() = graph.repository
    val state get() = repository.state.value!!
    private val args get() = InstrumentationRegistry.getArguments()
    private val suffix get() = args.getString("capturePrefix", "run")
    private val dir: File get() = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "verification/$suffix").apply { mkdirs() }

    @get:Rule val captureFailure = object : TestWatcher() {
        override fun failed(e: Throwable?, description: Description?) {
            runCatching { capture("failure_${description?.methodName}", audit = false) }
        }
    }
    fun awaitReady() {
        compose.waitUntil(15_000) { compose.activity.model.bank.value != null && compose.activity.navigation != null }
        compose.waitForIdle()
    }
    fun fresh(scenario: DemoScenario = DemoScenario.NORMAL) {
        awaitReady()
        runBlocking { repository.reset(scenario); graph.preferences.reset(); graph.preferences.easy(scenario == DemoScenario.EASY) }
        compose.runOnIdle { compose.activity.safetyModel.clear() }
        navigate("home", clear = true)
        compose.waitUntil(10_000) { state.scenario == scenario && compose.activity.model.preferences.value.easyMode == (scenario == DemoScenario.EASY) }
        compose.waitForIdle()
    }
    fun navigate(route: String, clear: Boolean = false) {
        compose.runOnIdle {
            compose.activity.navigation!!.navigate(route) {
                if (clear) popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }
        compose.waitForIdle()
    }
    fun waitScreen(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
    fun tap(tag: String, scroll: Boolean = false) {
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }
    fun createReview(scenario: DemoScenario, amount: Long = Fixtures.amount(scenario)) {
        runBlocking {
            repository.setDraft(TransferDraft(Fixtures.recipient(scenario), amount, Fixtures.purpose(scenario)))
            repository.reviewDraft()
        }
        navigate("transfer_state")
        waitScreen("transfer_review")
    }
    fun authorizeOnly() {
        val id = state.currentTransferId!!
        runBlocking {
            val challenge = repository.prepare(id)
            repository.authorize(challenge, AuthMethod.DEMO_CONFIRMATION)
        }
        waitScreen("trace_evaluating")
    }
    fun finish() {
        runBlocking { repository.finish(state.currentTransferId!!) }
        compose.waitForIdle()
    }
    fun evaluated(scenario: DemoScenario) {
        fresh(scenario); createReview(scenario); authorizeOnly(); finish()
    }
    fun confirmThroughUi(expected: String) {
        tap("transfer_confirm"); waitScreen("auth_confirm"); tap("auth_confirm"); waitScreen(expected)
    }
    fun share(text: String) {
        compose.runOnIdle {
            val activity = compose.activity
            activity.onNewIntent(Intent(activity, MainActivity::class.java).setAction(Intent.ACTION_SEND)
                .setType("text/plain").putExtra(Intent.EXTRA_TEXT, text))
        }
        waitScreen("shared_text_review")
    }
    fun capture(name: String, audit: Boolean = true) {
        compose.waitForIdle()
        device.waitForIdle(1_000)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        requireNotNull(bitmap) { "Emulator screenshot unavailable" }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        File(dir, "$name.semantics.txt").writeText(compose.onAllNodes(isRoot(), useUnmergedTree = true).printToString())
        if (audit) auditLayout(name)
    }
    fun auditLayout(name: String) {
        val failures = mutableListOf<String>()
        val geometry = mutableListOf<String>()
        val density = compose.activity.resources.displayMetrics.density
        val height = compose.activity.resources.displayMetrics.heightPixels
        val width = compose.activity.resources.displayMetrics.widthPixels
        val nodes = compose.onAllNodes(SemanticsMatcher("all") { true }, useUnmergedTree = true).fetchSemanticsNodes()
        nodes.forEach { node ->
            val bounds = node.boundsInRoot
            if (bounds.width <= 0 || bounds.height <= 0 || bounds.bottom <= 0 || bounds.top >= height || bounds.right <= 0 || bounds.left >= width) return@forEach
            if (node.config.getOrNull(SemanticsActions.OnClick) != null) {
                val touch = node.touchBoundsInRoot
                if (touch.width + 1f < 48f * density || touch.height + 1f < 48f * density)
                    failures += "Touch target ${node.id}: ${touch.width / density}x${touch.height / density}dp"
            }
            val layouts = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            layouts.forEach { layout ->
                geometry += "node=${node.id}; size=${layout.size}; paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height}; lines=${layout.lineCount}; maxLines=${layout.multiParagraph.didExceedMaxLines}; horizontal=${layout.didOverflowWidth}; vertical=${layout.didOverflowHeight}; text=${layout.layoutInput.text.text.take(80)}"
                if (layout.hasVisualOverflow) failures += "Text overflow ${node.id}: ${layout.layoutInput.text.text.take(80)}"
            }
        }
        File(dir, "$name.text-geometry.txt").writeText(geometry.joinToString("\n"))
        File(dir, "$name.audit.txt").writeText(if (failures.isEmpty()) "PASS: visible text and minimum touch targets" else failures.joinToString("\n"))
        assertTrue("$name layout audit:\n${failures.joinToString("\n")}", failures.isEmpty())
    }
}
