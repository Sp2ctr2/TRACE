package app.saeon.trace.data

import app.saeon.trace.core.*
import org.json.JSONArray
import org.json.JSONObject

/** Explicit versioned JSON, never Java object deserialization or raw message storage. */
object SnapshotCodec {
    private fun obj(vararg fields: Pair<String, Any?>): JSONObject = JSONObject().apply {
        fields.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
    }
    private fun <T> array(values: Iterable<T>, encode: (T) -> Any): JSONArray = JSONArray().apply { values.forEach { put(encode(it)) } }
    private fun JSONObject.nullString(key: String): String? = if (isNull(key) || !has(key)) null else getString(key)
    private fun <T> JSONArray.items(decode: (JSONObject) -> T): List<T> = (0 until length()).map { decode(getJSONObject(it)) }
    private fun riskSet(array: JSONArray): Set<RiskType> = (0 until array.length()).map { RiskType.valueOf(array.getString(it)) }.toSet()
    private fun recipient(value: Recipient) = obj("id" to value.id, "name" to value.name, "bank" to value.bank,
        "account" to value.account, "known" to value.known, "kind" to value.kind.name)
    private fun recipient(value: JSONObject) = Recipient(value.getString("id"), value.getString("name"), value.getString("bank"),
        value.getString("account"), value.getBoolean("known"), RecipientKind.valueOf(value.getString("kind")))
    private fun intent(value: TransactionIntent) = obj("id" to value.id, "amount" to value.amount, "recipient" to recipient(value.recipient),
        "purpose" to value.purpose.name, "createdAt" to value.createdAt, "fromAccount" to value.fromAccount,
        "originIntentId" to value.originIntentId, "officialRouteId" to value.officialRouteId)
    private fun intent(value: JSONObject) = TransactionIntent(value.getString("id"), value.getLong("amount"), recipient(value.getJSONObject("recipient")),
        Purpose.valueOf(value.getString("purpose")), value.getLong("createdAt"), value.getString("fromAccount"), value.nullString("originIntentId"), value.nullString("officialRouteId"))
    private fun challenge(value: AuthorizationChallenge) = obj("intentId" to value.intentId, "binding" to value.binding, "contextDigest" to value.contextDigest,
        "nonce" to value.nonce, "issuedAt" to value.issuedAt, "expiresAt" to value.expiresAt)
    private fun challenge(value: JSONObject) = AuthorizationChallenge(value.getString("intentId"), value.getString("binding"), value.getString("contextDigest"),
        value.getString("nonce"), value.getLong("issuedAt"), value.getLong("expiresAt"))
    private fun route(value: OfficialRoute) = obj("id" to value.id, "sourceIntentId" to value.sourceIntentId, "recipient" to recipient(value.recipient),
        "productId" to value.productId, "productName" to value.productName, "issuedAt" to value.issuedAt, "expiresAt" to value.expiresAt)
    private fun route(value: JSONObject) = OfficialRoute(value.getString("id"), value.getString("sourceIntentId"), recipient(value.getJSONObject("recipient")),
        value.getString("productId"), value.getString("productName"), value.getLong("issuedAt"), value.getLong("expiresAt"))
    private fun attestation(value: RiskAttestation) = obj("intentId" to value.intentId, "binding" to value.binding, "contextDigest" to value.contextDigest,
        "decision" to value.decision.name, "reasons" to array(value.reasonCodes) { it.name }, "nonce" to value.nonce,
        "issuedAt" to value.issuedAt, "expiresAt" to value.expiresAt, "keyId" to value.keyId,
        "rawContentExported" to value.rawContentExported, "signature" to value.signature)
    private fun attestation(value: JSONObject) = RiskAttestation(value.getString("intentId"), value.getString("binding"), value.getString("contextDigest"),
        PolicyDecision.valueOf(value.getString("decision")), riskSet(value.getJSONArray("reasons")), value.getString("nonce"),
        value.getLong("issuedAt"), value.getLong("expiresAt"), value.getString("keyId"), value.getBoolean("rawContentExported"), value.getString("signature"))
    private fun record(value: TransferRecord) = obj("intent" to intent(value.intent), "stage" to value.stage.name,
        "decision" to value.decision?.name, "reasons" to array(value.reasons) { it.name },
        "authorization" to value.authorization?.let { obj("challenge" to challenge(it.challenge), "method" to it.method.name) },
        "challenge" to value.challenge?.let(::challenge),
        "acknowledgement" to value.acknowledgement?.let { obj("binding" to it.binding, "contextDigest" to it.contextDigest, "acknowledgedAt" to it.acknowledgedAt) },
        "route" to value.route?.let(::route), "attestation" to value.attestation?.let(::attestation), "error" to value.error)
    private fun record(value: JSONObject) = TransferRecord(
        intent(value.getJSONObject("intent")), TransferStage.valueOf(value.getString("stage")), value.nullString("decision")?.let(PolicyDecision::valueOf),
        riskSet(value.getJSONArray("reasons")), value.optJSONObject("authorization")?.let { TransactionAuthorization(challenge(it.getJSONObject("challenge")), AuthMethod.valueOf(it.getString("method"))) },
        value.optJSONObject("challenge")?.let(::challenge), value.optJSONObject("acknowledgement")?.let { WarningAcknowledgement(it.getString("binding"), it.getString("contextDigest"), it.getLong("acknowledgedAt")) },
        value.optJSONObject("route")?.let(::route), value.optJSONObject("attestation")?.let(::attestation), value.nullString("error")
    )
    private fun receipt(value: TransferReceipt) = obj("id" to value.id, "intentId" to value.intentId, "recipient" to recipient(value.recipient),
        "amount" to value.amount, "completedAt" to value.completedAt, "purpose" to value.purpose.name, "balanceAfter" to value.balanceAfter,
        "nonce" to value.nonce, "direction" to value.direction.name, "fromAccount" to value.fromAccount, "memo" to value.memo, "seed" to value.seed)
    private fun receipt(value: JSONObject) = TransferReceipt(value.getString("id"), value.getString("intentId"), recipient(value.getJSONObject("recipient")),
        value.getLong("amount"), value.getLong("completedAt"), Purpose.valueOf(value.getString("purpose")), value.getLong("balanceAfter"), value.getString("nonce"),
        Direction.valueOf(value.getString("direction")), value.getString("fromAccount"), value.getString("memo"), value.getBoolean("seed"))
    private fun event(value: RiskEvent) = obj("id" to value.id, "type" to value.type.name, "createdAt" to value.createdAt, "expiresAt" to value.expiresAt,
        "source" to value.source.name, "summary" to value.summary, "recipientId" to value.recipientId)
    private fun event(value: JSONObject) = RiskEvent(value.getString("id"), RiskType.valueOf(value.getString("type")), value.getLong("createdAt"), value.getLong("expiresAt"),
        RiskSource.valueOf(value.getString("source")), value.getString("summary"), value.nullString("recipientId"))
    fun encode(state: BankState): String = obj(
        "version" to 1, "balance" to state.balance, "savings" to state.savings, "loanBalance" to state.loanBalance,
        "transferLimit" to state.transferLimit, "records" to array(state.records, ::record), "receipts" to array(state.receipts, ::receipt),
        "events" to array(state.events, ::event), "recipients" to array(state.recipients, ::recipient),
        "draft" to state.draft?.let { obj("recipient" to recipient(it.recipient), "amount" to it.amount, "purpose" to it.purpose.name) },
        "currentTransferId" to state.currentTransferId, "scenario" to state.scenario.name, "scenarioStartedAt" to state.scenarioStartedAt,
        "simulatedNow" to state.simulatedNow, "updatedAt" to state.updatedAt, "recurringEnabled" to state.recurringEnabled
    ).toString()
    fun decode(text: String): BankState {
        val value = JSONObject(text)
        require(value.getInt("version") == 1) { "Unsupported snapshot version" }
        return BankState(value.getLong("balance"), value.getLong("savings"), value.getLong("loanBalance"), value.getLong("transferLimit"),
            value.getJSONArray("records").items(::record), value.getJSONArray("receipts").items(::receipt), value.getJSONArray("events").items(::event),
            value.getJSONArray("recipients").items(::recipient), value.optJSONObject("draft")?.let { TransferDraft(recipient(it.getJSONObject("recipient")), it.getLong("amount"), Purpose.valueOf(it.getString("purpose"))) },
            value.nullString("currentTransferId"), DemoScenario.valueOf(value.getString("scenario")), value.getLong("scenarioStartedAt"),
            value.getLong("simulatedNow"), value.getLong("updatedAt"), value.getBoolean("recurringEnabled")
        ).also { state ->
            require(state.balance >= 0 && state.savings >= 0 && state.loanBalance >= 0)
            require(state.receipts.map { it.intentId }.distinct().size == state.receipts.size)
            require(state.records.map { it.intent.id }.distinct().size == state.records.size)
        }
    }
}
