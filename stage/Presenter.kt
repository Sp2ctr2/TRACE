package app.saeon.trace.ui

import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.BuildConfig
import app.saeon.trace.core.*
import app.saeon.trace.ui.design.*
import kotlinx.coroutines.delay

/** Monotonic clock; a tick never starts a transaction or advances a bank state. */
data class RehearsalClock(val budget:Int=180,val offset:Int=0,val accumulated:Long=0,val started:Long?=null) {
    fun elapsed(now:Long):Long=accumulated+(started?.let{(now-it).coerceAtLeast(0)}?:0)
    fun start(now:Long)=if(started!=null)this else copy(started=now)
    fun pause(now:Long)=copy(accumulated=elapsed(now),started=null)
    fun remaining(now:Long):Int=budget-(elapsed(now)/1000).toInt()
    fun scriptSecond(now:Long):Int=offset+(elapsed(now)/1000).toInt()
}
data class StageNote(val second:Int,val scene:String,val outcome:String,val amount:Long)
data class StageState(val unlocked:Boolean=false,val clock:RehearsalClock=RehearsalClock(),
    val selected:Int=0,val scene:String="",val notes:List<StageNote> = emptyList())
data class StageCue(val start:Int,val end:Int,val title:String,val line:String,val action:String?=null)
val StageCues=listOf(
    StageCue(0,17,"정상 인증","로그인과 본인인증은 모두 정상이었습니다."),
    StageCue(17,31,"300만 원의 손실","사용자가 직접 눌렀지만, 그 결정을 만든 것은 다른 사람이었습니다."),
    StageCue(31,45,"비어 있는 맥락","은행이 보기 어려웠던 건 왜 지금 보내는가였습니다."),
    StageCue(45,60,"사기는 과정","기관 사칭, 링크, 압박, 그리고 새로운 수취인."),
    StageCue(60,75,"TRACE 소개","은행은 거래를 봅니다. TRACE는 그 거래가 만들어진 과정을 봅니다."),
    StageCue(75,91,"정상 32,000원","정상 송금에서는 추가 확인을 요구하지 않습니다.","normal"),
    StageCue(91,112,"사건의 흐름","아직 막지 않습니다. 먼저 서로 떨어진 단서를 연결합니다.","context"),
    StageCue(112,130,"300만 원 앞에서","그리고 이 요청이 실제 송금과 만나는 순간에 개입합니다.","hold"),
    StageCue(130,148,"왜 멈췄는가","큰 금액이어서가 아니라, 앞선 요청이 이 송금으로 이어졌기 때문입니다.","comparison"),
    StageCue(148,163,"기기 안의 처리","이번 앱은 로컬 시연 정책으로 작동합니다. 실제 모델 정확도와는 구분합니다.","local"),
    StageCue(163,175,"은행 안으로","기존 은행의 판단을 바꾸는 대신, 거래 전의 맥락을 더합니다."),
    StageCue(175,180,"마무리","돈이 움직이기 전에, 판단을 지킵니다.")
)
fun stageTime(seconds:Int):String=(if(seconds<0)"+"else"")+"${kotlin.math.abs(seconds)/60}:${(kotlin.math.abs(seconds)%60).toString().padStart(2,'0')}"

@Composable fun PresenterScreen(model:BankViewModel,open:(String)->Unit) {
    val stage by model.stage.collectAsStateWithLifecycle()
    val bank by model.bank.collectAsStateWithLifecycle()
    val busy by model.interaction.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var tab by rememberSaveable{mutableIntStateOf(0)}
    var now by remember{mutableLongStateOf(SystemClock.elapsedRealtime())}
    var confirmReset by remember{mutableStateOf(false)}
    var confirmLock by remember{mutableStateOf(false)}
    LaunchedEffect(stage.clock.started){while(true){now=SystemClock.elapsedRealtime();delay(250)}}
    if(!stage.unlocked){Page(title="발표자 모드",tag="presenter_locked"){Body("앱 정보에서 발표자 모드를 활성화해 주세요.")};return}
    val current=StageCues.lastOrNull{it.start<=stage.clock.scriptSecond(now)}?:StageCues.first()
    val selected=StageCues[stage.selected]
    TaskPage("발표자 모드","presenter",root=true,actions={IconAction(BankIcons.More,"전체 메뉴"){open("more")}}) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("본편","질의응답","준비").forEachIndexed{i,label->
                FilterChip(tab==i,{tab=i},label={Text(label)},modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("stage_tab_$i"))
            }
        }
        if(tab==0) {
            TaskGap(12)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Caption(if(stage.clock.budget==180)"3분 발표 · 남은 시간"else"시연만 연습 · 남은 시간")
                    Text(stageTime(stage.clock.remaining(now)),fontSize=42.sp,fontWeight=FontWeight.SemiBold,
                        style=MaterialTheme.typography.displayLarge.copy(fontFeatureSettings="tnum"),modifier=Modifier.testTag("stage_clock"))
                }
                QuietButton(if(stage.clock.started==null)"시작"else"일시정지",Modifier.testTag("stage_timer_toggle")){model.stageTimerToggle()}
            }
            LinearProgressIndicator(progress={(stage.clock.elapsed(now).toFloat()/(stage.clock.budget*1000)).coerceIn(0f,1f)},
                modifier=Modifier.fillMaxWidth().height(3.dp),color=TraceColors.Coral,trackColor=TraceColors.Divider)
            TaskGap(12)
            Caption("시간표 기준: ${current.title}")
            TaskGap(14)
            SurfaceBox {
                Caption("${stageTime(selected.start)}–${stageTime(selected.end)}")
                Text(selected.title,style=MaterialTheme.typography.headlineSmall)
                TaskGap(8);Body(selected.line,subdued=true)
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                QuietButton("이전",Modifier.weight(1f),enabled=stage.selected>0){model.stageSelect(stage.selected-1)}
                Text("${stage.selected+1} / ${StageCues.size}",style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
                QuietButton("다음",Modifier.weight(1f),enabled=stage.selected<StageCues.lastIndex){model.stageSelect(stage.selected+1)}
            }
            if(selected.action!=null)PrimaryButton(when(selected.action){"normal"->"32,000원 송금 준비";"context"->"사건 흐름 열기";"hold"->"300만 원 송금 준비";"comparison"->"같은 거래 비교";else->"로컬 실행 정보"},
                Modifier.testTag("stage_launch"),enabled=!busy.busy){
                when(selected.action){"normal"->model.stageScenario(DemoScenario.NORMAL){open("transfer_state")};"context"->model.stageContext{open("stage_context")};"hold"->model.stageScenario(DemoScenario.IMPERSONATION){open("transfer_state")};"comparison"->open("comparison");else->tab=2}
            }
            TaskGap(8);Caption("장면은 수동으로 넘깁니다. 시간이 지나도 송금하지 않아요.")
        } else if(tab==1) {
            TaskGap(12);TaskHeadline("질문에는,\n직접 보여주세요")
            MenuRow("같은 계좌·같은 300만 원","달라지는 것은 거래 앞의 맥락",BankIcons.Transfer,tag="stage_comparison"){open("comparison")}
            MenuRow("대출 상환 경로","개인 계좌에서 공식 경로로 새 거래",BankIcons.Bank,tag="stage_loan"){model.stageScenario(DemoScenario.LOAN){open("transfer_state")}}
            MenuRow("확인하지 못했을 때","UNKNOWN은 자동 승인하지 않음",BankIcons.Pause){model.stageScenario(DemoScenario.UNKNOWN){open("transfer_state")}}
            MenuRow("정상 문맥과 만료된 신호","만료 사례이며 언어 모델 평가가 아님",BankIcons.Info){model.stageScenario(DemoScenario.EXPIRED){open("transfer_state")}}
            MenuRow("모든 시나리오","12개 입력 · 실제 로컬 정책 실행",BankIcons.More,tag="stage_all_cases"){open("demo_lab")}
        } else {
            TaskGap(12);TaskHeadline("무대에 오르기 전")
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                QuietButton("3분 본편",Modifier.weight(1f).testTag("stage_preset_full")){model.stagePreset(180)}
                QuietButton("55초 실연",Modifier.weight(1f).testTag("stage_preset_demo")){model.stagePreset(55)}
            }
            TaskDetail("기본 발표","180초 / 실연 75–130초")
            TaskDetail("앱 버전",BuildConfig.VERSION_NAME)
            val granted=runCatching{context.packageManager.getPackageInfo(context.packageName,android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions?.contains("android.permission.INTERNET")==true}.getOrDefault(true)
            TaskDetail("인터넷 권한",if(granted)"요청됨 · 연결 확인 필요"else"없음 · 기기 내부 시연")
            TaskDetail("판단 엔진","로컬 시연 정책")
            TaskDetail("실제 학습 가중치","미연결")
            TaskDetail("원문 수집","자동 통화·문자 수집 없음")
            TaskGap(8);Caption("기기 화면 미러링·알림 방해금지·밝기는 현장에서 직접 확인하세요. 준비 버튼은 시연 잔액을 초기화하지만 인증이나 판단을 건너뛰지 않습니다.")
            QuietButton("연습 기록 공유",Modifier.fillMaxWidth().testTag("stage_export")) {
                val text="TRACE 연습 기록 / ${BuildConfig.VERSION_NAME}\n시연 규칙이며 탐지율 측정이 아님\n"+stage.notes.joinToString("\n"){"${stageTime(it.second)} ${it.scene}: ${it.outcome} / ${won(it.amount)}원"}
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"연습 기록 공유"))
            }
            QuietButton("타이머·연습 기록 초기화",Modifier.fillMaxWidth()){confirmReset=true}
            QuietButton("발표자 모드 숨기기",Modifier.fillMaxWidth().testTag("stage_lock")){confirmLock=true}
        }
    }
    if(confirmReset)AlertDialog(onDismissRequest={confirmReset=false},title={Text("연습 기록을 초기화할까요?")},text={Text("은행 거래 내역은 그대로 유지합니다.")},confirmButton={QuietButton("초기화"){model.stagePreset(stage.clock.budget);confirmReset=false}},dismissButton={QuietButton("취소"){confirmReset=false}})
    if(confirmLock)AlertDialog(onDismissRequest={confirmLock=false},title={Text("발표 탭을 숨길까요?")},text={Text("타이머가 멈추고 하단의 전체 메뉴로 돌아갑니다. 가상 은행의 거래 기록은 유지합니다.")},confirmButton={QuietButton("숨기기",Modifier.testTag("stage_lock_confirm")){model.stageLock();open("home")}},dismissButton={QuietButton("취소"){confirmLock=false}})
}

@Composable fun StageContextScreen(model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val bank by model.bank.collectAsStateWithLifecycle()
    val state=bank?:return
    val nodes=state.context.events.sortedBy{it.createdAt}.distinctBy{it.type}.take(3).map{ThreadNode(timeLabel(it.createdAt),it.type.label)}+
        ThreadNode(timeLabel(model.repository.clock.now()),"처음 보는 계좌로 300만 원")
    TaskPage("사건의 흐름","stage_context",back=back,footer={PrimaryButton("송금 직전으로",Modifier.testTag("stage_context_next")){model.stageScenario(DemoScenario.IMPERSONATION){open("transfer_state")}}}) {
        TaskGap(24);TraceSignature();TaskGap(24)
        TaskHeadline("아직 막지 않습니다.\n먼저, 연결합니다.")
        TaskGap(24);ContextThread(nodes,"stage_context")
        TaskGap(24);Body("같은 사람에게 이어진 요청이\n실제 금융 행동과 만납니다.",subdued=true)
        TaskGap(16);Caption("준비된 가상 사건의 입력입니다.\n실제 통화나 문자를 수집한 화면이 아닙니다.")
    }
}
