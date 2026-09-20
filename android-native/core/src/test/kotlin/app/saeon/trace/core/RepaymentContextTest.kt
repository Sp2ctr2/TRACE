package app.saeon.trace.core

import org.junit.Assert.*
import org.junit.Test

class RepaymentContextTest {
    private val now = Fixtures.epoch
    private fun evaluate(state: BankState): BankState {
        val id = state.currentTransferId!!
        val (pending, challenge) = BankEngine.prepare(state, id, now)
        val authorized = BankEngine.authorize(pending, challenge, AuthMethod.DEMO_CONFIRMATION, now)
        return BankEngine.finish(authorized, id, BankEngine.attest(authorized, id, now, "test-key"), now, true)
    }
    @Test fun changingPurposeCannotEraseObservedRepaymentRequest() {
        Purpose.entries.forEach { purpose ->
            val initial = Fixtures.initial(DemoScenario.LOAN, now)
            val reviewed = BankEngine.review(initial, TransferDraft(Fixtures.park, 8_000_000, purpose), now)
            val result = evaluate(reviewed)
            assertEquals("Purpose: $purpose", TransferStage.VERIFY, result.current!!.stage)
            assertEquals(Fixtures.START_BALANCE, result.balance)
            assertTrue(result.receipts.none { !it.seed })
        }
    }
    @Test fun inferredRepaymentCanUseOfficialRouteOnlyAsNewLoanIntent() {
        val initial = Fixtures.initial(DemoScenario.LOAN, now)
        val verified = evaluate(BankEngine.review(initial, TransferDraft(Fixtures.park, 8_000_000, Purpose.OTHER), now))
        val oldId = verified.currentTransferId!!
        val routed = BankEngine.resolveRoute(verified, oldId, now, true)
        val renewed = BankEngine.useOfficialRoute(routed, oldId, now)
        assertNotEquals(oldId, renewed.currentTransferId)
        assertEquals(Purpose.LOAN, renewed.current!!.intent.purpose)
        assertNull(renewed.current!!.authorization)
        assertEquals(Fixtures.START_BALANCE, renewed.balance)
        assertEquals(4_840_000L, evaluate(renewed).balance)
    }
    @Test fun inferredRepaymentRouteFailureStillHasNoFallback() {
        val initial = Fixtures.initial(DemoScenario.UNKNOWN, now)
        val verified = evaluate(BankEngine.review(initial, TransferDraft(Fixtures.park, 8_000_000, Purpose.GENERAL), now))
        val result = BankEngine.resolveRoute(verified, verified.currentTransferId!!, now, false)
        assertEquals(TransferStage.UNKNOWN, result.current!!.stage)
        assertTrue(result.receipts.none { !it.seed })
        assertEquals(Fixtures.START_BALANCE, result.balance)
    }
    @Test fun manualLoanTextCreatesStructuredRepaymentEvidenceWithoutRawExport() {
        val raw = "저금리로 바꾸려면 이 계좌로 기존 대출부터 갚아야 합니다. SECRET_8491"
        val signals = SignalExtractor.extract(raw, now, RiskSource.SHARED_TEXT)
        assertTrue(signals.any { it.type == RiskType.LOAN_REPAYMENT_REQUEST })
        assertTrue(signals.none { "SECRET_8491" in it.summary })
        val initial = Fixtures.initial().copy(events = signals)
        val result = evaluate(BankEngine.review(initial, TransferDraft(Fixtures.park, 8_000_000, Purpose.GENERAL), now))
        assertEquals(TransferStage.VERIFY, result.current!!.stage)
    }
    @Test fun trustedRouteDoesNotEraseUnrelatedImpersonationEvidence() {
        val initial = Fixtures.initial(DemoScenario.LOAN, now)
        val verified = evaluate(BankEngine.review(initial, TransferDraft(Fixtures.park, 8_000_000, Purpose.LOAN), now))
        val routed = BankEngine.resolveRoute(verified, verified.currentTransferId!!, now, true)
        val renewed = BankEngine.useOfficialRoute(routed, routed.currentTransferId!!, now)
        val extra = SignalExtractor.extract("경찰입니다. 지금 바로 이체하세요.", now, RiskSource.SHARED_TEXT)
        val result = evaluate(renewed.copy(events = renewed.events + extra))
        assertEquals(TransferStage.HOLD, result.current!!.stage)
        assertEquals(Fixtures.START_BALANCE, result.balance)
    }
    @Test fun expiredRepaymentRequestDoesNotPermanentlyFlagGeneralTransfers() {
        val initial = Fixtures.initial(DemoScenario.LOAN, now)
        val record = BankEngine.review(initial, TransferDraft(Fixtures.park, 1000, Purpose.GENERAL), now).current!!
        val expired = now + Fixtures.EVENT_TTL + 1
        assertEquals(PolicyDecision.ALLOW, BankPolicy.evaluate(record, initial.context, expired).decision)
    }
    @Test fun canonicalNormalRemainsUnchanged() {
        val initial = Fixtures.initial()
        val result = evaluate(BankEngine.review(initial, TransferDraft(Fixtures.seoyeon, 32_000), now))
        assertEquals(TransferStage.COMPLETE, result.current!!.stage)
        assertEquals(12_808_000L, result.balance)
    }
}
