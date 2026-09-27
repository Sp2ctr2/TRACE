@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.saeon.trace.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

@Composable fun BankingHome(state:BankState,preferences:BankPreferences,model:BankViewModel,open:(String)->Unit) {
    TaskPage("새온은행","home",root=true,actions={IconAction(BankIcons.Bell,"알림"){open("notifications")};IconAction(BankIcons.Profile,"내 정보"){open("profile")}}) {
        val compact=LocalTaskHeight.current<465.dp
        val amountSize=if(compact)34 else 40
        Column(Modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(24.dp)).padding(horizontal=18.dp,vertical=if(compact)10.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(role=Role.Button){open("account")},verticalArrangement=Arrangement.Center) {
                    Text("새온 생활통장",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
                    Caption("110-***-0001")
                }
                IconAction(BankIcons.Eye,if(preferences.hideBalance)"잔액 보이기"else"잔액 숨기기",Modifier.testTag("home_balance_toggle")){model.preference{hideBalance(!preferences.hideBalance)}}
            }
            if(preferences.hideBalance)Text("잔액 숨김",style=MaterialTheme.typography.displaySmall,modifier=Modifier.heightIn(min=48.dp))
            else BankAmount(state.balance,Modifier.testTag("home_balance"),amountSize)
            Space(if(compact)8 else 16)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1.5f)){BankAction("송금",Modifier.testTag("home_transfer")){open("transfer")}}
                Box(Modifier.weight(1f)){OutlinedButton(onClick={open("bring")},modifier=Modifier.fillMaxWidth().heightIn(min=54.dp).testTag("home_bring"),
                    shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,TraceColors.Divider),colors=ButtonDefaults.outlinedButtonColors(contentColor=TraceColors.Ink)){
                    Text("가져오기",style=MaterialTheme.typography.labelLarge)}}
            }
        }
        Space(if(compact)10 else 18)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
            listOf(Triple("이번 달 쓴 돈","382,400원","spending"),Triple("모아둔 돈","${won(state.savings)}원","savings")).forEach{(title,value,route)->
                Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(role=Role.Button){open(route)},verticalArrangement=Arrangement.Center){
                    Caption(title);Text(value,style=MaterialTheme.typography.titleMedium.copy(fontSize=if(compact)16.sp else 19.sp),fontWeight=FontWeight.SemiBold)
                }
            }
        }
        Space(if(compact)6 else 14);Rule()
        SectionHeading("최근 거래",if(compact)"전체"else"전체 내역"){open("history")}
        state.receipts.take(if(compact)1 else 2).forEach{r->
            Row(Modifier.fillMaxWidth().heightIn(min=if(compact)48.dp else 58.dp).clickable(role=Role.Button){open("receipt/${r.id}")},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                if(!compact)Box(Modifier.size(36.dp).background(TraceColors.Surface,CircleShape),contentAlignment=Alignment.Center){AppIcon(if(r.direction==Direction.CREDIT)BankIcons.Download else BankIcons.Card,size=18,tint=TraceColors.Muted)}
                Column(Modifier.weight(1f)){Text(r.recipient.name,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium);if(!compact)Caption(r.memo.ifBlank{r.purpose.label})}
                Text("${if(r.direction==Direction.DEBIT)"−"else"+"}${won(r.amount)}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
            }
        }
        if(!compact&&state.recurringEnabled)BankRow("예정된 출금",value="통신비 68,000원",modifier=Modifier.testTag("home_upcoming")){open("recurring")}
        Space(if(compact)4 else 8)
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(role=Role.Button){open("safety")}.testTag("home_trace"),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            AppIcon(BankIcons.Trace,size=23,tint=TraceColors.Coral)
            Text(if(state.pending.isNotEmpty())"보류한 송금 ${state.pending.size}건"else"보내기 전, 한 번 더.",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
            AppIcon(BankIcons.Chevron,size=14,tint=TraceColors.Muted)
        }
    }
}

@Composable fun BankingAssets(state:BankState,open:(String)->Unit) {
    Page(title="자산",tag="assets") {
        Caption("입출금과 저축");Space(8);BankAmount(state.balance+state.savings,size=38);Space(8)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Caption("대출을 뺀 순자산");Text("${won(state.balance+state.savings-state.loanBalance)}원",style=MaterialTheme.typography.bodyMedium)}
        Space(26);SectionHeading("내 계좌")
        QuietSurface(padding=16){
            BankRow("생활통장","언제든 꺼내 쓰는 돈","${won(state.balance)}원",BankIcons.Bank,Modifier.testTag("asset_primary_account")){open("account")}
            Rule();BankRow("모아적금","저축 목표 300만 원","${won(state.savings)}원",BankIcons.Assets){open("savings")}
        }
        Space(20);SectionHeading("카드와 대출")
        BankRow("새온 체크카드","이번 달 사용","382,400원",BankIcons.Card){open("card")}
        BankRow("생활안심대출","남은 원금","${won(state.loanBalance)}원",BankIcons.Bank){open("loan")}
        Space(16);SectionHeading("함께 살펴보기")
        BankRow("투자 모아보기","가상 포트폴리오와 관심 목록",icon=BankIcons.Assets){open("investments")}
        BankRow("내 신용 관리","예시 점수와 변동 알림",icon=BankIcons.Info){open("credit")}
        BankRow("보장 내역","가상 보험 계약 상세",icon=BankIcons.Shield){open("insurance")}
        Space(16);Caption("투자·신용·보험은 설명용 별도 데이터입니다. 위 계좌 잔액 합계에 포함하지 않습니다.")
    }
}

@Composable fun BankingReview(state:BankState,record:TransferRecord,interaction:InteractionState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val intent=record.intent
    val amountError=runCatching{BankEngine.validateAmount(state,intent.amount,intent.purpose,model.repository.clock.now())}.exceptionOrNull()?.message
    TaskPage("송금 확인","transfer_review",back=back,footer={
        Row(Modifier.fillMaxWidth().padding(bottom=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
            Icon(BankIcons.Trace,null,Modifier.size(18.dp).traceShared("trace-mark"),tint=TraceColors.Coral)
            Caption("보내기 전 맥락까지 확인해요",Modifier.padding(start=5.dp))
        }
        BankAction("${won(intent.amount)}원 보내기",Modifier.testTag("transfer_confirm"),enabled=amountError==null&&!interaction.busy&&record.stage==TransferStage.REVIEW){model.requestAuthorization(intent.id)}
    }) {
        val compact=LocalTaskHeight.current<460.dp
        Space(if(compact)0 else 16)
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
            if(!compact){PersonBadge(intent.recipient.name,48,true);Space(14)}
            RecipientTitle(intent.recipient.id,intent.recipient.name)
            Space(5);BankAmount(intent.amount,Modifier.traceShared("amount/${intent.recipient.id}/${intent.amount}"),size=38,center=true)
            Space(2);Text("보낼까요?",style=MaterialTheme.typography.titleLarge.copy(fontSize=24.sp,letterSpacing=(-.5).sp))
        }
        Space(if(compact)16 else 30)
        QuietSurface(padding=16){
            BankingDetail("받는 계좌",intent.recipient.bank,intent.recipient.account)
            Rule();BankingDetail("출금 계좌","새온 생활통장")
            Rule();BankingDetail("목적 · 수수료","${intent.purpose.label} · 0원")
        }
        QuietButton("금액·목적 수정",Modifier.fillMaxWidth().testTag("review_edit"),enabled=record.stage==TransferStage.REVIEW&&!interaction.busy){model.editReview(intent.id){open("amount")}}
        if(intent.officialRouteId!=null)Caption("수취인이 바뀌어 새 거래로 인증합니다.")
        amountError?.let{ErrorNote(it)};record.error?.takeIf{it!=amountError}?.let{ErrorNote(it)}
    }
}
@Composable private fun BankingDetail(label:String,value:String,sub:String?=null) {
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(label,Modifier.widthIn(min=70.dp),style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
        Column(Modifier.weight(1f),horizontalAlignment=Alignment.End){
            Text(value,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium,textAlign=TextAlign.End)
            if(sub!=null)Text(sub,style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted,textAlign=TextAlign.End)
        }
    }
}

@Composable fun BankingMore(preferences:BankPreferences,open:(String)->Unit) {
    var search by rememberSaveable{mutableStateOf("")}
    val entries=listOf(
        Triple("내 정보","프로필과 주 거래 계좌","profile"),Triple("보안 및 인증","생체 인증·등록 기기","security"),
        Triple("송금 설정","한도·출금 계좌","transfer_settings"),Triple("자주 쓰는 계좌","즐겨찾기 관리","favorites"),
        Triple("자동이체","예정 내역·예약 관리","schedules"),Triple("알림","거래와 안전 확인","notifications"),
        Triple("증명서 발급","잔액·계좌 정보·거래 내역","certificates"),Triple("송금 확인증","완료한 송금 증빙","history"),
        Triple("카드 분실·재발급","가상 카드 보호","card_service"),Triple("계정 일시잠금","새 송금과 내 계좌 이동 중지","account_lock"),
        Triple("피해 신고","기기 내 상담 요청","report"),Triple("고객센터","문의·접수 내역","support"),
        Triple("화면 설정","라이트·다크·투명 효과","appearance"),Triple("접근성","큰 글자·동작 줄이기","accessibility"),
        Triple("개인정보","데이터 보관·삭제","privacy"),Triple("이용 안내와 약관","시연 범위·데이터 처리","terms"),
        Triple("도움말","송금과 TRACE 상태 안내","help"),Triple("앱 정보","버전·시연 환경","app_info"))
    Page("전체","settings") {
        BankRow("${preferences.displayName}님","새온은행 시연 계정",icon=BankIcons.Profile){open("profile")}
        Space(10);Field(search,"메뉴 검색",{search=it},Modifier.testTag("menu_search"));Space(18)
        val matches=entries.filter{search.isBlank()||it.first.contains(search)||it.second.contains(search)}
        if(matches.isEmpty())EmptyState("찾는 메뉴가 없어요.","송금, 카드, 증명서 등으로 다시 검색해 주세요.")
        matches.forEach{(title,sub,route)->BankRow(title,sub,modifier=Modifier.testTag("menu_$route")){open(route)}}
    }
}

private data class QuickDemo(val scenario:DemoScenario,val title:String,val description:String)
private val quickDemos=listOf(
    QuickDemo(DemoScenario.NORMAL,"정상 송금","친구에게 32,000원 정산"),
    QuickDemo(DemoScenario.IMPERSONATION,"위험 송금","기관 사칭 직후 300만 원"),
    QuickDemo(DemoScenario.LOAN,"대출 상환","개인 계좌로 선상환 요청"),
    QuickDemo(DemoScenario.WARN,"링크 유도","확인하지 않은 링크와 입금"),
    QuickDemo(DemoScenario.NEW_ACCOUNT,"정상 신규 계좌","직접 확인한 300만 원 거래"),
    QuickDemo(DemoScenario.FAMILY_FAKE,"가족 사칭","새 번호와 급한 송금 요청"),
    QuickDemo(DemoScenario.REMOTE,"원격 조작","기기 조작 중 송금 지시"),
    QuickDemo(DemoScenario.INVESTMENT,"투자금 요구","출금 전 보증금·비밀 유지"),
    QuickDemo(DemoScenario.UNKNOWN,"확인 불가","공식 상환 경로 조회 실패"),
    QuickDemo(DemoScenario.EDUCATION,"만료된 교육 신호","현재 송금에 적용하지 않음"),
    QuickDemo(DemoScenario.UNRELATED,"다른 거래의 링크","친구 정산과 연결하지 않음"),
    QuickDemo(DemoScenario.EASY,"큰 글자 보호","쉬운 모드의 송금 보류"))
@Composable fun BankingDemoCenter(model:BankViewModel,open:(String)->Unit) {
    val unlocked by model.stage.collectAsStateWithLifecycle()
    val interaction by model.interaction.collectAsStateWithLifecycle()
    var expanded by rememberSaveable{mutableStateOf(false)}
    var lock by remember{mutableStateOf(false)}
    if(!unlocked.unlocked){Page("시연센터","demo_center_locked"){Body("앱 정보에서 시연센터를 활성화해 주세요.")};return}
    Page("시연센터","demo_center",actions={IconAction(BankIcons.More,"전체 메뉴"){open("more")}}) {
        Space(6);Text("같은 송금,\n다른 맥락.",style=MaterialTheme.typography.headlineLarge.copy(letterSpacing=(-.7).sp))
        Space(10);Body("상황을 고르고, 직접 보내보세요.",subdued=true)
        Space(18);SectionHeading("빠른 시연")
        quickDemos.take(3).forEach{x->
            BankRow(x.title,x.description,icon=if(x.scenario==DemoScenario.NORMAL)BankIcons.Transfer else if(x.scenario==DemoScenario.LOAN)BankIcons.Bank else BankIcons.Trace,
                modifier=Modifier.testTag("demo_${x.scenario.name}")){if(!interaction.busy)model.stageScenario(x.scenario){open("transfer_state")}}
        }
        Space(8);Rule();SectionHeading("다른 상황",if(expanded)"접기"else"모두 보기"){expanded=!expanded}
        (if(expanded)quickDemos.drop(3)else quickDemos.drop(3).take(2)).forEach{x->
            BankRow(x.title,x.description,modifier=Modifier.testTag("demo_${x.scenario.name}")){if(!interaction.busy)model.stageScenario(x.scenario){open("transfer_state")}}
        }
        Space(10);BankRow("같은 300만 원 비교","계좌·금액·목적은 그대로",icon=BankIcons.Transfer,modifier=Modifier.testTag("demo_compare")){open("comparison")}
        BankRow("시나리오 상세","입력과 예상 동작 확인",icon=BankIcons.Info,modifier=Modifier.testTag("demo_all_cases")){open("demo_lab")}
        Space(18);Caption("시연 시작 시 가상 잔액을 초기화합니다. 결과 화면을 강제하지 않고 확인·인증·로컬 정책을 거칩니다.")
        QuietButton("시연센터 숨기기",Modifier.fillMaxWidth().testTag("demo_lock")){lock=true}
    }
    if(lock)AlertDialog(onDismissRequest={lock=false},title={Text("시연센터를 숨길까요?")},text={Text("시연 데이터는 유지하고 일반 메뉴로 돌아갑니다.")},
        confirmButton={QuietButton("숨기기",Modifier.testTag("demo_lock_confirm")){model.stageLock();open("home")}},dismissButton={QuietButton("취소"){lock=false}},containerColor=TraceColors.Surface)
}

@Composable fun BankingSafety(state:BankState,open:(String)->Unit) {
    Page("안전","safety") {
        Space(8);TraceSignature();Space(18)
        Text(if(state.pending.isEmpty())"돈을 보내기 전,\n흐름을 봅니다."else"보내지 않은 송금부터\n살펴볼까요?",style=MaterialTheme.typography.headlineLarge.copy(letterSpacing=(-.65).sp))
        Space(12);Body("누가, 왜, 지금 보내라고 했는지.\n송금 앞의 맥락을 함께 확인해요.",subdued=true)
        Space(24);QuietSurface {
            BankRow("보류·확인 중인 송금",value="${state.pending.size}건",icon=BankIcons.Pause){open("pending")}
            Rule();BankRow("보호 기록과 이어진 흐름","거래와 연결된 위험 신호",icon=BankIcons.History){open("timeline")}
        }
        Space(18);BankRow("요청 내용 직접 확인","붙여넣은 내용을 기기 안에서 확인",icon=BankIcons.Message){open("manual")}
        BankRow("공식 경로로 확인","상대가 보낸 번호나 링크 대신",icon=BankIcons.Bank){open("safety_guide")}
        BankRow("계정 일시잠금","송금과 내 계좌 이동 중지",icon=BankIcons.Lock){open("account_lock")}
        BankRow("피해 신고·상담","기기 내 시연 접수",icon=BankIcons.Phone){open("report")}
        Space(18);Caption("점수 하나로 안전을 보장하지 않습니다. 현재 앱은 가상 금융 데이터와 로컬 시연 정책을 사용합니다.")
    }
}
