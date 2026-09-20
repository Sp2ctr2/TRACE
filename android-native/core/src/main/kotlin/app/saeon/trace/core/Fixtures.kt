package app.saeon.trace.core

import java.time.ZonedDateTime

object Fixtures {
    const val START_BALANCE = 12_840_000L
    const val EVENT_TTL = 15 * 60_000L
    val seoyeon = Recipient("seoyeon", "이서연", "노을은행", "110-***-7421", true)
    val kim = Recipient("kim", "김○○", "노을은행", "110-***-9802")
    val park = Recipient("park", "박○○", "노을은행", "100-***-5934")
    val family = Recipient("family", "한지민", "새온은행", "110-***-2018", true)
    val minjun = Recipient("minjun", "최민준", "노을은행", "110-***-8042", true)
    val official = Recipient("saeon-loan-center", "새온은행 대출상환센터", "새온은행", "200-***-3014", false, RecipientKind.INSTITUTION)
    val recipients = listOf(seoyeon, family, minjun, kim, park)
    const val LOAN_ID = "saeon-life-loan-01"
    const val LOAN_NAME = "생활안심대출"
    val epoch: Long = ZonedDateTime.parse("2026-09-20T09:47:00+09:00[Asia/Seoul]").toInstant().toEpochMilli()
    fun recipient(scenario: DemoScenario): Recipient = when (scenario) {
        DemoScenario.NORMAL -> seoyeon
        DemoScenario.IMPERSONATION, DemoScenario.EASY, DemoScenario.WARN -> kim
        DemoScenario.LOAN, DemoScenario.UNKNOWN -> park
    }
    fun amount(scenario: DemoScenario): Long = when (scenario) {
        DemoScenario.NORMAL -> 32_000
        DemoScenario.IMPERSONATION, DemoScenario.EASY -> 3_000_000
        DemoScenario.LOAN, DemoScenario.UNKNOWN -> 8_000_000
        DemoScenario.WARN -> 120_000
    }
    fun purpose(scenario: DemoScenario): Purpose = when (scenario) {
        DemoScenario.LOAN, DemoScenario.UNKNOWN -> Purpose.LOAN
        DemoScenario.NORMAL -> Purpose.SETTLEMENT
        else -> Purpose.GENERAL
    }
    fun message(scenario: DemoScenario): String = when (scenario) {
        DemoScenario.NORMAL -> "어제 저녁값 32,000원 보내줄래?"
        DemoScenario.IMPERSONATION, DemoScenario.EASY -> "계좌가 위험합니다. 지금 바로 안전계좌로 300만 원을 보내세요."
        DemoScenario.LOAN, DemoScenario.UNKNOWN -> "저금리로 바꾸려면 이 계좌로 기존 대출부터 갚아야 합니다."
        DemoScenario.WARN -> "보낸 링크에서 주문을 확인하고 이 계좌로 입금해 주세요."
    }
    fun events(scenario: DemoScenario, now: Long): List<RiskEvent> {
        val target = recipient(scenario).id
        fun e(type: RiskType, minutesAgo: Int, summary: String): RiskEvent {
            val createdAt = now - minutesAgo * 60_000L
            return RiskEvent(
                "fixture-${scenario.name}-${type.name}-$now", type, createdAt,
                createdAt + EVENT_TTL, RiskSource.DEMO_FIXTURE, summary, target
            )
        }
        return when (scenario) {
            DemoScenario.NORMAL -> emptyList()
            DemoScenario.IMPERSONATION, DemoScenario.EASY -> listOf(
                e(RiskType.IMPERSONATION, 6, "검찰 관계자라고 소개"),
                e(RiskType.SUSPICIOUS_LINK, 5, "보안 확인용 링크 전달"),
                e(RiskType.URGENCY, 2, "지금 바로 이체 요청"),
                e(RiskType.FINANCIAL_INSTRUCTION, 2, "안전계좌로 자금 이동 요구")
            )
            DemoScenario.LOAN, DemoScenario.UNKNOWN -> listOf(e(RiskType.FINANCIAL_INSTRUCTION, 2, "대출을 바꾸기 위한 선상환 요구"))
            DemoScenario.WARN -> listOf(
                e(RiskType.SUSPICIOUS_LINK, 3, "상대가 보낸 주문 확인 링크"),
                e(RiskType.FINANCIAL_INSTRUCTION, 1, "링크를 확인한 뒤 입금 요청")
            )
        }
    }
    fun initial(scenario: DemoScenario = DemoScenario.NORMAL, now: Long = epoch): BankState = BankState(
        scenario = scenario, scenarioStartedAt = now, simulatedNow = now, updatedAt = now,
        events = events(scenario, now), receipts = seedHistory(now)
    )
    private fun seedHistory(now: Long): List<TransferReceipt> = listOf(
        TransferReceipt("SIM-OPEN-03", "opening-coffee", Recipient("cafe", "모퉁이 카페", "새온은행", "가상 카드 결제"), 5_500, now - 30 * 60_000, Purpose.PURCHASE, START_BALANCE, "seed-03", memo = "체크카드", seed = true),
        TransferReceipt("SIM-OPEN-02", "opening-payroll", Recipient("salary", "새온스튜디오", "새온은행", "급여 입금"), 2_840_000, now - 22 * 3_600_000, Purpose.OTHER, START_BALANCE + 5_500, "seed-02", Direction.CREDIT, memo = "9월 급여", seed = true),
        TransferReceipt("SIM-OPEN-01", "opening-seoyeon", seoyeon, 32_000, now - 2 * 86_400_000, Purpose.SETTLEMENT, START_BALANCE + 5_500 - 2_840_000, "seed-01", memo = "저녁 정산", seed = true)
    )
}
