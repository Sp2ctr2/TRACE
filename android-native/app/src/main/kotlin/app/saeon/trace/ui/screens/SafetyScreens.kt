package app.saeon.trace.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

@Composable fun SafetyScreen(state: BankState, model: BankViewModel, open: (String) -> Unit) {
    val now = model.repository.clock.now()
    val active = state.events.filter { it.active(now) }.sortedByDescending { it.createdAt }
    Page(title = "안전 센터", tag = "safety_center") {
        Space(12); TraceSignature(); Space(22)
        Headline(if (state.pending.isEmpty()) "현재 확인이 필요한\n송금이 없습니다." else "송금 ${state.pending.size}건이\n확인을 기다리고 있어요.")
        Space(14); Body(if (state.pending.isEmpty()) "평소에는 조용하게, 위험할 땐 분명하게." else "아직 돈은 나가지 않았습니다. 이유와 확인할 경로를 정리해 두었어요.", subdued = true)
        if (state.pending.isNotEmpty()) {
            Space(24)
            state.pending.take(2).forEach { record ->
                MenuRow("${record.intent.recipient.name} · ${won(record.intent.amount)}원", stageLabel(record.stage), BankIcons.Pause,
                    tag = "pending_${record.intent.id}") { model.resume(record.intent.id) { open("transfer_state") } }
            }
            QuietButton("보류 내역 모두 보기") { open("pending") }
        }
        Space(24); Rule(); Space(20)
        SectionTitle("최근 맥락", if (state.events.isNotEmpty()) "전체 흐름" else null, if (state.events.isNotEmpty()) ({ open("timeline") }) else null)
        if (active.isEmpty()) {
            Caption("현재 거래와 연결할 위험 신호가 없습니다.")
        } else active.take(3).sortedBy { it.createdAt }.forEach { event ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Caption(timeLabel(event.createdAt), Modifier.widthIn(min = 42.dp))
                Column(Modifier.weight(1f)) { Text(event.type.label, style = MaterialTheme.typography.bodyLarge); Space(4); Caption(event.summary) }
            }
        }
        Space(26); Rule(); Space(16)
        MenuRow("받은 내용을 직접 확인하기", "메시지를 붙여 넣거나 말로 입력해요.", BankIcons.Message, tag = "manual_check_open") { open("manual") }
        Rule()
        MenuRow("데이터 경계", "원문 대신 필요한 위험 신호만 남깁니다.", BankIcons.Lock) { open("privacy") }
        Rule()
        MenuRow("안전하게 확인하는 방법", "상대가 준 경로와 확인할 경로는 달라야 해요.", BankIcons.Shield) { open("safety_guide") }
        Rule()
        MenuRow("쉬운 모드", "큰 글자와 하나씩 확인하는 안내", BankIcons.Settings) { open("accessibility") }
        Space(24); Caption("TRACE는 경찰이나 은행 상담원을 대신하지 않습니다. 이 앱은 로컬 규칙과 가상 정책으로 보호 흐름을 시연합니다.")
    }
}

fun stageLabel(stage: TransferStage): String = when (stage) {
    TransferStage.HOLD -> "송금 보류 · 공식 경로로 확인 필요"
    TransferStage.VERIFY -> "상환 경로 확인 필요"
    TransferStage.UNKNOWN -> "공식 경로 확인 불가 · 송금하지 않음"
    TransferStage.WARN -> "보내기 전 확인 필요"
    TransferStage.ROUTE -> "공식 경로 확인 · 새 거래 검토 필요"
    TransferStage.REVIEW -> "송금 내역 확인 중"
    TransferStage.AUTHORIZING -> "거래 인증 중"
    TransferStage.EVALUATING -> "위험 맥락 확인 중"
    TransferStage.COMPLETE -> "송금 완료"
    TransferStage.CANCELLED -> "취소한 송금"
    TransferStage.SUPERSEDED -> "새 거래로 변경됨"
}

@Composable fun PendingScreen(state: BankState, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    Page(title = "보류된 송금", tag = "pending_history", back = back) {
        if (state.pending.isEmpty()) EmptyState("보류된 송금이 없어요.", "완료된 송금은 거래 내역에서 볼 수 있어요.")
        state.pending.asReversed().forEach { record ->
            MenuRow("${record.intent.recipient.name} · ${won(record.intent.amount)}원",
                "${dateLabel(record.intent.createdAt)}\n${stageLabel(record.stage)}", tag = "pending_row_${record.intent.id}") {
                model.resume(record.intent.id) { open("transfer_state") }
            }
            Rule()
        }
        Space(24); Caption("앱을 닫거나 다시 열어도 보류된 송금은 자동으로 완료되지 않습니다.")
    }
}

private data class TimelineItem(val time: Long, val title: String, val description: String, val accent: Boolean = false)
@Composable fun TimelineScreen(state: BankState, back: () -> Unit) {
    val record = state.current ?: state.pending.lastOrNull()
    val relevant = state.events.filter { record == null || it.recipientId == null || it.recipientId == record.intent.recipient.id }
    val rows = relevant.sortedBy { it.createdAt }.map { TimelineItem(it.createdAt, it.type.label, it.summary) }.toMutableList()
    if (record != null) {
        if (!record.intent.recipient.known) rows += TimelineItem(record.intent.createdAt, "새로운 수취인", "${record.intent.recipient.name} · ${record.intent.recipient.bank}")
        rows += TimelineItem(record.intent.createdAt, "${won(record.intent.amount)}원 송금 시도", stageLabel(record.stage),
            record.stage in setOf(TransferStage.HOLD, TransferStage.VERIFY, TransferStage.UNKNOWN))
    }
    Page(title = "위험 신호의 흐름", tag = "trace_timeline", back = back) {
        Space(12); Headline("짧은 시간 안에\n이어진 위험 신호"); Space(16)
        Body("각 사건만 보면 평범할 수 있지만, 같은 흐름에서 이어졌습니다.", subdued = true)
        Space(32)
        if (rows.isEmpty()) EmptyState("연결된 흐름이 아직 없어요.", "직접 확인한 위험 신호와 송금 시도가 생기면 시간순으로 보여드려요.")
        rows.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.width(12.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.padding(top = 7.dp).size(5.dp).background(if (item.accent) TraceColors.Deep else TraceColors.Divider))
                    if (index < rows.lastIndex) Box(Modifier.width(1.dp).weight(1f).background(TraceColors.Divider))
                }
                Column(Modifier.weight(1f).padding(bottom = 28.dp)) {
                    Caption(timeLabel(item.time)); Space(6)
                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = if (item.accent) TraceColors.CoralText else TraceColors.Ink)
                    Space(6); Body(item.description, subdued = true)
                }
            }
        }
        Space(16); Rule(); Space(20)
        Caption("원문 대신 필요한 위험 신호만 남깁니다.")
        Space(10); Caption("위험 신호는 15분 동안 현재 거래에 연결됩니다. 시간이 지나도 이미 보류한 거래를 자동으로 풀지는 않아요.")
    }
}

@Composable fun SafetyGuideScreen(state: BankState, open: (String) -> Unit, back: () -> Unit) {
    val context = LocalContext.current
    val record = state.current?.takeIf { it.stage in setOf(TransferStage.HOLD, TransferStage.VERIFY, TransferStage.UNKNOWN, TransferStage.WARN, TransferStage.ROUTE) }
    Page(title = "안전하게 확인하기", tag = "trace_safety_guide", back = back, footer = {
        PrimaryButton("앱 안에서 공식 경로 열기", Modifier.testTag("safety_official_channel"), accent = record != null) { open(if (record?.intent?.purpose == Purpose.LOAN) "loan" else "support") }
    }) {
        Space(10); Headline("지금은 이렇게\n확인하세요."); Space(18)
        if (record != null) {
            Text("아직 돈은 나가지 않았습니다.", style = MaterialTheme.typography.titleMedium)
            Space(12); Caption("${record.intent.recipient.name} · ${won(record.intent.amount)}원 · ${stageLabel(record.stage)}")
        } else Body("서두르라는 요청보다, 독립된 확인이 먼저입니다.", subdued = true)
        Space(26)
        NumberedReason(1, "상대가 준 번호나 링크는 사용하지 마세요.", "상대가 만든 확인 경로로는 사실 여부를 확인할 수 없어요.", accent = true)
        Rule()
        NumberedReason(2, "은행 앱에서 직접 공식 경로를 여세요.", "고객센터나 대출 상환 메뉴처럼, 원래 알고 있던 경로에서 확인하세요.")
        Rule()
        NumberedReason(3, "확인이 끝나기 전에는 보내지 마세요.", "보류된 송금은 이 화면을 닫아도 자동으로 실행되지 않습니다.")
        Rule()
        NumberedReason(4, "필요하면 전화를 끊고 상황을 나누세요.", "가족이나 신뢰하는 사람에게, 어떤 요청을 받았는지 직접 설명해 주세요.")
        Space(18)
        QuietButton("확인할 내용을 다른 사람과 공유") {
            val text = "송금 요청을 독립된 경로로 확인하려고 합니다. 상대가 준 번호나 링크가 아닌, 원래 알고 있던 연락처로 함께 확인해 주세요.\n새온은행 × TRACE 시연 안내 · 실제 자금 이동 없음"
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "안전 확인 안내 공유"))
        }
        Space(12); Caption("새온은행은 가상 은행입니다. 실제 기관에 연결되거나 경찰이 확인했다는 의미가 아닙니다.")
    }
}

@Composable fun PrivacyScreen(state: BankState, model: BankViewModel, back: () -> Unit) {
    val now = model.repository.clock.now()
    val expiredCount = state.events.count { !it.active(now) && it.expiresAt <= now }
    val packet = state.current?.attestation
    Page(title = "개인정보와 데이터 경계", tag = "privacy_boundary", back = back) {
        Space(10); TraceSignature(); Space(24); Headline("지키기 위해,\n가져가지 않습니다.")
        Space(16); Body("공유한 원문은 기기 안에서만 읽어요. 은행 정책에는 필요한 위험 신호와 거래 연결값만 사용합니다.", subdued = true)
        Space(30); Rule(); Space(20)
        Text("이 기기 안에서", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Space(16); DetailRow("입력", "사용자가 동의한 공유 내용")
        DetailRow("처리", "로컬 규칙으로 위험 표현 추출")
        DetailRow("남는 정보", "종류·시간·출처·유효기간")
        DetailRow("원문 저장", "하지 않음", true)
        Space(24); Rule(); Space(20)
        Text("은행 정책 모듈로", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Space(16); DetailRow("위험 신호", "reasonCodes")
        DetailRow("신선도", "issuedAt · expiresAt")
        DetailRow("거래 연결", "금액·수취인·목적의 해시")
        DetailRow("서명", "Keystore · EC P-256")
        DetailRow("원문 전달", "rawContentExported = false", true)
        Space(18); Caption("이번 앱은 은행 정책 모듈도 기기 안에서 실행합니다. 실제 은행이나 외부 서버로 전송하지 않아요.")
        if (packet != null) {
            Space(28); Rule(); Space(20); SectionTitle("이 거래의 확인 기록")
            DetailRow("판단", packet.decision.name)
            DetailRow("거래 연결값", packet.binding.take(16) + "…")
            DetailRow("원문 포함", if (packet.rawContentExported) "포함됨 · 오류" else "포함하지 않음")
            DetailRow("서명 생성", dateLabel(packet.issuedAt))
        }
        Space(28); Rule(); Space(20); SectionTitle("기기에 남는 기록")
        Body("위험 신호는 15분 동안 현재 거래에 사용합니다. 보류 판단과 가상 영수증은 앱을 다시 열어도 유지해요.", subdued = true)
        Space(16)
        if (expiredCount > 0) QuietButton("만료된 신호 ${expiredCount}개 정리") { model.act { model.repository.clearExpiredSignals() } }
        else Caption("정리할 만료 신호가 없어요.")
        Space(16); Caption("SMS, 연락처, 통화 기록을 수집하지 않습니다. 음성 입력은 버튼을 눌렀을 때만 기기 내 음성 인식을 사용합니다.")
    }
}

@Composable fun ManualCheckScreen(model: SafetyViewModel, open: (String) -> Unit, back: () -> Unit,
                                  onVoice: () -> Unit, onStopVoice: () -> Unit, voiceActive: Boolean) {
    BackHandler(onBack = back)
    val input by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val result = input.result
    if (result != null) {
        Page(title = "내용 확인", tag = "manual_result", back = back, footer = {
            PrimaryButton("송금 화면으로") { open("transfer") }
            SecondaryButton("다른 내용 확인") { model.clear() }
        }) {
            Space(14); TraceSignature(); Space(24)
            Headline(if (result.isEmpty()) "뚜렷한 위험 신호를\n찾지 못했어요." else "이런 표현을\n함께 확인해 주세요.")
            Space(18); Body("내용만으로 사기인지 단정하지 않습니다. 실제 송금의 목적과 받는 곳, 시간 흐름을 함께 봐야 해요.", subdued = true)
            Space(26)
            if (result.isEmpty()) {
                SurfaceBox { Body("안전하다고 확인된 것은 아닙니다."); Space(10); Caption("다른 경로로 상대와 송금 목적을 확인해 주세요.") }
            } else {
                SectionTitle("가능한 위험 신호")
                result.forEachIndexed { index, event -> NumberedReason(index + 1, event.type.label, event.type.explanation); Rule() }
            }
            Space(24); Caption("입력 원문은 저장하지 않았어요. 구조화된 신호만 안전 센터에서 볼 수 있습니다.")
            MenuRow("안전 센터에서 흐름 보기", icon = BankIcons.History) { open("safety") }
        }
        return
    }
    Page(title = if (input.shared) "공유한 내용 확인" else "직접 확인하기", tag = "shared_text_review", back = back, footer = {
        PrimaryButton(if (input.busy) "내용을 확인하고 있어요." else "TRACE로 확인", Modifier.testTag("share_consent"),
            enabled = input.text.isNotBlank() && input.text.length <= SignalExtractor.MAX_INPUT && !input.busy && !voiceActive) { model.analyze() }
        SecondaryButton("취소", Modifier.testTag("share_cancel"), onClick = back)
    }) {
        Space(10); Headline(if (input.shared) "이 내용을\n확인할까요?" else "받은 내용을\n직접 확인해요.")
        Space(14); Body("확인을 누르기 전에는 분석하지 않습니다. 필요한 내용만 남기고 확인해 주세요.", subdued = true)
        Space(24)
        Field(input.text, "확인할 내용", model::edit, Modifier.testTag("shared_text_input"), minLines = 5, maxLines = 10, enabled = !input.busy && !voiceActive)
        Space(6); Caption("${input.text.length} / ${SignalExtractor.MAX_INPUT}자")
        input.error?.let { ErrorNote(it) }
        Space(14)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuietButton("붙여넣기", Modifier.weight(1f), enabled = !voiceActive) {
                val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).text?.toString().orEmpty() else ""
                if (text.isBlank()) model.showError("클립보드에 확인할 텍스트가 없어요. 내용을 직접 입력해 주세요.") else model.edit(text)
            }
            QuietButton(if (voiceActive) "음성 입력 중지" else "말로 내용 입력", Modifier.weight(1f)) { if (voiceActive) onStopVoice() else onVoice() }
        }
        if (voiceActive) {
            Space(12); Body("마이크가 켜져 있습니다.")
            Caption("입력을 멈추려면 ‘음성 입력 중지’를 눌러 주세요. 받아쓴 내용은 확인을 눌러야 분석합니다.")
        }
        Space(24); Rule(); Space(18)
        Caption("원문은 외부로 보내거나 영구 저장하지 않습니다. 앱의 공유 메뉴로 들어온 내용도 동의한 뒤에만 확인해요.")
        Space(12); Caption("기기 내 규칙이 표현을 분류합니다. 모든 사기나 우회 표현을 탐지하는 서비스는 아닙니다.")
    }
}
