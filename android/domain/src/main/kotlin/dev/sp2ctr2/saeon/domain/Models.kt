package dev.sp2ctr2.saeon.domain

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable enum class Decision { ALLOW, WARN, VERIFY, HOLD }
@Serializable enum class Phase { AMOUNT, REVIEW, EVALUATING, WARN, VERIFY, ROUTE, UNKNOWN, HOLD, COMPLETE, CANCELLED, ERROR }
@Serializable enum class Scenario { NORMAL, IMPERSONATION, LOAN, LOOKUP_FAILURE, WARN }
@Serializable enum class Purpose { SETTLEMENT, LIVING, FAMILY, REPAYMENT, PURCHASE, OTHER }
@Serializable enum class RiskType { IMPERSONATION, URGENCY, SUSPICIOUS_LINK, FINANCIAL_INSTRUCTION, NEW_RECIPIENT, PURPOSE_MISMATCH }
@Serializable enum class EventSource { DEMO, USER_SHARED }
@Serializable data class Recipient(val id: String, val name: String, val bank: String, val account: String, val saved: Boolean = false) {
    val displayAccount: String get() = "$bank · $account"
}
@Serializable data class RiskEvent(val id: String, val type: RiskType, val at: Long, val expiresAt: Long, val source: EventSource, val summary: String)
@Serializable data class RoutePermit(val transactionId: String, val amount: Long, val recipientId: String, val expiresAt: Long, val epoch: String)
@Serializable data class TransferDraft(
    val id: String = UUID.randomUUID().toString(), val recipient: Recipient,
    val amount: Long = 0, val purpose: Purpose = Purpose.SETTLEMENT,
    val phase: Phase = Phase.AMOUNT, val createdAt: Long,
    val warnAcknowledged: Boolean = false, val route: RoutePermit? = null,
    val previousRecipient: String? = null, val error: String? = null
)
@Serializable data class Receipt(val id: String, val transactionId: String, val recipient: Recipient, val amount: Long, val purpose: Purpose, val at: Long, val balanceAfter: Long, val proofDigest: String)
@Serializable data class LedgerEntry(val id: String, val title: String, val signedAmount: Long, val at: Long, val category: String, val receiptId: String? = null)
@Serializable data class Notice(val id: String, val title: String, val body: String, val at: Long, val route: String, val read: Boolean = false)
@Serializable data class ScheduledTransfer(val id: String, val recipient: Recipient, val amount: Long, val day: Int, val active: Boolean = true)
@Serializable data class BankState(
    val schemaVersion: Int = 1,
    val epoch: String = UUID.randomUUID().toString(),
    val balance: Long = Fixtures.INITIAL_BALANCE,
    val savings: Long = 2_400_000,
    val loan: Long = 8_000_000,
    val draft: TransferDraft? = null,
    val risks: List<RiskEvent> = emptyList(),
    val heldCase: Boolean = false,
    val scenario: Scenario = Scenario.NORMAL,
    val receipts: List<Receipt> = emptyList(),
    val ledger: List<LedgerEntry> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val schedules: List<ScheduledTransfer> = emptyList(),
    val favorites: Set<String> = setOf("friend", "family"),
    val dailyLimit: Long = 10_000_000,
    val clockOrigin: Long = Fixtures.CLOCK,
    val wallOrigin: Long = System.currentTimeMillis()
) {
    val totalAssets: Long get() = balance + savings
    val netAssets: Long get() = totalAssets - loan
    fun now(wall: Long = System.currentTimeMillis()): Long = clockOrigin + (wall - wallOrigin).coerceAtLeast(0)
}

data class AuthorizationProof(val transactionId: String, val binding: String, val epoch: String, val nonce: String, val issuedAt: Long, val expiresAt: Long, val signature: ByteArray)
interface ProofSigner { fun sign(payload: ByteArray): ByteArray; fun verify(payload: ByteArray, signature: ByteArray): Boolean }
class BankRuleException(message: String) : IllegalStateException(message)
