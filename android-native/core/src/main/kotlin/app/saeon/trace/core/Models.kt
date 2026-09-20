package app.saeon.trace.core

import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

object Digests {
    fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    // Length-prefixing prevents ambiguous concatenations in signed fields.
    fun fields(vararg values: String): String = values.joinToString("") { "${it.toByteArray(Charsets.UTF_8).size}:$it" }
}
fun newId(): String = UUID.randomUUID().toString()

enum class Purpose(val label: String) {
    GENERAL("선택 안 함"), LIVING("생활비"), SETTLEMENT("정산"), FAMILY("가족"),
    LOAN("대출 상환"), PURCHASE("상품·서비스"), OTHER("기타")
}
enum class RecipientKind { PERSON, INSTITUTION }
data class Recipient(
    val id: String, val name: String, val bank: String, val account: String,
    val known: Boolean = false, val kind: RecipientKind = RecipientKind.PERSON
)
enum class RiskType(val label: String, val explanation: String) {
    IMPERSONATION("기관 사칭 정황", "기관 관계자라고 소개하며 돈을 옮기도록 안내했어요."),
    URGENCY("즉시 송금 요구", "확인할 시간을 주지 않고 바로 보내라고 했어요."),
    SUSPICIOUS_LINK("확인하지 않은 링크", "상대가 보낸 링크가 금융 행동 요청과 가까운 시간에 나타났어요."),
    FINANCIAL_INSTRUCTION("금융 행동 요청", "계좌 이체나 대출 상환을 요구하는 내용이 있었어요."),
    LOAN_REPAYMENT_REQUEST("대출 상환 요청", "대출을 갚거나 바꾸기 위해 먼저 돈을 보내라는 요청이 있었어요."),
    NEW_RECIPIENT("새로운 수취인", "이 요청 뒤에 처음 보내는 계좌가 입력됐어요."),
    PURPOSE_RECIPIENT_MISMATCH("목적과 받는 곳의 불일치", "대출을 갚는 돈이지만 받는 곳은 확인된 상환 계좌가 아니에요.")
}
enum class RiskSource { DEMO_FIXTURE, SHARED_TEXT, MANUAL_TEXT, USER_VOICE, TRANSACTION }
data class RiskEvent(
    val id: String, val type: RiskType, val createdAt: Long, val expiresAt: Long,
    val source: RiskSource, val summary: String, val recipientId: String? = null
) {
    fun active(now: Long): Boolean = createdAt <= now && expiresAt > now
}
data class RiskContext(val events: List<RiskEvent>) {
    fun relevant(intent: TransactionIntent, now: Long): List<RiskEvent> = events.filter {
        it.active(now) && (it.recipientId == null || it.recipientId == intent.recipient.id)
    }
    fun digest(intent: TransactionIntent, now: Long): String = Digests.sha256(
        relevant(intent, now).sortedBy { it.id }.joinToString("") {
            Digests.fields(it.id, it.type.name, it.createdAt.toString(), it.expiresAt.toString(), it.source.name, it.recipientId.orEmpty())
        }
    )
}
data class TransactionIntent(
    val id: String, val amount: Long, val recipient: Recipient, val purpose: Purpose,
    val createdAt: Long, val fromAccount: String = "saeon-living",
    val originIntentId: String? = null, val officialRouteId: String? = null
) {
    val binding: String get() = Digests.sha256(Digests.fields(
        "TRACE/INTENT/1", id, amount.toString(), recipient.id, recipient.name,
        recipient.bank, recipient.account, recipient.kind.name, recipient.known.toString(),
        purpose.name, fromAccount, createdAt.toString(), originIntentId.orEmpty(), officialRouteId.orEmpty()
    ))
}
enum class PolicyDecision { ALLOW, WARN, VERIFY, HOLD }
enum class TransferStage {
    REVIEW, AUTHORIZING, EVALUATING, WARN, VERIFY, HOLD, UNKNOWN, ROUTE,
    COMPLETE, CANCELLED, SUPERSEDED
}
enum class AuthMethod { DEMO_CONFIRMATION, BIOMETRIC }
data class AuthorizationChallenge(
    val intentId: String, val binding: String, val contextDigest: String,
    val nonce: String, val issuedAt: Long, val expiresAt: Long
)
data class TransactionAuthorization(val challenge: AuthorizationChallenge, val method: AuthMethod)
data class WarningAcknowledgement(val binding: String, val contextDigest: String, val acknowledgedAt: Long)
data class OfficialRoute(
    val id: String, val sourceIntentId: String, val recipient: Recipient,
    val productId: String, val productName: String, val issuedAt: Long, val expiresAt: Long
)
data class RiskAttestation(
    val intentId: String, val binding: String, val contextDigest: String,
    val decision: PolicyDecision, val reasonCodes: Set<RiskType>, val nonce: String,
    val issuedAt: Long, val expiresAt: Long, val keyId: String,
    val rawContentExported: Boolean = false, val signature: String = ""
) {
    fun canonical(): String = Digests.fields(
        "TRACE/ATTESTATION/1", intentId, binding, contextDigest, decision.name,
        reasonCodes.map { it.name }.sorted().joinToString(","), nonce,
        issuedAt.toString(), expiresAt.toString(), keyId, rawContentExported.toString()
    )
}
data class Evaluation(val decision: PolicyDecision, val reasons: Set<RiskType>)
data class TransferRecord(
    val intent: TransactionIntent, val stage: TransferStage = TransferStage.REVIEW,
    val decision: PolicyDecision? = null, val reasons: Set<RiskType> = emptySet(),
    val authorization: TransactionAuthorization? = null, val challenge: AuthorizationChallenge? = null,
    val acknowledgement: WarningAcknowledgement? = null, val route: OfficialRoute? = null,
    val attestation: RiskAttestation? = null, val error: String? = null
)
enum class Direction { DEBIT, CREDIT }
data class TransferReceipt(
    val id: String, val intentId: String, val recipient: Recipient, val amount: Long,
    val completedAt: Long, val purpose: Purpose, val balanceAfter: Long,
    val nonce: String, val direction: Direction = Direction.DEBIT,
    val fromAccount: String = "새온 생활통장", val memo: String = "", val seed: Boolean = false
)
data class TransferDraft(val recipient: Recipient, val amount: Long = 0, val purpose: Purpose = Purpose.GENERAL)
enum class DemoScenario(val label: String, val expected: String) {
    NORMAL("정상 송금", "ALLOW"), IMPERSONATION("기관 사칭", "HOLD"),
    LOAN("대출 상환", "VERIFY → ALLOW"), UNKNOWN("공식 경로 조회 실패", "VERIFY → UNKNOWN"),
    WARN("보내기 전 확인", "WARN"), EASY("쉬운 모드", "HOLD")
}
data class BankState(
    val balance: Long = 12_840_000,
    val savings: Long = 2_400_000,
    val loanBalance: Long = 8_000_000,
    val transferLimit: Long = 10_000_000,
    val records: List<TransferRecord> = emptyList(),
    val receipts: List<TransferReceipt> = emptyList(),
    val events: List<RiskEvent> = emptyList(),
    val recipients: List<Recipient> = Fixtures.recipients,
    val draft: TransferDraft? = null,
    val currentTransferId: String? = null,
    val scenario: DemoScenario = DemoScenario.NORMAL,
    val scenarioStartedAt: Long = 0,
    val simulatedNow: Long = 0,
    val updatedAt: Long = 0,
    val recurringEnabled: Boolean = true
) {
    val context: RiskContext get() = RiskContext(events)
    val current: TransferRecord? get() = records.find { it.intent.id == currentTransferId }
    val pending: List<TransferRecord> get() = records.filter {
        it.stage in setOf(TransferStage.HOLD, TransferStage.WARN, TransferStage.VERIFY, TransferStage.UNKNOWN, TransferStage.ROUTE)
    }
    fun record(id: String): TransferRecord = records.find { it.intent.id == id }
        ?: throw BankFailure("TRANSACTION_MISSING", "송금 내역을 찾지 못했어요. 돈은 새로 보내지 않았습니다. 홈에서 다시 확인해 주세요.")
    fun withRecord(record: TransferRecord): BankState = copy(
        records = records.filterNot { it.intent.id == record.intent.id } + record
    )
}
class BankFailure(val code: String, override val message: String) : IllegalStateException(message)
fun dayInSeoul(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString()
