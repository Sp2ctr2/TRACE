package app.saeon.trace.core

import kotlin.random.Random

/** Deterministic model-based ledger exercise; no Android or test framework needed. */
object StateWalk {
    fun verify(seed: Int, steps: Int = 200): Int {
        val random = Random(seed)
        var now = Fixtures.epoch
        var state = Fixtures.initial(DemoScenario.entries[seed % DemoScenario.entries.size], now)
        val liquidAtStart = state.balance + state.savings
        val debtAtStart = state.loanBalance
        var accepted = 0
        repeat(steps) {
            now += random.nextLong(1, 1_500)
            val before = state
            try {
                val record = state.current
                state = when (random.nextInt(13)) {
                    0 -> BankEngine.review(state, TransferDraft(
                        Fixtures.recipients[random.nextInt(Fixtures.recipients.size)],
                        random.nextLong(1, 150_000), if (random.nextInt(5) == 0) Purpose.LOAN else Purpose.GENERAL), now)
                    1 -> if (record != null) BankEngine.prepare(state, record.intent.id, now).first else state
                    2 -> if (record?.challenge != null) BankEngine.authorize(state, record.challenge, AuthMethod.DEMO_CONFIRMATION, now) else state
                    3 -> if (record != null && record.stage == TransferStage.EVALUATING)
                        BankEngine.finish(state, record.intent.id, BankEngine.attest(state, record.intent.id, now, "walk-key"), now, true) else state
                    4 -> if (record != null) BankEngine.cancel(state, record.intent.id) else state
                    5 -> BankEngine.recover(state)
                    6 -> if (record != null) BankEngine.acknowledgeWarning(state, record.intent.id, now) else state
                    7 -> if (record != null) BankEngine.resolveRoute(state, record.intent.id, now, random.nextBoolean()) else state
                    8 -> if (record != null) BankEngine.useOfficialRoute(state, record.intent.id, now) else state
                    9 -> BankEngine.bringFromSavings(state, random.nextLong(1, 50_000), "walk-$seed-$it", now)
                    10 -> if (state.records.isNotEmpty()) state.copy(currentTransferId = state.records[random.nextInt(state.records.size)].intent.id) else state
                    11 -> {
                        now += if (random.nextBoolean()) 121_000 else 901_000
                        state
                    }
                    else -> if (record?.stage == TransferStage.EVALUATING) {
                        val packet = BankEngine.attest(state, record.intent.id, now, "walk-key")
                        val rejected = runCatching { BankEngine.finish(state, record.intent.id, packet.copy(binding = "tampered"), now, true) }
                        check(rejected.exceptionOrNull() is BankFailure) { "Tampered packet accepted: seed=$seed step=$it" }
                        state
                    } else state
                }
                accepted++
            } catch (_: BankFailure) {
                state = before
            }
            val outgoing = state.receipts.filter { !it.seed && it.direction == Direction.DEBIT }
            check(state.balance >= 0 && state.savings >= 0 && state.loanBalance >= 0)
            check(state.balance + state.savings == liquidAtStart - outgoing.sumOf { it.amount }) { "Money conservation: seed=$seed step=$it" }
            check(state.loanBalance == debtAtStart - outgoing.filter { it.purpose == Purpose.LOAN }.sumOf { it.amount })
            check(state.receipts.map { it.intentId }.distinct().size == state.receipts.size) { "Duplicate debit: seed=$seed step=$it" }
            state.records.forEach { record ->
                val receipts = outgoing.filter { it.intentId == record.intent.id }
                check((record.stage == TransferStage.COMPLETE) == receipts.isNotEmpty()) { "Stage/receipt mismatch: seed=$seed step=$it" }
                check(receipts.size <= 1)
                if (record.stage == TransferStage.HOLD) {
                    check(record.authorization == null && record.challenge == null)
                    check(BankEngine.recover(state).record(record.intent.id).stage == TransferStage.HOLD)
                }
            }
        }
        return accepted
    }
    @JvmStatic fun main(args: Array<String>) {
        var accepted = 0
        repeat(100) { accepted += verify(it) }
        println("PASS: 100 deterministic state walks, 20,000 attempted transitions, $accepted accepted/no-op transitions.")
        println("Checked liquid-money conservation, loan principal, receipt uniqueness, completion consistency, HOLD restoration, tamper rejection.")
    }
}
