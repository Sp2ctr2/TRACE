package app.saeon.trace

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AllScreensTest : UiHarness() {
    private fun shot(route: String, name: String, delayMs: Long = 420) {
        navigate(route)
        Thread.sleep(delayMs)
        compose.waitForIdle()
        capture(name, audit = false)
    }

    @Test fun captureEveryPrimaryDestination() {
        fresh()
        shot("home", "01_home")
        shot("assets", "02_assets")
        shot("account", "03_account")
        shot("savings", "04_savings")
        shot("card", "05_card")
        shot("loan", "06_loan")
        shot("bring", "07_bring")
        shot("history", "08_history")
        shot("receipt/SIM-OPEN-03", "09_receipt")
        shot("transfer", "10_transfer_recipient")
        shot("recipient_entry", "11_recipient_entry")

        runBlocking {
            repository.setDraft(TransferDraft(Fixtures.seoyeon, 32_000, Purpose.SETTLEMENT))
        }
        shot("amount", "12_transfer_amount")

        shot("safety", "13_safety")
        shot("pending", "14_pending")
        shot("timeline", "15_timeline")
        shot("safety_guide", "16_safety_guide")
        shot("privacy", "17_privacy")
        shot("manual", "18_manual_check")
        shot("more", "19_more")
        shot("profile", "20_profile")
        shot("security", "21_security")
        shot("transfer_settings", "22_transfer_settings")
        shot("favorites", "23_favorites")
        shot("recurring", "24_recurring")
        shot("notifications", "25_notifications")
        shot("appearance", "26_appearance")
        shot("accessibility", "27_accessibility")
        shot("support", "28_support")
        shot("help", "29_help")
        shot("app_info", "30_app_info")

        compose.runOnIdle { compose.activity.model.stageUnlock() }
        shot("presenter", "31_demo_center")
        tap("demo_more", scroll = true)
        Thread.sleep(250)
        capture("32_demo_center_all", audit = false)
        shot("comparison", "33_same_amount_comparison")
        shot("demo_lab", "34_demo_lab_full")
    }

    @Test fun captureTransferStatesAndSheets() {
        fresh()
        createReview(DemoScenario.NORMAL)
        shot("transfer_state", "35_transfer_review")
        tap("transfer_confirm")
        Thread.sleep(350)
        capture("36_authentication_sheet", audit = false)
        tap("auth_confirm")
        Thread.sleep(430)
        capture("37_trace_orbit_loading", audit = false)
        waitScreen("transfer_complete")
        capture("38_transfer_complete", audit = false)
        tap("complete_receipt")
        waitScreen("receipt_expanded")
        capture("39_receipt_expanded", audit = false)
        tap("receipt_collapse")

        evaluated(DemoScenario.IMPERSONATION)
        capture("40_hold", audit = false)
        tap("hold_reasons_open", scroll = true)
        waitScreen("trace_hold_reason")
        capture("41_hold_reasons", audit = false)
        tap("hold_reasons_close")
        shot("timeline", "42_hold_timeline")
        shot("safety_guide", "43_hold_safety_guide")

        evaluated(DemoScenario.WARN)
        capture("44_warn_sheet", audit = false)
        tap("warn_check", scroll = true)
        capture("45_warn_acknowledged", audit = false)

        evaluated(DemoScenario.LOAN)
        capture("46_verify", audit = false)
        tap("verify_route")
        waitScreen("trace_official_route")
        capture("47_official_route", audit = false)

        evaluated(DemoScenario.UNKNOWN)
        capture("48_unknown_before_lookup", audit = false)
        tap("verify_route")
        waitScreen("trace_unknown")
        capture("49_unknown", audit = false)
    }

    @Test fun captureDemoScenarioEntryScreens() {
        fresh()
        compose.runOnIdle { compose.activity.model.stageUnlock() }
        val cases = listOf(
            DemoScenario.NORMAL,
            DemoScenario.IMPERSONATION,
            DemoScenario.LOAN,
            DemoScenario.WARN,
            DemoScenario.NEW_ACCOUNT,
            DemoScenario.FAMILY_FAKE,
            DemoScenario.REMOTE,
            DemoScenario.INVESTMENT,
            DemoScenario.UNKNOWN,
            DemoScenario.EDUCATION,
            DemoScenario.UNRELATED
        )
        cases.forEachIndexed { index, scenario ->
            compose.runOnIdle { compose.activity.model.stageScenario(scenario) {} }
            Thread.sleep(350)
            navigate("transfer_state")
            Thread.sleep(350)
            capture("50_scenario_${index.toString().padStart(2,'0')}_${scenario.name.lowercase()}", audit = false)
        }
    }

    @Test fun captureDarkAndCompactPresentationStates() {
        fresh()
        runBlocking {
            graph.preferences.theme("dark")
            graph.preferences.transparency(false)
        }
        compose.waitUntil(10_000) { compose.activity.model.preferences.value.themeMode == "dark" }
        shot("home", "70_dark_home")
        createReview(DemoScenario.NORMAL)
        shot("transfer_state", "71_dark_review")
        evaluated(DemoScenario.IMPERSONATION)
        capture("72_dark_hold", audit = false)

        runBlocking {
            graph.preferences.theme("light")
            graph.preferences.easy(true)
            graph.preferences.motion(true)
        }
        compose.waitUntil(10_000) { compose.activity.model.preferences.value.easyMode }
        evaluated(DemoScenario.IMPERSONATION)
        capture("73_easy_hold", audit = false)
    }
}
