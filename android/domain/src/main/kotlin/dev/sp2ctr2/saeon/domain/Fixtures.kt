package dev.sp2ctr2.saeon.domain

import java.time.Instant
import java.util.UUID

object Fixtures {
    const val INITIAL_BALANCE = 12_840_000L
    const val OPENING_BALANCE = 10_037_500L
    val CLOCK: Long = Instant.parse("2026-09-20T00:47:00Z").toEpochMilli()
    val friend = Recipient("friend", "이서연", "노을은행", "110-***-7421", true)
    val family = Recipient("family", "이가족", "새온은행", "110-***-4512", true)
    val suspect = Recipient("suspect", "김○○", "노을은행", "110-***-9802")
    val loanPayee = Recipient("loan-payee", "박○○", "노을은행", "100-***-5934")
    val official = Recipient("official-loan", "새온은행 대출상환센터", "새온은행", "200-***-3014")
    val recipients = listOf(friend, family, suspect, loanPayee)
    fun seed(wall: Long = System.currentTimeMillis(), scenario: Scenario = Scenario.NORMAL): BankState {
        val now = CLOCK
        val base = BankState(
            wallOrigin = wall, scenario = scenario,
            ledger = listOf(
                LedgerEntry("seed-coffee", "모퉁이 카페", -5_500, now - 86_400_000, "카드 결제"),
                LedgerEntry("seed-salary", "9월 급여", 2_840_000, now - 172_800_000, "입금"),
                LedgerEntry("seed-friend", "이서연", -32_000, now - 259_200_000, "송금")
            ),
            schedules = listOf(ScheduledTransfer("phone", Recipient("telecom", "새온 통신", "새온은행", "010-***-0625", true), 68_000, 25)),
            notices = listOf(Notice("welcome", "새온은행에 오신 것을 환영해요", "시연 계좌와 모든 기능은 이 기기 안에서 동작합니다.", now, "about"))
        )
        return withScenario(base, scenario, now)
    }
    fun withScenario(base: BankState, scenario: Scenario, now: Long): BankState {
        fun event(type: RiskType, minutesAgo: Int, summary: String) = RiskEvent(UUID.randomUUID().toString(), type, now - minutesAgo * 60_000, now + 15 * 60_000, EventSource.DEMO, summary)
        val events = when (scenario) {
            Scenario.IMPERSONATION -> listOf(
                event(RiskType.IMPERSONATION, 6, "공유된 요청에서 기관을 사칭한 정황"),
                event(RiskType.SUSPICIOUS_LINK, 5, "보안 확인을 명목으로 링크를 전달한 정황"),
                event(RiskType.URGENCY, 2, "확인할 시간 없이 즉시 이체를 요구"),
                event(RiskType.FINANCIAL_INSTRUCTION, 2, "안전계좌라는 명목으로 자금 이동을 요구")
            )
            Scenario.LOAN, Scenario.LOOKUP_FAILURE -> listOf(event(RiskType.PURPOSE_MISMATCH, 2, "대출 상환을 안내하면서 개인 계좌를 전달"))
            Scenario.WARN -> listOf(event(RiskType.SUSPICIOUS_LINK, 3, "공유한 내용에 확인되지 않은 링크가 포함됨"))
            Scenario.NORMAL -> emptyList()
        }
        return base.copy(epoch = UUID.randomUUID().toString(), draft = null, heldCase = false, risks = events, scenario = scenario)
    }
    fun defaultRecipient(scenario: Scenario) = when (scenario) {
        Scenario.IMPERSONATION -> suspect
        Scenario.LOAN, Scenario.LOOKUP_FAILURE -> loanPayee
        else -> friend
    }
    fun defaultAmount(scenario: Scenario) = when (scenario) {
        Scenario.IMPERSONATION -> 3_000_000L
        Scenario.LOAN, Scenario.LOOKUP_FAILURE -> 8_000_000L
        else -> 32_000L
    }
    fun message(scenario: Scenario) = when(scenario) {
        Scenario.NORMAL -> "어제 저녁값 32,000원 보내줄래?"
        Scenario.IMPERSONATION -> "계좌가 위험합니다. 지금 바로 안전계좌로 300만 원을 보내세요."
        Scenario.LOAN, Scenario.LOOKUP_FAILURE -> "저금리로 바꾸려면 이 계좌로 기존 대출부터 갚아야 합니다."
        Scenario.WARN -> "정산 내용은 이 링크에 있어. 확인하고 저녁값 보내줘."
    }
}
