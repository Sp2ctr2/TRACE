package app.saeon.trace.core

/** Pure bank-policy state machine. The Android repository persists each returned
 * state in one SQLite transaction. UI navigation is never an authorization.
 */
object BankEngine {
    private fun requireBank(ok: Boolean, code: String, text: String) {
        if (!ok) throw BankFailure(code, text)
    }
    fun validateAmount(state: BankState, amount: Long, purpose: Purpose = Purpose.GENERAL, now: Long = state.simulatedNow) {
        requireBank(amount > 0, "ZERO_AMOUNT", "1원 이상 입력해 주세요. 아직 돈은 나가지 않았습니다.")
        requireBank(amount <= 100_000_000L, "AMOUNT_TOO_LARGE", "입력할 수 있는 금액을 넘었어요. 금액을 줄여 주세요. 돈은 나가지 않았습니다.")
        requireBank(amount <= state.balance, "INSUFFICIENT_BALANCE", "잔액이 부족해요. 금액을 줄이거나 출금 계좌를 확인해 주세요. 돈은 나가지 않았습니다.")
        val spent = state.receipts.filter { !it.seed && it.direction == Direction.DEBIT && dayInSeoul(it.completedAt) == dayInSeoul(now) }.sumOf { it.amount }
        requireBank(amount <= state.transferLimit && spent <= state.transferLimit - amount, "TRANSFER_LIMIT", "오늘 보낼 수 있는 한도를 넘었어요. 송금 설정에서 한도를 확인해 주세요. 돈은 나가지 않았습니다.")
        if (purpose == Purpose.LOAN) requireBank(amount <= state.loanBalance, "LOAN_AMOUNT", "남은 대출보다 큰 금액이에요. 상환 금액을 다시 확인해 주세요. 돈은 나가지 않았습니다.")
    }
    fun review(state: BankState, draft: TransferDraft, now: Long, id: String = newId()): BankState {
        validateAmount(state, draft.amount, draft.purpose, now)
        requireBank(draft.recipient.id.isNotBlank() && draft.recipient.account.isNotBlank(), "RECIPIENT_MISSING", "받는 계좌를 다시 선택해 주세요. 아직 돈은 나가지 않았습니다.")
        requireBank(state.records.none { it.intent.id == id }, "DUPLICATE_ID", "이미 사용한 거래 번호예요. 새 송금으로 다시 확인해 주세요. 돈은 추가로 나가지 않았습니다.")
        val intent = TransactionIntent(id, draft.amount, draft.recipient, draft.purpose, now)
        return state.withRecord(TransferRecord(intent)).copy(currentTransferId = id, draft = draft, updatedAt = now)
    }
    fun prepare(state: BankState, id: String, now: Long, nonce: String = newId()): Pair<BankState, AuthorizationChallenge> {
        val record = state.record(id)
        requireBank(record.stage == TransferStage.REVIEW, "STATE_NOT_REVIEW", "지금 상태에서는 송금할 수 없어요. 거래 상태를 다시 확인해 주세요. 돈은 새로 나가지 않았습니다.")
        validateAmount(state, record.intent.amount, record.intent.purpose, now)
        val challenge = AuthorizationChallenge(id, record.intent.binding, state.context.digest(record.intent, now), nonce, now, now + BankPolicy.AUTH_TTL)
        return state.withRecord(record.copy(stage = TransferStage.AUTHORIZING, challenge = challenge, authorization = null, attestation = null, error = null)) to challenge
    }
    fun authorize(state: BankState, challenge: AuthorizationChallenge, method: AuthMethod, now: Long): BankState {
        val record = state.record(challenge.intentId)
        requireBank(record.stage == TransferStage.AUTHORIZING && record.challenge == challenge, "STALE_CALLBACK", "이전 인증 응답은 사용할 수 없어요. 송금을 다시 확인해 주세요. 돈은 나가지 않았습니다.")
        requireBank(challenge.issuedAt <= now && challenge.expiresAt > now, "AUTH_EXPIRED", "인증 시간이 지났어요. 송금을 다시 확인해 주세요. 돈은 나가지 않았습니다.")
        requireBank(challenge.binding == record.intent.binding && challenge.contextDigest == state.context.digest(record.intent, now), "TRANSACTION_CHANGED", "거래 또는 위험 정황이 바뀌었어요. 바뀐 내용을 다시 확인해 주세요. 돈은 나가지 않았습니다.")
        return state.withRecord(record.copy(stage = TransferStage.EVALUATING, authorization = TransactionAuthorization(challenge, method)))
    }
    fun attest(state: BankState, id: String, now: Long, keyId: String): RiskAttestation {
        val record = state.record(id)
        val auth = record.authorization ?: throw BankFailure("AUTH_REQUIRED", "거래 인증이 필요해요. 돈은 나가지 않았습니다.")
        requireBank(record.stage == TransferStage.EVALUATING, "NOT_EVALUATING", "지금은 송금을 실행할 수 없어요. 상태를 다시 확인해 주세요. 돈은 새로 나가지 않았습니다.")
        val decision = BankPolicy.evaluate(record, state.context, now)
        return RiskAttestation(id, record.intent.binding, state.context.digest(record.intent, now), decision.decision,
            decision.reasons, auth.challenge.nonce, now, minOf(now + BankPolicy.ATTEST_TTL, auth.challenge.expiresAt), keyId)
    }
    fun finish(state: BankState, id: String, packet: RiskAttestation, now: Long, signatureValid: Boolean): BankState {
        // A duplicate completion returns the durable receipt; it cannot debit again.
        if (state.receipts.any { it.intentId == id && !it.seed }) return state
        val record = state.record(id)
        requireBank(record.stage == TransferStage.EVALUATING, "COMMIT_FORBIDDEN", "보류되거나 확인 중인 송금은 실행할 수 없어요. 돈은 나가지 않았습니다.")
        val auth = record.authorization ?: throw BankFailure("AUTH_REQUIRED", "새 거래 인증이 필요해요. 돈은 나가지 않았습니다.")
        val challenge = auth.challenge
        val digest = state.context.digest(record.intent, now)
        requireBank(signatureValid && !packet.rawContentExported, "ATTESTATION_INVALID", "안전 확인 서명을 검증하지 못했어요. 송금은 실행하지 않았습니다. 다시 확인해 주세요.")
        requireBank(packet.intentId == id && packet.binding == record.intent.binding && packet.binding == challenge.binding &&
            packet.contextDigest == digest && challenge.contextDigest == digest && packet.nonce == challenge.nonce,
            "TRANSACTION_CHANGED", "거래 또는 위험 정황이 바뀌었어요. 새 인증이 필요합니다. 돈은 나가지 않았습니다.")
        requireBank(packet.issuedAt <= now && packet.expiresAt > now && challenge.issuedAt <= now && challenge.expiresAt > now,
            "ATTESTATION_EXPIRED", "안전 확인 시간이 지났어요. 다시 확인해 주세요. 돈은 나가지 않았습니다.")
        requireBank(state.receipts.none { it.nonce == packet.nonce }, "REPLAY", "이미 사용한 승인입니다. 송금은 추가로 실행하지 않았습니다.")
        val evaluation = BankPolicy.evaluate(record, state.context, now)
        requireBank(packet.decision == evaluation.decision && packet.reasonCodes == evaluation.reasons, "POLICY_CHANGED", "현재 정황과 안전 확인 결과가 달라요. 새로 확인해 주세요. 돈은 나가지 않았습니다.")
        val decision = evaluation.decision
        if (decision != PolicyDecision.ALLOW) {
            val stage = when (decision) {
                PolicyDecision.HOLD -> TransferStage.HOLD
                PolicyDecision.VERIFY -> TransferStage.VERIFY
                PolicyDecision.WARN -> TransferStage.WARN
                else -> error("Non-allow decision required")
            }
            return state.withRecord(record.copy(stage = stage, decision = decision, reasons = evaluation.reasons,
                authorization = null, challenge = null, attestation = packet)).copy(updatedAt = now)
        }
        validateAmount(state, record.intent.amount, record.intent.purpose, now)
        if (record.intent.purpose == Purpose.LOAN) requireBank(BankPolicy.routeValid(record.intent, record.route, now),
            "ROUTE_REQUIRED", "확인된 공식 상환 경로가 필요해요. 돈은 나가지 않았습니다.")
        val after = state.balance - record.intent.amount
        val receipt = TransferReceipt("SIM-${id.take(8).uppercase()}", id, record.intent.recipient, record.intent.amount,
            now, record.intent.purpose, after, packet.nonce, memo = if (record.intent.purpose == Purpose.LOAN) Fixtures.LOAN_NAME else record.intent.purpose.label)
        return state.withRecord(record.copy(stage = TransferStage.COMPLETE, decision = decision, reasons = evaluation.reasons,
            authorization = null, challenge = null, attestation = packet)).copy(
            balance = after, loanBalance = if (record.intent.purpose == Purpose.LOAN) state.loanBalance - record.intent.amount else state.loanBalance,
            receipts = listOf(receipt) + state.receipts, draft = null, updatedAt = now
        )
    }
    fun acknowledgeWarning(state: BankState, id: String, now: Long): BankState {
        val record = state.record(id)
        requireBank(record.stage == TransferStage.WARN, "NOT_A_WARNING", "이 송금은 확인 표시만으로 진행할 수 없어요. 돈은 나가지 않았습니다.")
        // Re-check before acknowledging. New evidence can escalate WARN to HOLD.
        val evaluation = BankPolicy.evaluate(record.copy(acknowledgement = null), state.context, now)
        if (evaluation.decision == PolicyDecision.HOLD || evaluation.decision == PolicyDecision.VERIFY) {
            return state.withRecord(record.copy(stage = if (evaluation.decision == PolicyDecision.HOLD) TransferStage.HOLD else TransferStage.VERIFY,
                decision = evaluation.decision, reasons = evaluation.reasons, acknowledgement = null))
        }
        return state.withRecord(record.copy(stage = TransferStage.REVIEW, authorization = null, challenge = null, attestation = null,
            acknowledgement = WarningAcknowledgement(record.intent.binding, state.context.digest(record.intent, now), now)))
    }
    fun resolveRoute(state: BankState, id: String, now: Long, available: Boolean): BankState {
        val record = state.record(id)
        requireBank(record.stage in setOf(TransferStage.VERIFY, TransferStage.UNKNOWN), "ROUTE_STATE", "현재 거래는 상환 경로 조회 대상이 아니에요. 송금을 실행하지 않았습니다.")
        requireBank(record.intent.purpose == Purpose.LOAN, "ROUTE_PURPOSE", "대출 상환 거래가 아니에요. 송금을 실행하지 않았습니다.")
        if (!available) return state.withRecord(record.copy(stage = TransferStage.UNKNOWN, route = null, authorization = null, challenge = null)).copy(updatedAt = now)
        val route = OfficialRoute(newId(), id, Fixtures.official, Fixtures.LOAN_ID, Fixtures.LOAN_NAME, now, now + 5 * 60_000)
        return state.withRecord(record.copy(stage = TransferStage.ROUTE, route = route, authorization = null, challenge = null)).copy(updatedAt = now)
    }
    fun useOfficialRoute(state: BankState, id: String, now: Long, newIntentId: String = newId()): BankState {
        val old = state.record(id)
        val route = old.route ?: throw BankFailure("ROUTE_REQUIRED", "공식 경로를 먼저 확인해 주세요. 아직 돈은 나가지 않았습니다.")
        requireBank(old.stage == TransferStage.ROUTE && route.sourceIntentId == id && route.expiresAt > now && route.issuedAt <= now,
            "ROUTE_EXPIRED", "공식 경로 확인 시간이 지났어요. 다시 조회해 주세요. 돈은 나가지 않았습니다.")
        requireBank(newIntentId != id && state.records.none { it.intent.id == newIntentId }, "NEW_INTENT_REQUIRED", "새 거래 번호가 필요해요. 이전 인증은 사용할 수 없습니다. 돈은 나가지 않았습니다.")
        val intent = TransactionIntent(newIntentId, old.intent.amount, route.recipient, Purpose.LOAN, now,
            originIntentId = id, officialRouteId = route.id)
        requireBank(BankPolicy.routeValid(intent, route, now), "ROUTE_INVALID", "공식 상환 경로가 일치하지 않아요. 송금을 실행하지 않았습니다.")
        return state.withRecord(old.copy(stage = TransferStage.SUPERSEDED, authorization = null, challenge = null))
            .withRecord(TransferRecord(intent, route = route)).copy(currentTransferId = newIntentId, draft = null, updatedAt = now)
    }
    fun cancel(state: BankState, id: String): BankState {
        val record = state.record(id)
        requireBank(record.stage != TransferStage.COMPLETE, "ALREADY_COMPLETED", "이미 완료된 송금입니다. 송금 내역에서 영수증을 확인해 주세요.")
        return state.withRecord(record.copy(stage = TransferStage.CANCELLED, authorization = null, challenge = null, attestation = null))
            .copy(currentTransferId = if (state.currentTransferId == id) null else state.currentTransferId, draft = null)
    }
    fun cancelAuthorization(state: BankState, id: String, message: String? = null): BankState {
        val record = state.record(id)
        if (record.stage !in setOf(TransferStage.AUTHORIZING, TransferStage.EVALUATING)) return state
        return state.withRecord(record.copy(stage = TransferStage.REVIEW, authorization = null, challenge = null, attestation = null, error = message))
    }
    fun recover(state: BankState): BankState = state.copy(records = state.records.map {
        if (it.stage in setOf(TransferStage.AUTHORIZING, TransferStage.EVALUATING))
            it.copy(stage = TransferStage.REVIEW, authorization = null, challenge = null, attestation = null,
                error = "중단된 송금입니다. 완료 내역이 없는 거래는 자동으로 보내지 않습니다. 다시 확인해 주세요.")
        else it
    })
    fun revise(state: BankState, id: String, draft: TransferDraft, now: Long, newIntentId: String = newId()): BankState {
        val old = state.record(id)
        requireBank(old.stage in setOf(TransferStage.REVIEW, TransferStage.WARN), "REVISION_FORBIDDEN", "보류 중인 송금은 이 화면에서 변경하거나 실행할 수 없어요. 공식 경로로 확인하거나 취소해 주세요.")
        return review(state.withRecord(old.copy(stage = TransferStage.SUPERSEDED, authorization = null, challenge = null, attestation = null)), draft, now, newIntentId)
    }
    fun bringFromSavings(state: BankState, amount: Long, operationId: String, now: Long): BankState {
        if (state.receipts.any { it.intentId == operationId }) return state
        requireBank(amount > 0 && amount <= state.savings, "SAVINGS_BALANCE", "모아적금에서 가져올 수 있는 금액을 확인해 주세요. 잔액은 바뀌지 않았습니다.")
        val after = state.balance + amount
        val receipt = TransferReceipt("SIM-${operationId.take(8).uppercase()}", operationId,
            Recipient("savings", "새온 모아적금", "새온은행", "220-***-0102", true, RecipientKind.INSTITUTION),
            amount, now, Purpose.OTHER, after, "internal-$operationId", Direction.CREDIT,
            "새온 모아적금", "내 계좌에서 가져오기")
        return state.copy(balance = after, savings = state.savings - amount, receipts = listOf(receipt) + state.receipts, updatedAt = now)
    }
}
