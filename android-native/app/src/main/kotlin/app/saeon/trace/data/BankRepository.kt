package app.saeon.trace.data

import androidx.room.withTransaction
import app.saeon.trace.core.*
import app.saeon.trace.security.DemoBankGateway
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BankRepository(
    private val database: BankDatabase,
    val clock: DemoClock,
    private val gateway: DemoBankGateway,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<BankState?>(null)
    val state: StateFlow<BankState?> = _state.asStateFlow()
    private val _fatal = MutableStateFlow<String?>(null)
    val fatal: StateFlow<String?> = _fatal.asStateFlow()
    private var observer: Job? = null
    private var publishedRevision = -1L
    @Synchronized private fun publish(state: BankState, revision: Long) {
        // Room invalidation and the write result can arrive on different dispatchers.
        // Never let a delayed observer roll the visible ledger back to an older revision.
        if (revision >= publishedRevision) {
            publishedRevision = revision
            _state.value = state
        }
    }
    private fun decode(row: SnapshotEntity): BankState {
        check(Digests.sha256(row.payload) == row.checksum) { "Ledger checksum mismatch" }
        return SnapshotCodec.decode(row.payload)
    }
    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val loaded = database.withTransaction {
                    val row = database.snapshots().read()
                    val initial = if (row == null) {
                        clock.reset(); Fixtures.initial(now = clock.now())
                    } else {
                        val saved = decode(row)
                        clock.restore(saved.simulatedNow, row.storedAtWall)
                        BankEngine.recover(saved)
                    }
                    val revision = (row?.revision ?: 0) + 1
                    val restored = initial.copy(simulatedNow = clock.now())
                    persist(restored, revision)
                    restored to revision
                }
                publish(loaded.first, loaded.second)
                _fatal.value = null
                if (observer == null) observer = scope.launch(Dispatchers.IO) {
                    database.snapshots().observe().collect { row ->
                        if (row != null) try { publish(decode(row), row.revision) }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { _fatal.value = storageMessage }
                    }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { _fatal.value = storageMessage }
        }
    }
    private suspend fun persist(state: BankState, revision: Long) {
        val encoded = SnapshotCodec.encode(state)
        database.snapshots().write(SnapshotEntity(payload = encoded, checksum = Digests.sha256(encoded), revision = revision, storedAtWall = System.currentTimeMillis()))
    }
    suspend fun change(action: (BankState, Long) -> BankState): BankState = withContext(Dispatchers.IO) {
        mutex.withLock {
            val result = database.withTransaction {
                val row = database.snapshots().read() ?: throw BankFailure("DATABASE_NOT_READY", storageMessage)
                val current = decode(row)
                val now = clock.now()
                val updated = action(current, now).copy(simulatedNow = now, updatedAt = now)
                check(updated.balance >= 0 && updated.savings >= 0 && updated.loanBalance >= 0)
                check(updated.receipts.map { it.intentId }.distinct().size == updated.receipts.size)
                persist(updated, row.revision + 1)
                updated to row.revision + 1
            }
            publish(result.first, result.second)
            result.first
        }
    }
    suspend fun setDraft(draft: TransferDraft) = change { state, _ -> state.copy(draft = draft) }
    suspend fun reviewDraft(): BankState = change { state, now ->
        val draft = state.draft ?: throw BankFailure("RECIPIENT_MISSING", "받는 분을 먼저 선택해 주세요. 아직 돈은 나가지 않았습니다.")
        BankEngine.review(state, draft, now)
    }
    suspend fun editReview(id: String): BankState = change { state, _ ->
        val old = state.record(id)
        if (old.stage != TransferStage.REVIEW) throw BankFailure("EDIT_FORBIDDEN", "보류 중인 송금은 변경할 수 없어요. 돈은 나가지 않았습니다.")
        state.withRecord(old.copy(stage = TransferStage.SUPERSEDED, authorization = null, challenge = null, attestation = null))
            .copy(draft = TransferDraft(old.intent.recipient, old.intent.amount, old.intent.purpose), currentTransferId = null)
    }
    suspend fun prepare(id: String): AuthorizationChallenge {
        var challenge: AuthorizationChallenge? = null
        change { state, now -> BankEngine.prepare(state, id, now).also { challenge = it.second }.first }
        return checkNotNull(challenge)
    }
    suspend fun authorize(challenge: AuthorizationChallenge, method: AuthMethod) = change { state, now -> BankEngine.authorize(state, challenge, method, now) }
    suspend fun finish(id: String): BankState = change { state, now -> gateway.evaluateAndCommit(state, id, now) }
    suspend fun cancelAuthorization(id: String, message: String? = null) = change { state, _ -> BankEngine.cancelAuthorization(state, id, message) }
    suspend fun acknowledge(id: String) = change { state, now -> BankEngine.acknowledgeWarning(state, id, now) }
    suspend fun resolveRoute(id: String) = change { state, now ->
        val record = state.record(id)
        // An explicitly requested refresh discards the old route before querying.
        // Refreshing cannot create a new intent, authorization, receipt or debit.
        val pending = if (record.stage == TransferStage.ROUTE) state.withRecord(record.copy(stage = TransferStage.VERIFY, route = null)) else state
        gateway.resolve(pending, id, now)
    }
    suspend fun useRoute(id: String) = change { state, now -> BankEngine.useOfficialRoute(state, id, now) }
    suspend fun cancel(id: String) = change { state, _ -> BankEngine.cancel(state, id) }
    suspend fun selectTransfer(id: String) = change { state, _ -> state.record(id); state.copy(currentTransferId = id) }
    suspend fun addSignals(signals: List<RiskEvent>) = change { state, _ -> state.copy(events = (state.events + signals).distinctBy { it.id }) }
    suspend fun clearExpiredSignals() = change { state, now -> state.copy(events = state.events.filter { it.expiresAt > now }) }
    suspend fun bring(amount: Long, id: String) = change { state, now -> BankEngine.bringFromSavings(state, amount, id, now) }
    suspend fun setLimit(limit: Long) = change { state, _ ->
        if (limit !in 10_000L..100_000_000L) throw BankFailure("LIMIT_RANGE", "송금 한도는 1만 원부터 1억 원까지 설정할 수 있어요. 잔액은 바뀌지 않았습니다.")
        state.copy(transferLimit = limit)
    }
    suspend fun recurring(value: Boolean) = change { state, _ -> state.copy(recurringEnabled = value) }
    suspend fun addRecipient(recipient: Recipient) = change { state, _ ->
        if (state.recipients.none { it.id == recipient.id }) state.copy(recipients = state.recipients + recipient) else state
    }
    suspend fun reset(scenario: DemoScenario = DemoScenario.NORMAL) = withContext(Dispatchers.IO) {
        mutex.withLock {
            clock.reset()
            val fresh = Fixtures.initial(scenario, clock.now())
            val revision = database.withTransaction {
                val next = (database.snapshots().read()?.revision ?: 0) + 1
                persist(fresh, next)
                next
            }
            publish(fresh, revision)
            _fatal.value = null
        }
    }
    suspend fun expireSignalsForDemo() {
        clock.advance(Fixtures.EVENT_TTL + 1)
        change { state, _ -> state }
    }
    companion object {
        const val storageMessage = "저장된 거래 상태를 읽지 못했어요. 송금을 새로 실행하지 않았습니다. 다시 보내지 말고, 상태를 복구한 뒤 내역을 확인해 주세요."
    }
}
