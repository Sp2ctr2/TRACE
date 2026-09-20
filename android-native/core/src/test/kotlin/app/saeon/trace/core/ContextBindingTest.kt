package app.saeon.trace.core

import org.junit.Assert.*
import org.junit.Test

/** Regression tests for editing a transaction after a risky request. */
class ContextBindingTest {
    private val now = Fixtures.epoch
    private fun review(scenario: DemoScenario, purpose: Purpose = Purpose.GENERAL, recipient: Recipient = Fixtures.recipient(scenario)): BankState =
        BankEngine.review(Fixtures.initial(scenario, now), TransferDraft(recipient, Fixtures.amount(scenario), purpose), now)
    private fun evaluate(state: BankState): BankState {
        val id = state.currentTransferId!!
        val (prepared, challenge) = BankEngine.prepare(state, id, now)
        val authorized = BankEngine.authorize(prepared, challenge, AuthMethod.DEMO_CONFIRMATION, now)
        return BankEngine.finish(authorized, id, BankEngine.attest(authorized, id, now, "test"), now, true)
    }
    private fun assertNotSent(state: BankState) {
        assertEquals(Fixtures.START_BALANCE, state.balance)
        assertTrue(state.receipts.none { !it.seed })
    }
    @Test fun loanRequestCannotBeRelabelledAsLivingExpenses() {
        val result = evaluate(review(DemoScenario.LOAN, Purpose.LIVING))
        assertEquals(TransferStage.VERIFY, result.current!!.stage)
        assertTrue(RiskType.LOAN_REPAYMENT_REQUEST in result.current!!.reasons)
        assertNotSent(result)
    }
    @Test fun changingPayeeCannotDiscardAnActiveLoanRequest() {
        val result = evaluate(review(DemoScenario.LOAN, Purpose.FAMILY, Fixtures.family))
        assertEquals(TransferStage.VERIFY, result.current!!.stage)
        assertNotSent(result)
    }
    @Test fun changingAccountCannotDiscardAnActiveImpersonationRequest() {
        val changed = Fixtures.family.copy(id = "edited", account = "110-***-2019")
        val result = evaluate(review(DemoScenario.IMPERSONATION, Purpose.OTHER, changed))
        assertEquals(TransferStage.HOLD, result.current!!.stage)
        assertNotSent(result)
    }
    @Test fun cancelAndStartAgainDoesNotEraseRequestEvidence() {
        val blocked = evaluate(review(DemoScenario.UNKNOWN))
        val unknown = BankEngine.resolveRoute(blocked, blocked.currentTransferId!!, now, false)
        val cancelled = BankEngine.cancel(unknown, unknown.currentTransferId!!)
        val retry = BankEngine.review(cancelled, TransferDraft(Fixtures.seoyeon, 32_000, Purpose.SETTLEMENT), now)
        assertEquals(TransferStage.VERIFY, evaluate(retry).current!!.stage)
        assertNotSent(retry)
    }
    @Test fun independentlyVerifiedRouteCorrectsPurposeAndRequiresFreshAuthorization() {
        val initial = evaluate(review(DemoScenario.LOAN, Purpose.OTHER))
        val id = initial.currentTransferId!!
        val route = BankEngine.resolveRoute(initial, id, now, true)
        val renewed = BankEngine.useOfficialRoute(route, id, now)
        assertNotEquals(id, renewed.currentTransferId)
        assertEquals(Purpose.LOAN, renewed.current!!.intent.purpose)
        assertEquals(TransferStage.REVIEW, renewed.current!!.stage)
        assertNull(renewed.current!!.authorization)
        assertNotSent(renewed)
        val result = evaluate(renewed)
        assertEquals(TransferStage.COMPLETE, result.current!!.stage)
        assertEquals(4_840_000L, result.balance)
        assertEquals(0L, result.loanBalance)
        assertEquals(1, result.receipts.count { !it.seed })
    }
    @Test fun expiredRequestDoesNotContaminateAnUnrelatedNewTransfer() {
        val state = review(DemoScenario.LOAN, Purpose.GENERAL, Fixtures.seoyeon)
        val future = now + Fixtures.EVENT_TTL + 1
        assertEquals(PolicyDecision.ALLOW, BankPolicy.evaluate(state.current!!, state.context, future).decision)
    }
    @Test fun locallyExtractedLoanPurposeIsIndependentOfDropdownAndRawContent() {
        val raw = "저금리로 바꾸려면 이 계좌로 기존 대출부터 갚아야 합니다. 비밀문구-7021"
        val events = SignalExtractor.extract(raw, now, RiskSource.SHARED_TEXT)
        assertTrue(events.any { it.type == RiskType.LOAN_REPAYMENT_REQUEST })
        assertTrue(events.none { it.summary.contains("7021") || it.summary == raw })
        val state = review(DemoScenario.NORMAL, Purpose.OTHER).copy(events = events)
        assertEquals(TransferStage.VERIFY, evaluate(state).current!!.stage)
        assertNotSent(state)
    }
    @Test fun aLoanMentionWithoutRepaymentInstructionDoesNotCreateThatSignal() {
        val events = SignalExtractor.extract("대출 상품의 금리와 만기를 비교해 주세요.", now, RiskSource.MANUAL_TEXT)
        assertTrue(events.none { it.type == RiskType.LOAN_REPAYMENT_REQUEST })
    }
}
