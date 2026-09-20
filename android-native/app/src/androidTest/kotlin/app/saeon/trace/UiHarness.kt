package app.saeon.trace

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntSize
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

abstract class UiHarness {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val layoutFailures = mutableListOf<String>()
    @After fun assertCapturedLayouts() {
        assertTrue("Captured layout defects:\n${layoutFailures.joinToString("\n")}", layoutFailures.isEmpty())
    }
    val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val context get() = instrumentation.targetContext
    val graph get() = (context.applicationContext as SaeonApplication).graph
    val repository get() = graph.repository
    val device get() = UiDevice.getInstance(instrumentation)
    val state get() = checkNotNull(repository.state.value)
    fun recoverKnownEmulatorSystemDialog() {
        val launcherAnr = device.findObject(By.text("Pixel Launcher isn't responding"))
            ?: device.findObject(By.textContains("Pixel Launcher isn't responding"))
        if (launcherAnr != null) {
            val close = device.findObject(By.res("android:id/aerr_close"))
                ?: throw AssertionError("Pixel Launcher ANR is visible but its close control cannot be identified")
            close.click()
            device.waitForIdle()
        }
    }
    fun pressPhysicalBack() {
        recoverKnownEmulatorSystemDialog()
        device.pressBack()
        device.waitForIdle()
    }
    private val pass get() = InstrumentationRegistry.getArguments().getString("pass") ?: "local"
    val output: File get() = File(context.getExternalFilesDir(null), "verification/$pass").apply { mkdirs() }
    @get:Rule val captureFailure = object : TestWatcher() {
        override fun failed(error: Throwable?, description: Description?) {
            runCatching { capture("failure_${description?.methodName ?: "unknown"}", audit = false) }
        }
    }
    fun awaitReady() {
        recoverKnownEmulatorSystemDialog()
        compose.waitUntil(15_000) { repository.state.value != null && compose.activity.navigation != null }
        compose.waitForIdle()
        recoverKnownEmulatorSystemDialog()
    }
    fun fresh(scenario: DemoScenario = DemoScenario.NORMAL) {
        awaitReady()
        runBlocking {
            repository.reset(scenario)
            graph.preferences.reset()
            graph.preferences.easy(scenario == DemoScenario.EASY)
        }
        compose.runOnIdle { compose.activity.model.dismissError(); compose.activity.safetyModel.clear() }
        navigate("home", clear = true)
        compose.waitUntil(10_000) { compose.activity.model.preferences.value.easyMode == (scenario == DemoScenario.EASY) }
        compose.waitForIdle()
    }
    fun navigate(route: String, clear: Boolean = false) {
        compose.runOnIdle {
            compose.activity.navigation!!.navigate(route) {
                if (clear) popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
        }
        compose.waitForIdle()
    }
    fun waitScreen(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.waitForIdle()
    }
    fun tap(tag: String, scroll: Boolean = false) {
        waitScreen(tag)
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }
    fun createReview(scenario: DemoScenario) {
        runBlocking {
            repository.setDraft(TransferDraft(Fixtures.recipient(scenario), Fixtures.amount(scenario), Fixtures.purpose(scenario)))
            repository.reviewDraft()
        }
        navigate("transfer_state")
        waitScreen("transfer_review")
    }
    fun authorizeOnly() {
        runBlocking {
            val challenge = repository.prepare(state.currentTransferId!!)
            repository.authorize(challenge, AuthMethod.DEMO_CONFIRMATION)
        }
        waitScreen("trace_evaluating")
    }
    fun finish() { runBlocking { repository.finish(state.currentTransferId!!) }; compose.waitForIdle() }
    fun evaluated(scenario: DemoScenario) {
        fresh(scenario); createReview(scenario); authorizeOnly(); finish()
    }
    fun confirmThroughUi(expectedTag: String) {
        tap("transfer_confirm")
        waitScreen("auth_confirm")
        tap("auth_confirm")
        waitScreen(expectedTag)
    }
    fun share(text: String) {
        compose.runOnIdle {
            compose.activity.startActivity(Intent(compose.activity, MainActivity::class.java)
                .setAction(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        waitScreen("shared_text_review")
    }
    fun capture(name: String, audit: Boolean = true) {
        compose.waitForIdle()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Emulator screenshot unavailable" }
        File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val nodes = compose.onAllNodes(SemanticsMatcher("all nodes") { true }, useUnmergedTree = true).fetchSemanticsNodes()
        File(output, "$name.semantics.txt").writeText(nodes.joinToString("\n\n") { "${it.id} clipped=${it.boundsInRoot} layout=${it.size}\n${it.config}" })
        if (audit) auditLayout(nodes, name)
    }
    private fun auditLayout(nodes: List<SemanticsNode>, name: String) {
        val minTarget = 48f * context.resources.displayMetrics.density - 1.5f
        val issues = mutableListOf<String>()
        val metrics = mutableListOf<String>()
        nodes.forEach { node ->
            val visible = node.boundsInRoot.width > 0 && node.boundsInRoot.height > 0
            if (visible && node.config.contains(SemanticsActions.OnClick)) {
                val actual = node.touchBoundsInRoot
                val logical = node.size
                if ((logical.width < minTarget && actual.width < minTarget) || (logical.height < minTarget && actual.height < minTarget))
                    issues += "Touch target ${node.id}: layout=$logical touch=$actual\n${node.config}"
            }
            if (visible) {
                val result = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(result)
                result.forEach { text ->
                    val metric = textOverflow(text, node.size)
                    if (text.hasVisualOverflow || metric.omitted) metrics +=
                        "node=${node.id} layout=${text.size} reconstructedParagraph=${text.multiParagraph.width}x${text.multiParagraph.height} occupiedWidth=${metric.occupiedWidth} dx=${metric.dx} dy=${metric.dy} omitted=${metric.omitted} text=${text.layoutInput.text}"
                    if (metric.exceedsBounds) issues +=
                        "Text overflow ${node.id}: dx=${metric.dx} dy=${metric.dy} omitted=${metric.omitted}: ${text.layoutInput.text}"
                }
            }
        }
        File(output, "$name.text-metrics.txt").writeText(metrics.joinToString("\n") + "\n")
        File(output, "$name.audit.txt").writeText(if (issues.isEmpty()) "PASS: visible occupied text lines (one physical pixel rounding tolerance; no omitted lines) and 48dp interactive bounds\n" else issues.joinToString("\n"))
        if (issues.isNotEmpty()) layoutFailures += "$name layout audit:\n${issues.joinToString("\n")}"
    }
}

/** Simple Text semantics reconstructs MultiParagraph at the parent's max width,
 * but preserves the tight measured layout size. Paragraph.width includes blank
 * space and is therefore not a glyph-overflow test. Compare occupied line widths
 * and height; omitted lines and ellipsis always fail. Contract-tested with real
 * narrow/clipped Android Text nodes in TextBoundsAuditTest.
 */
internal data class TextOverflowMetric(val occupiedWidth: Float, val dx: Float, val dy: Float, val omitted: Boolean) {
    val exceedsBounds: Boolean get() = dx > 1f || dy > 1f || omitted
}
internal fun textOverflow(text: TextLayoutResult, renderedSize: IntSize = text.size): TextOverflowMetric {
    val occupied = (0 until text.lineCount).maxOfOrNull { text.getLineRight(it) - text.getLineLeft(it) } ?: 0f
    val bottom = (0 until text.lineCount).maxOfOrNull { text.getLineBottom(it) } ?: 0f
    val explicitLines = text.layoutInput.text.text.count { it == '\n' } + 1
    val hardBreakTruncated = text.layoutInput.maxLines >= explicitLines && text.lineCount < explicitLines
    val omitted = text.multiParagraph.didExceedMaxLines ||
        text.didOverflowHeight ||
        (0 until text.lineCount).any { text.isLineEllipsized(it) } ||
        hardBreakTruncated
    val effectiveHeight = minOf(text.size.height, renderedSize.height)
    return TextOverflowMetric(occupied, occupied - text.size.width, maxOf(bottom, text.multiParagraph.height) - effectiveHeight, omitted)
}
