package app.saeon.trace

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
    val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val context get() = instrumentation.targetContext
    val graph get() = (context.applicationContext as SaeonApplication).graph
    val repository get() = graph.repository
    val device get() = UiDevice.getInstance(instrumentation)
    val state get() = checkNotNull(repository.state.value)
    private val pass get() = InstrumentationRegistry.getArguments().getString("pass") ?: "local"
    val output: File get() = File(context.getExternalFilesDir(null), "verification/$pass").apply { mkdirs() }
    @get:Rule val captureFailure = object : TestWatcher() {
        override fun failed(error: Throwable?, description: Description?) {
            runCatching { capture("failure_${description?.methodName ?: "unknown"}", audit = false) }
        }
    }
    fun awaitReady() {
        compose.waitUntil(15_000) { repository.state.value != null && compose.activity.navigation != null }
        compose.waitForIdle()
        DeviceWindowGuard.clearKnownLauncherDialog(output)
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
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
    fun tap(tag: String, scroll: Boolean = false) {
        DeviceWindowGuard.clearKnownLauncherDialog(output)
        waitScreen(tag)
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
        DeviceWindowGuard.clearKnownLauncherDialog(output)
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
        if (audit) {
            DeviceWindowGuard.clearKnownLauncherDialog(output)
            DeviceWindowGuard.assertNoCrashDialog()
        }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Emulator screenshot unavailable" }
        File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val nodes = compose.onAllNodes(SemanticsMatcher("all nodes") { true }, useUnmergedTree = true).fetchSemanticsNodes()
        File(output, "$name.semantics.txt").writeText(nodes.joinToString("\n\n") { "${it.id} clipped=${it.boundsInRoot} layout=${it.size}\n${it.config}" })
        if (audit) auditLayout(nodes, name)
    }
    private fun auditLayout(nodes: List<SemanticsNode>, name: String) {
        val minTarget = 48f * context.resources.displayMetrics.density - 1.5f
        val tolerancePx = 1.5f
        val issues = mutableListOf<String>()
        val geometry = mutableListOf<String>()
        nodes.forEach { node ->
            val visible = node.boundsInRoot.width > 0 && node.boundsInRoot.height > 0
            if (visible && node.config.contains(SemanticsActions.OnClick)) {
                val actual = node.touchBoundsInRoot
                val logical = node.size
                if ((logical.width < minTarget && actual.width < minTarget) || (logical.height < minTarget && actual.height < minTarget))
                    issues += "Touch target ${node.id}: layout=$logical touch=$actual\n${node.config}"
            }
            if (visible) {
                val results = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
                results.forEach { layout ->
                    val lineBounds = (0 until layout.lineCount).joinToString(" | ") { line ->
                        listOf(layout.getLineLeft(line), layout.getLineRight(line), layout.getLineTop(line), layout.getLineBottom(line)).joinToString(",")
                    }
                    val renderedWidth = (0 until layout.lineCount).maxOfOrNull { line ->
                        layout.getLineRight(line) - layout.getLineLeft(line)
                    } ?: 0f
                    val editable = node.config.contains(SemanticsActions.SetText)
                    // Compose's simple-text semantics can reconstruct a paragraph
                    // using the parent's maximum width, while the actual Text node
                    // is intrinsic-sized. Unused paragraph capacity is NOT clipping.
                    // Compare rendered line spans with the logical node instead.
                    val widthOverflow = renderedWidth > node.size.width + tolerancePx
                    val heightOverflow = layout.multiParagraph.height > node.size.height + tolerancePx
                    val truncated = layout.multiParagraph.didExceedMaxLines ||
                        (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
                    geometry += "node=${node.id}; logical=${node.size}; result=${layout.size}; paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height}; renderedWidth=$renderedWidth; lines=${layout.lineCount}; rawOverflow=${layout.hasVisualOverflow}; widthOverflow=$widthOverflow; heightOverflow=$heightOverflow; truncated=$truncated; editable=$editable; lineBounds=$lineBounds; text=${layout.layoutInput.text.text.take(80)}"
                    // Editable fields intentionally scroll. Their value and IME
                    // reachability are exercised in separate interaction tests.
                    if (!editable && (widthOverflow || heightOverflow || truncated))
                        issues += "Text clipping ${node.id}: logical=${node.size}; renderedWidth=$renderedWidth; height=${layout.multiParagraph.height}; truncated=$truncated; text=${layout.layoutInput.text}"
                }
            }
        }
        File(output, "$name.text-geometry.txt").writeText(geometry.joinToString("\n"))
        File(output, "$name.audit.txt").writeText(if (issues.isEmpty()) "PASS: rendered non-editable text and logical 48dp interactive bounds\n" else issues.joinToString("\n"))
        assertTrue("$name layout audit:\n${issues.joinToString("\n")}", issues.isEmpty())
    }
}
