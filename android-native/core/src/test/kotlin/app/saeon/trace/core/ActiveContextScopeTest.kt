package app.saeon.trace.core

import org.junit.Assert.*
import org.junit.Test

class ActiveContextScopeTest {
    private val now = Fixtures.epoch
    private fun evaluate(state: BankState, at: Long = now): BankState {
        val id = state.currentTransferId!!
        val (prepared, challenge) = BankEngine.prepare(state, id, at)
        val authorized = BankEngine.authorize(prepared, challenge, AuthMethod.DEMO_CONFIRMATION, at)
        val packet = BankEngine.attest(authorized, id, at, "test-only")
        return BankEngine.finish(authorized, id, packet, at, signatureValid = true)
    }
    @Test fun recipientAndPurposeChangesCannotEraseActivePressure() {
        for (scenario in listOf(DemoScenario.IMPERSONATION, DemoScenario.EASY, DemoScenario.LOAN, DemoScenario.UNKNOWN)) {
            for (recipient in Fixtures.recipients) for (purpose in Purpose.entries) {
                val initial = Fixtures.initial(scenario, now)
                val reviewed = BankEngine.review(initial, TransferDraft(recipient, 32_000, purpose), now)
                val result = evaluate(reviewed)
                val expected = if (scenario in listOf(DemoScenario.IMPERSONATION, DemoScenario.EASY)) TransferStage.HOLD else TransferStage.VERIFY
                assertEquals("$scenario / ${recipient.id} / $purpose", expected, result.current!!.stage)
                assertEquals(Fixtures.START_BALANCE, result.balance)
                assertTrue(result.receipts.none { !it.seed })
            }
        }
    }
    @Test fun cancellingAndStartingANewPayeeDoesNotClearPressure() {
        val initial = Fixtures.initial(DemoScenario.IMPERSONATION, now)
        val held = evaluate(BankEngine.review(initial, TransferDraft(Fixtures.kim, 3_000_000), now))
        val cancelled = BankEngine.cancel(held, held.currentTransferId!!)
        val next = evaluate(BankEngine.review(cancelled, TransferDraft(Fixtures.family, 32_000, Purpose.FAMILY), now))
        assertEquals(TransferStage.HOLD, next.current!!.stage)
        assertEquals(Fixtures.START_BALANCE, next.balance)
        assertTrue(next.receipts.none { !it.seed })
    }
    @Test fun officialRouteKeepsSessionSignalsButRequiresNewAuthorization() {
        val initial = Fixtures.initial(DemoScenario.LOAN, now)
        val verified = evaluate(BankEngine.review(initial, TransferDraft(Fixtures.family, 32_000, Purpose.GENERAL), now))
        val oldId = verified.currentTransferId!!
        val routed = BankEngine.resolveRoute(verified, oldId, now, true)
        val newReview = BankEngine.useOfficialRoute(routed, oldId, now)
        assertNotEquals(oldId, newReview.currentTransferId)
        assertNull(newReview.current!!.authorization)
        assertEquals(initial.events, newReview.events)
        assertEquals(Purpose.LOAN, newReview.current!!.intent.purpose)
        assertEquals(Fixtures.START_BALANCE, newReview.balance)
        val completed = evaluate(newReview)
        assertEquals(TransferStage.COMPLETE, completed.current!!.stage)
        assertEquals(12_808_000L, completed.balance)
        assertEquals(7_968_000L, completed.loanBalance)
    }
    @Test fun expiryChangesNewEvaluationsButDoesNotReleaseHeldIntent() {
        val initial = Fixtures.initial(DemoScenario.IMPERSONATION, now)
        val held = evaluate(BankEngine.review(initial, TransferDraft(Fixtures.kim, 3_000_000), now))
        val heldId = held.currentTransferId!!
        val later = now + Fixtures.EVENT_TTL + 1
        val next = evaluate(BankEngine.review(held, TransferDraft(Fixtures.family, 32_000), later), later)
        assertEquals(TransferStage.HOLD, next.record(heldId).stage)
        assertEquals(TransferStage.COMPLETE, next.current!!.stage)
        assertEquals(1, next.receipts.count { !it.seed })
    }
    @Test fun noPressurePreservesTheCanonicalNormalFlow() {
        val initial = Fixtures.initial()
        val completed = evaluate(BankEngine.review(initial, TransferDraft(Fixtures.seoyeon, 32_000), now))
        assertEquals(12_808_000L, completed.balance)
        assertEquals(TransferStage.COMPLETE, completed.current!!.stage)
        assertEquals(1, completed.receipts.count { !it.seed })
    }
}
