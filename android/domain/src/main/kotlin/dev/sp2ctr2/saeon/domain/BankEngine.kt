package dev.sp2ctr2.saeon.domain

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.util.UUID

/** Pure, deterministic policy and ledger boundary. No UI, network or Android dependency. */
object BankEngine {
    private fun guard(condition: Boolean, message: String) { if (!condition) throw BankRuleException(message) }
    fun activeRisks(state: BankState, now: Long) = state.risks.filter { it.at <= now && it.expiresAt > now }
    fun evaluate(state: BankState, draft: TransferDraft, now: Long): Decision {
        if (state.heldCase || draft.phase == Phase.HOLD) return Decision.HOLD
        val types = activeRisks(state, now).map { it.type }.toSet()
        if (RiskType.IMPERSONATION in types && RiskType.URGENCY in types && RiskType.FINANCIAL_INSTRUCTION in types) return Decision.HOLD
        if (draft.purpose == Purpose.REPAYMENT || RiskType.PURPOSE_MISMATCH in types) {
            val permit = draft.route
            val valid = draft.recipient == Fixtures.official && permit != null && permit.transactionId == draft.id && permit.amount == draft.amount && permit.recipientId == draft.recipient.id && permit.epoch == state.epoch && permit.expiresAt > now && draft.purpose == Purpose.REPAYMENT
            if (!valid) return Decision.VERIFY
        }
        if (RiskType.SUSPICIOUS_LINK in types) return Decision.WARN
        return Decision.ALLOW
    }
    fun validationError(state: BankState, draft: TransferDraft): String? = when {
        draft.amount <= 0 -> "1원 이상 입력해 주세요."
        draft.amount > state.balance -> "잔액 안에서 입력해 주세요."
        draft.amount > state.dailyLimit -> "설정한 송금 한도를 초과했어요."
        draft.purpose == Purpose.REPAYMENT && draft.amount > state.loan -> "남은 대출 잔액보다 큰 금액은 상환할 수 없어요."
        draft.recipient.id.isBlank() -> "받는 분을 먼저 선택해 주세요."
        else -> null
    }
    fun begin(state: BankState, recipient: Recipient, amount: Long, now: Long): BankState {
        if (state.draft?.phase == Phase.HOLD) return state
        val purpose = if (state.scenario in setOf(Scenario.LOAN, Scenario.LOOKUP_FAILURE)) Purpose.REPAYMENT else Purpose.SETTLEMENT
        return state.copy(draft = TransferDraft(recipient = recipient, amount = amount, purpose = purpose, createdAt = now))
    }
    fun edit(state: BankState, amount: Long, purpose: Purpose? = null): BankState {
        val d = state.draft ?: throw BankRuleException("송금을 다시 시작해 주세요.")
        guard(d.phase in setOf(Phase.AMOUNT, Phase.REVIEW, Phase.ERROR), "지금은 송금 정보를 바꿀 수 없어요.")
        return state.copy(draft = d.copy(id = UUID.randomUUID().toString(), amount = amount.coerceAtLeast(0), purpose = purpose ?: d.purpose, phase = Phase.AMOUNT, route = null, warnAcknowledged = false, error = null))
    }
    fun review(state: BankState): BankState {
        val d = state.draft ?: throw BankRuleException("송금을 다시 시작해 주세요.")
        guard(d.phase in setOf(Phase.AMOUNT, Phase.REVIEW, Phase.ERROR), "확인이 필요한 송금이 있어요.")
        validationError(state, d)?.let { throw BankRuleException(it) }
        return state.copy(draft = d.copy(phase = Phase.REVIEW, error = null))
    }
    fun assess(state: BankState, now: Long): BankState {
        val d = state.draft ?: throw BankRuleException("송금을 다시 시작해 주세요.")
        guard(d.phase in setOf(Phase.REVIEW, Phase.EVALUATING), "거래 정보를 다시 확인해 주세요.")
        validationError(state, d)?.let { throw BankRuleException(it) }
        val decision = evaluate(state, d, now)
        val phase = when(decision) {
            Decision.ALLOW -> Phase.REVIEW
            Decision.WARN -> if(d.warnAcknowledged) Phase.REVIEW else Phase.WARN
            Decision.VERIFY -> Phase.VERIFY
            Decision.HOLD -> Phase.HOLD
        }
        return state.copy(draft = d.copy(phase = phase), heldCase = state.heldCase || decision == Decision.HOLD)
    }
    fun acknowledgeWarning(state: BankState): BankState {
        val d = state.draft ?: throw BankRuleException("송금을 다시 시작해 주세요.")
        guard(d.phase == Phase.WARN && !state.heldCase, "이 송금은 계속할 수 없어요.")
        return state.copy(draft = d.copy(phase = Phase.REVIEW, warnAcknowledged = true))
    }
    fun lookupRoute(state: BankState, now: Long, fail: Boolean): BankState {
        val d = state.draft ?: throw BankRuleException("송금을 다시 시작해 주세요.")
        guard(d.phase in setOf(Phase.VERIFY, Phase.UNKNOWN, Phase.ROUTE) && !state.heldCase, "공식 경로를 확인할 수 없는 상태예요.")
        if (fail) return state.copy(draft = d.copy(phase = Phase.UNKNOWN, route = null))
        guard(d.amount > 0 && d.amount <= state.loan, "상환 금액을 다시 확인해 주세요.")
        // A route result is not authorization and never debits the ledger.
        return state.copy(draft = d.copy(phase = Phase.ROUTE, route = RoutePermit(d.id, d.amount, Fixtures.official.id, now + 120_000, state.epoch)))
    }
    fun acceptRoute(state: BankState, now: Long): BankState {
        val d = state.draft ?: throw BankRuleException("송금을 다시 시작해 주세요.")
        val permit = d.route ?: throw BankRuleException("공식 경로를 다시 확인해 주세요.")
        guard(d.phase == Phase.ROUTE && permit.transactionId == d.id && permit.amount == d.amount && permit.epoch == state.epoch && permit.expiresAt > now && !state.heldCase, "공식 경로 확인 시간이 지났어요. 다시 조회해 주세요.")
        val id = UUID.randomUUID().toString()
        return state.copy(draft = d.copy(id = id, recipient = Fixtures.official, purpose = Purpose.REPAYMENT, phase = Phase.REVIEW, createdAt = now, previousRecipient = d.recipient.name, warnAcknowledged = false, route = permit.copy(transactionId = id)))
    }
    fun binding(state: BankState, d: TransferDraft): String = digest(binary {
        writeInt(1); writeUTF(state.epoch); writeUTF(d.id); writeUTF("saeon-living")
        writeUTF(d.recipient.id); writeUTF(d.recipient.bank); writeUTF(d.recipient.account)
        writeLong(d.amount); writeUTF(d.purpose.name); writeBoolean(d.warnAcknowledged)
        writeUTF(d.route?.transactionId.orEmpty()); writeLong(d.route?.expiresAt ?: 0)
    })
    fun proofPayload(p: AuthorizationProof): ByteArray = binary {
        writeInt(1); writeUTF(p.transactionId); writeUTF(p.binding); writeUTF(p.epoch)
        writeUTF(p.nonce); writeLong(p.issuedAt); writeLong(p.expiresAt)
    }
    fun authorize(state: BankState, signer: ProofSigner, now: Long): AuthorizationProof {
        val d = state.draft ?: throw BankRuleException("송금을 다시 시작해 주세요.")
        guard(d.phase == Phase.REVIEW, "송금 내역을 먼저 확인해 주세요.")
        val p = AuthorizationProof(d.id, binding(state, d), state.epoch, UUID.randomUUID().toString(), now, now + 60_000, byteArrayOf())
        return p.copy(signature = signer.sign(proofPayload(p)))
    }
    fun commit(state: BankState, proof: AuthorizationProof, signer: ProofSigner, now: Long): BankState {
        // Idempotent retries return the original state, not a second receipt.
        if (state.receipts.any { it.transactionId == proof.transactionId }) return state
        val d = state.draft ?: throw BankRuleException("송금을 다시 시작해 주세요.")
        guard(d.phase == Phase.REVIEW, "이 상태에서는 송금을 실행할 수 없어요.")
        guard(!state.heldCase, "송금이 보류되어 있어요.")
        validationError(state, d)?.let { throw BankRuleException(it) }
        guard(proof.transactionId == d.id && proof.binding == binding(state, d) && proof.epoch == state.epoch, "거래 정보가 바뀌었어요. 다시 확인해 주세요.")
        guard(proof.issuedAt <= now && proof.expiresAt > now && proof.expiresAt - proof.issuedAt <= 60_000, "확인 시간이 지났어요. 다시 확인해 주세요.")
        guard(signer.verify(proofPayload(proof), proof.signature), "거래 확인 정보를 검증하지 못했어요. 돈은 나가지 않았습니다.")
        val decision = evaluate(state, d, now)
        guard(decision == Decision.ALLOW || (decision == Decision.WARN && d.warnAcknowledged), "추가 확인이 필요한 송금이에요.")
        val newBalance = Math.subtractExact(state.balance, d.amount)
        val id = "SN-${state.receipts.size.plus(1).toString().padStart(5, '0')}"
        val r = Receipt(id, d.id, d.recipient, d.amount, d.purpose, now, newBalance, digest(proof.signature))
        val repayment = d.purpose == Purpose.REPAYMENT && d.recipient == Fixtures.official
        return state.copy(
            balance = newBalance, loan = if(repayment) Math.subtractExact(state.loan, d.amount) else state.loan,
            draft = d.copy(phase = Phase.COMPLETE), receipts = state.receipts + r,
            ledger = listOf(LedgerEntry(UUID.randomUUID().toString(), d.recipient.name, -d.amount, now, if(repayment) "대출 상환" else "송금", id)) + state.ledger,
            notices = listOf(Notice(UUID.randomUUID().toString(), if(repayment) "대출 상환을 마쳤어요" else "송금을 마쳤어요", "${d.recipient.name}님에게 가상 거래가 완료됐어요.", now, "receipt/$id")) + state.notices
        )
    }
    fun cancel(state: BankState): BankState = state.copy(draft = state.draft?.copy(phase = Phase.CANCELLED, route = null))
    fun bringFromSavings(state: BankState, amount: Long, now: Long): BankState {
        guard(amount > 0 && amount <= state.savings, "가져올 수 있는 적금 잔액을 확인해 주세요.")
        return state.copy(balance = Math.addExact(state.balance, amount), savings = state.savings - amount,
            ledger = listOf(LedgerEntry(UUID.randomUUID().toString(), "내 적금에서 가져오기", amount, now, "내 계좌 이동")) + state.ledger)
    }
    fun ledgerIsConsistent(state: BankState) = state.balance == Fixtures.OPENING_BALANCE + state.ledger.sumOf { it.signedAmount } && state.balance >= 0 && state.savings >= 0 && state.loan >= 0
    private fun binary(write: DataOutputStream.() -> Unit): ByteArray = ByteArrayOutputStream().also { out -> DataOutputStream(out).use { it.write() } }.toByteArray()
    fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
