package dev.sp2ctr2.saeon.domain

import java.util.UUID

/** Transparent on-device RULE analyzer. Not an ML model and not a URL scanner. */
object LocalRiskAnalyzer {
    const val MAX_INPUT = 8_000
    fun analyze(raw: String, now: Long): List<RiskEvent> {
        require(raw.length <= MAX_INPUT) { "내용은 8,000자 이내로 입력해 주세요." }
        val text = raw.trim().lowercase()
        if(text.isBlank()) return emptyList()
        val educational = listOf("예방 교육", "사기 예방", "뉴스 기사", "송금하지 마", "보내지 마").any { it in text }
        if(educational) return emptyList()
        val institution = listOf("검찰", "경찰", "금감원", "금융감독원", "수사관").any { it in text }
        val moveMoney = listOf("송금", "이체", "보내", "입금", "옮기", "갚아").any { it in text }
        val safeAccount = "안전계좌" in text || "안전 계좌" in text
        val urgent = listOf("지금", "즉시", "바로", "당장", "오늘까지").any { it in text }
        val loan = ("대출" in text || "저금리" in text) && moveMoney
        val result = mutableListOf<Pair<RiskType,String>>()
        if((institution || safeAccount) && moveMoney) result += RiskType.IMPERSONATION to "기관·안전계좌를 언급하며 자금 이동을 요청한 정황"
        if(urgent && moveMoney) result += RiskType.URGENCY to "즉시 금융 행동을 요구한 표현"
        if(moveMoney) result += RiskType.FINANCIAL_INSTRUCTION to "공유한 내용에 자금 이동 요청이 포함됨"
        if(Regex("https?://|www\\.").containsMatchIn(text)) result += RiskType.SUSPICIOUS_LINK to "공유된 내용에 미확인 링크가 포함됨 · 접속 여부는 수집하지 않음"
        if(loan) result += RiskType.PURPOSE_MISMATCH to "대출 상환 목적과 받는 계좌의 독립 확인이 필요함"
        return result.map { (type, summary) -> RiskEvent(UUID.randomUUID().toString(), type, now, now + 15*60_000, EventSource.USER_SHARED, summary) }
    }
}
