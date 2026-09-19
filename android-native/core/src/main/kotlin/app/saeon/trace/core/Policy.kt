package app.saeon.trace.core

import java.text.Normalizer

object BankPolicy {
    const val AUTH_TTL = 120_000L
    const val ATTEST_TTL = 60_000L
    fun routeValid(intent: TransactionIntent, route: OfficialRoute?, now: Long): Boolean =
        route != null && route.issuedAt <= now && route.expiresAt > now &&
        route.productId == Fixtures.LOAN_ID && route.recipient == Fixtures.official &&
        intent.recipient == Fixtures.official && intent.purpose == Purpose.LOAN &&
        intent.originIntentId == route.sourceIntentId && intent.officialRouteId == route.id

    fun evaluate(record: TransferRecord, context: RiskContext, now: Long): Evaluation {
        val intent = record.intent
        val types = context.relevant(intent, now).map { it.type }.toMutableSet()
        if (!intent.recipient.known) types += RiskType.NEW_RECIPIENT
        val trustedRoute = routeValid(intent, record.route, now)
        if (intent.purpose == Purpose.LOAN && !trustedRoute) types += RiskType.PURPOSE_RECIPIENT_MISMATCH
        val base = when {
            RiskType.IMPERSONATION in types && RiskType.FINANCIAL_INSTRUCTION in types &&
                (RiskType.URGENCY in types || RiskType.NEW_RECIPIENT in types) -> PolicyDecision.HOLD
            RiskType.PURPOSE_RECIPIENT_MISMATCH in types -> PolicyDecision.VERIFY
            RiskType.SUSPICIOUS_LINK in types && (RiskType.FINANCIAL_INSTRUCTION in types || RiskType.URGENCY in types) -> PolicyDecision.WARN
            RiskType.URGENCY in types && RiskType.FINANCIAL_INSTRUCTION in types && RiskType.NEW_RECIPIENT in types -> PolicyDecision.WARN
            else -> PolicyDecision.ALLOW
        }
        val ack = record.acknowledgement
        val acceptedWarning = base == PolicyDecision.WARN && ack != null &&
            ack.binding == intent.binding && ack.contextDigest == context.digest(intent, now) &&
            ack.acknowledgedAt <= now && now - ack.acknowledgedAt < AUTH_TTL
        return Evaluation(if (acceptedWarning) PolicyDecision.ALLOW else base, types)
    }
}

/** A bounded, explainable local rule extractor, NOT a trained fraud classifier.
 * Raw input is never included in its output. URL text is not fetched or opened.
 */
object SignalExtractor {
    const val MAX_INPUT = 4_000
    fun extract(raw: String, now: Long, source: RiskSource): List<RiskEvent> {
        if (raw.isBlank()) throw BankFailure("SHARE_EMPTY", "확인할 내용이 비어 있어요. 받은 내용을 붙여 넣어 주세요. 송금은 실행하지 않았습니다.")
        if (raw.length > MAX_INPUT) throw BankFailure("INPUT_TOO_LONG", "한 번에 4,000자까지 확인할 수 있어요. 필요한 부분만 남겨 주세요. 송금은 실행하지 않았습니다.")
        val text = Normalizer.normalize(raw, Normalizer.Form.NFKC)
            .replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "").lowercase()
        val types = linkedSetOf<RiskType>()
        if (listOf("검찰", "경찰", "수사관", "금융기관", "안전계좌", "계좌가 위험").any(text::contains)) types += RiskType.IMPERSONATION
        if (listOf("지금", "즉시", "바로", "당장", "오늘 안", "긴급", "서둘러").any(text::contains)) types += RiskType.URGENCY
        if (Regex("(?:https?://|www\\.)\\S+", RegexOption.IGNORE_CASE).containsMatchIn(text)) types += RiskType.SUSPICIOUS_LINK
        if (listOf("송금", "입금", "이체", "상환", "보내세요", "보내주", "갚아", "돈을 옮").any(text::contains)) types += RiskType.FINANCIAL_INSTRUCTION
        return types.map { type -> RiskEvent(newId(), type, now, now + Fixtures.EVENT_TTL, source, type.explanation) }
    }
}
