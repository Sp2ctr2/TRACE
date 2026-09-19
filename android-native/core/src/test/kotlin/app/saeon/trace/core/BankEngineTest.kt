package app.saeon.trace.core

import org.junit.Assert.*
import org.junit.Test

class BankEngineTest {
    private val now = Fixtures.epoch
    private fun reviewed(scenario: DemoScenario, id: String = newId()): BankState {
        val state = Fixtures.initial(scenario, now)
        return BankEngine.review(state, TransferDraft(Fixtures.recipient(scenario), Fixtures.amount(scenario), Fixtures.purpose(scenario)), now, id)
    }
    private fun ready(state: BankState, id: String = state.currentTransferId!!): BankState {
        val (pending, challenge) = BankEngine.prepare(state, id, now)
        return BankEngine.authorize(pending, challenge, AuthMethod.DEMO_CONFIRMATION, now)
    }
    private fun finish(state: BankState): BankState {
        val id = state.currentTransferId!!
        return BankEngine.finish(state, id, BankEngine.attest(state, id, now, "test-key"), now, true)
    }
    private fun evaluated(scenario: DemoScenario): BankState = finish(ready(reviewed(scenario)))
    private fun failure(code: String? = null, block: () -> Unit) {
        try { block(); fail("Expected BankFailure") } catch (error: BankFailure) {
            if (code != null) assertEquals(code, error.code)
        }
    }
    @Test fun normalDebitsExactlyOnce() {
        val result = evaluated(DemoScenario.NORMAL)
        assertEquals(12_808_000L, result.balance)
        assertEquals(1, result.receipts.count { !it.seed })
        assertEquals(TransferStage.COMPLETE, result.current!!.stage)
    }
    @Test fun duplicateCallbackIsIdempotent() {
        val active = ready(reviewed(DemoScenario.NORMAL))
        val id = active.currentTransferId!!
        val packet = BankEngine.attest(active, id, now, "test")
        val first = BankEngine.finish(active, id, packet, now, true)
        val second = BankEngine.finish(first, id, packet, now, true)
        assertEquals(first, second)
    }
    @Test fun holdNeverCommits() {
        val result = evaluated(DemoScenario.IMPERSONATION)
        assertEquals(TransferStage.HOLD, result.current!!.stage)
        assertEquals(Fixtures.START_BALANCE, result.balance)
        assertTrue(result.receipts.none { !it.seed })
        failure("COMMIT_FORBIDDEN") { BankEngine.finish(result, result.currentTransferId!!, result.current!!.attestation!!.copy(decision = PolicyDecision.ALLOW), now, true) }
    }
    @Test fun restoredHoldCannotCommit() {
        val restored = BankEngine.recover(evaluated(DemoScenario.IMPERSONATION))
        assertEquals(TransferStage.HOLD, restored.current!!.stage)
        failure("STATE_NOT_REVIEW") { BankEngine.prepare(restored, restored.currentTransferId!!, now) }
    }
    @Test fun holdCannotBeAcknowledged() {
        val result = evaluated(DemoScenario.IMPERSONATION)
        failure("NOT_A_WARNING") { BankEngine.acknowledgeWarning(result, result.currentTransferId!!, now) }
    }
    @Test fun holdCannotBeEdited() {
        val result = evaluated(DemoScenario.IMPERSONATION)
        failure("REVISION_FORBIDDEN") { BankEngine.revise(result, result.currentTransferId!!, TransferDraft(Fixtures.kim, 1), now) }
    }
    @Test fun verifyWithoutRouteNeverCommits() {
        val result = evaluated(DemoScenario.LOAN)
        assertEquals(TransferStage.VERIFY, result.current!!.stage)
        assertEquals(Fixtures.START_BALANCE, result.balance)
        failure("COMMIT_FORBIDDEN") { BankEngine.finish(result, result.currentTransferId!!, result.current!!.attestation!!, now, true) }
    }
    @Test fun unknownIsFailClosed() {
        val state = evaluated(DemoScenario.UNKNOWN)
        val result = BankEngine.resolveRoute(state, state.currentTransferId!!, now, false)
        assertEquals(TransferStage.UNKNOWN, result.current!!.stage)
        assertEquals(Fixtures.START_BALANCE, result.balance)
        assertTrue(result.receipts.none { !it.seed })
        failure("STATE_NOT_REVIEW") { BankEngine.prepare(result, result.currentTransferId!!, now) }
    }
    @Test fun officialRouteCreatesNewIntentWithoutAutoCommit() {
        val initial = evaluated(DemoScenario.LOAN)
        val oldId = initial.currentTransferId!!
        val routed = BankEngine.resolveRoute(initial, oldId, now, true)
        val renewed = BankEngine.useOfficialRoute(routed, oldId, now)
        assertNotEquals(oldId, renewed.currentTransferId)
        assertEquals(oldId, renewed.current!!.intent.originIntentId)
        assertNull(renewed.current!!.authorization)
        assertEquals(Fixtures.START_BALANCE, renewed.balance)
        assertEquals(TransferStage.REVIEW, renewed.current!!.stage)
        val result = finish(ready(renewed))
        assertEquals(4_840_000L, result.balance)
        assertEquals(0L, result.loanBalance)
    }
    @Test fun officialLabelAloneDoesNotAuthorizeLoan() {
        val initial = Fixtures.initial()
        val state = BankEngine.review(initial, TransferDraft(Fixtures.official, 100_000, Purpose.LOAN), now)
        assertEquals(PolicyDecision.VERIFY, BankPolicy.evaluate(state.current!!, state.context, now).decision)
    }
    @Test fun expiredRouteCannotBeUsed() {
        val initial = evaluated(DemoScenario.LOAN)
        val routed = BankEngine.resolveRoute(initial, initial.currentTransferId!!, now, true)
        failure("ROUTE_EXPIRED") { BankEngine.useOfficialRoute(routed, routed.currentTransferId!!, now + 301_000) }
    }
    @Test fun amountChangeInvalidatesOldApproval() {
        val first = ready(reviewed(DemoScenario.NORMAL))
        val changed = first.withRecord(first.current!!.copy(intent = first.current!!.intent.copy(amount = 1_000)))
        val oldPacket = BankEngine.attest(first, first.currentTransferId!!, now, "test")
        failure("TRANSACTION_CHANGED") { BankEngine.finish(changed, changed.currentTransferId!!, oldPacket, now, true) }
    }
    @Test fun recipientChangeInvalidatesOldApproval() {
        val first = ready(reviewed(DemoScenario.NORMAL))
        val changed = first.withRecord(first.current!!.copy(intent = first.current!!.intent.copy(recipient = Fixtures.family)))
        failure("TRANSACTION_CHANGED") { BankEngine.finish(changed, changed.currentTransferId!!, BankEngine.attest(first, first.currentTransferId!!, now, "test"), now, true) }
    }
    @Test fun purposeChangeInvalidatesOldApproval() {
        val first = ready(reviewed(DemoScenario.NORMAL))
        val changed = first.withRecord(first.current!!.copy(intent = first.current!!.intent.copy(purpose = Purpose.LOAN)))
        failure("TRANSACTION_CHANGED") { BankEngine.finish(changed, changed.currentTransferId!!, BankEngine.attest(first, first.currentTransferId!!, now, "test"), now, true) }
    }
    @Test fun accountChangeInvalidatesOldApproval() {
        val first = ready(reviewed(DemoScenario.NORMAL))
        val original = first.current!!
        val changed = first.withRecord(original.copy(intent = original.intent.copy(recipient = original.intent.recipient.copy(account = "changed"))))
        failure("TRANSACTION_CHANGED") { BankEngine.finish(changed, changed.currentTransferId!!, BankEngine.attest(first, first.currentTransferId!!, now, "test"), now, true) }
    }
    @Test fun newContextDuringAuthRequiresNewReview() {
        val reviewed = reviewed(DemoScenario.NORMAL)
        val (pending, challenge) = BankEngine.prepare(reviewed, reviewed.currentTransferId!!, now)
        val changed = pending.copy(events = listOf(RiskEvent("new", RiskType.URGENCY, now, now + 60_000, RiskSource.MANUAL_TEXT, "정형 신호")))
        failure("TRANSACTION_CHANGED") { BankEngine.authorize(changed, challenge, AuthMethod.DEMO_CONFIRMATION, now) }
    }
    @Test fun newContextDuringEvaluationCannotRaceCommit() {
        val state = ready(reviewed(DemoScenario.NORMAL))
        val changed = state.copy(events = listOf(RiskEvent("new", RiskType.URGENCY, now, now + 60_000, RiskSource.MANUAL_TEXT, "정형 신호")))
        failure("TRANSACTION_CHANGED") { BankEngine.finish(changed, changed.currentTransferId!!, BankEngine.attest(state, state.currentTransferId!!, now, "key"), now, true) }
    }
    @Test fun badSignatureNeverMovesMoney() {
        val state = ready(reviewed(DemoScenario.NORMAL))
        failure("ATTESTATION_INVALID") { BankEngine.finish(state, state.currentTransferId!!, BankEngine.attest(state, state.currentTransferId!!, now, "key"), now, false) }
        assertEquals(Fixtures.START_BALANCE, state.balance)
    }
    @Test fun rawContentExportClaimIsRejected() {
        val state = ready(reviewed(DemoScenario.NORMAL))
        val packet = BankEngine.attest(state, state.currentTransferId!!, now, "key").copy(rawContentExported = true)
        failure("ATTESTATION_INVALID") { BankEngine.finish(state, state.currentTransferId!!, packet, now, true) }
    }
    @Test fun forgedDecisionCannotOverridePolicy() {
        val state = ready(reviewed(DemoScenario.IMPERSONATION))
        val packet = BankEngine.attest(state, state.currentTransferId!!, now, "key").copy(decision = PolicyDecision.ALLOW)
        failure("POLICY_CHANGED") { BankEngine.finish(state, state.currentTransferId!!, packet, now, true) }
    }
    @Test fun expiredPacketCannotCommit() {
        val state = ready(reviewed(DemoScenario.NORMAL))
        val packet = BankEngine.attest(state, state.currentTransferId!!, now, "key")
        failure("ATTESTATION_EXPIRED") { BankEngine.finish(state, state.currentTransferId!!, packet, now + 61_000, true) }
    }
    @Test fun nonceReplayAcrossTransactionsIsRejected() {
        val first = evaluated(DemoScenario.NORMAL)
        val state = BankEngine.review(first, TransferDraft(Fixtures.seoyeon, 10_000), now)
        val oldNonce = first.receipts.first { !it.seed }.nonce
        val (pending, challenge) = BankEngine.prepare(state, state.currentTransferId!!, now, oldNonce)
        val active = BankEngine.authorize(pending, challenge, AuthMethod.DEMO_CONFIRMATION, now)
        failure("REPLAY") { finish(active) }
    }
    @Test fun cancelledBiometricReturnsReviewNotSuccess() {
        val state = reviewed(DemoScenario.NORMAL)
        val (pending, _) = BankEngine.prepare(state, state.currentTransferId!!, now)
        val cancelled = BankEngine.cancelAuthorization(pending, state.currentTransferId!!)
        assertEquals(TransferStage.REVIEW, cancelled.current!!.stage)
        assertEquals(Fixtures.START_BALANCE, cancelled.balance)
        assertNull(cancelled.current!!.authorization)
    }
    @Test fun interruptedEvaluationRestoresToReview() {
        val recovered = BankEngine.recover(ready(reviewed(DemoScenario.NORMAL)))
        assertEquals(TransferStage.REVIEW, recovered.current!!.stage)
        assertNull(recovered.current!!.authorization)
        assertTrue(recovered.receipts.none { !it.seed })
    }
    @Test fun insufficientBalanceRejected() {
        failure("INSUFFICIENT_BALANCE") { BankEngine.review(Fixtures.initial().copy(balance = 3), TransferDraft(Fixtures.seoyeon, 4), now) }
    }
    @Test fun zeroNegativeAndOverflowRejected() {
        for (amount in listOf(0L, -1L, Long.MAX_VALUE)) failure { BankEngine.review(Fixtures.initial(), TransferDraft(Fixtures.seoyeon, amount), now) }
    }
    @Test fun expiredEventsNoLongerInfluenceNewIntent() {
        val state = reviewed(DemoScenario.IMPERSONATION)
        assertEquals(PolicyDecision.ALLOW, BankPolicy.evaluate(state.current!!, state.context, now + Fixtures.EVENT_TTL + 1).decision)
    }
    @Test fun futureEventsAreNotActive() {
        val state = reviewed(DemoScenario.IMPERSONATION)
        val context = RiskContext(state.events.map { it.copy(createdAt = now + 60_000) })
        assertEquals(PolicyDecision.ALLOW, BankPolicy.evaluate(state.current!!, context, now).decision)
    }
    @Test fun warnRequiresAcknowledgementAndNewAuth() {
        val state = evaluated(DemoScenario.WARN)
        assertEquals(TransferStage.WARN, state.current!!.stage)
        val acked = BankEngine.acknowledgeWarning(state, state.currentTransferId!!, now)
        assertEquals(TransferStage.REVIEW, acked.current!!.stage)
        assertNull(acked.current!!.authorization)
        val result = finish(ready(acked))
        assertEquals(12_720_000L, result.balance)
    }
    @Test fun warningEscalationCannotBeAcknowledgedAway() {
        val state = evaluated(DemoScenario.WARN)
        val escalated = state.copy(events = Fixtures.events(DemoScenario.IMPERSONATION, now))
        val result = BankEngine.acknowledgeWarning(escalated, escalated.currentTransferId!!, now)
        assertEquals(TransferStage.HOLD, result.current!!.stage)
    }
    @Test fun duplicateSavingsImportDoesNotMintMoney() {
        val first = BankEngine.bringFromSavings(Fixtures.initial(), 20_000, "operation", now)
        val second = BankEngine.bringFromSavings(first, 20_000, "operation", now)
        assertEquals(first, second)
        assertEquals(15_240_000L, second.balance + second.savings)
    }
    @Test fun manualInputExportsOnlyStructuredSignals() {
        val secret = "개인 암호 문구 abc123"
        val events = SignalExtractor.extract("경찰입니다. 지금 http://example.invalid 로 들어가 입금하세요. $secret", now, RiskSource.SHARED_TEXT)
        assertEquals(4, events.size)
        assertTrue(events.none { it.summary.contains(secret) || it.summary.contains("abc123") || it.summary.contains("http") })
    }
    @Test fun emptyShareDoesNotCreateRiskEvents() {
        failure("SHARE_EMPTY") { SignalExtractor.extract("  ", now, RiskSource.SHARED_TEXT) }
    }
    @Test fun signalInputIsBounded() {
        failure("INPUT_TOO_LONG") { SignalExtractor.extract("가".repeat(4_001), now, RiskSource.SHARED_TEXT) }
    }
    @Test fun transactionBindingIsLengthPrefixed() {
        assertNotEquals(Digests.fields("ab", "c"), Digests.fields("a", "bc"))
        assertEquals(64, reviewed(DemoScenario.NORMAL).current!!.intent.binding.length)
    }
}
