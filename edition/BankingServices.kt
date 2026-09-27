@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.saeon.trace.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.data.*
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

@Composable fun BankingServices(route:String,state:BankState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    when(route) {
        "certificates" -> CertificateCenter(state,model,open,back)
        "certificate" -> CertificateView(state,model,back)
        "account_lock" -> AccountLockScreen(state,model,back)
        "card_service" -> CardCareScreen(model,open,back)
        "schedules" -> ScheduleScreen(state,model,open,back)
        "savings_add" -> SavingsAdd(state,model,open,back)
        "support","report" -> SupportCenter(route=="report",model,open,back)
        "requests" -> RequestHistory(model,back)
        "investments","investment_detail","credit","insurance","spending" -> FinancialDetails(route,state,model,open,back)
        "terms" -> BankTerms(back)
    }
}
private fun docName(kind:String)=when(kind){"balance"->"잔액 확인서";"account"->"계좌 정보 확인서";else->"거래 내역 확인서"}
private fun escape(s:String)=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
@Composable private fun CertificateCenter(state:BankState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val data by model.serviceState.collectAsStateWithLifecycle()
    Page("증명서 발급","certificates",back=back) {
        Space(8);Headline("필요한 내역을\n한 장에 담아요.");Space(12);Body("이 기기의 가상 계좌와 거래 기록으로 만듭니다.",subdued=true);Space(24)
        listOf("balance" to "현재 계좌 잔액","account" to "마스킹된 계좌 정보","history" to "가상 거래 내역").forEach{(kind,sub)->
            BankRow(docName(kind),sub,icon=BankIcons.History,modifier=Modifier.testTag("issue_$kind")){
                model.act { model.services.issue(kind,state.balance);open("certificate") }
            }
        }
        Space(20);SectionHeading("발급 내역")
        if(data.documents.isEmpty())Caption("아직 발급한 문서가 없어요.")
        data.documents.take(5).forEach{d->BankRow(docName(d.kind),d.id){open("certificate")}}
        Space(22);Caption("모든 문서에 시연 표시가 들어갑니다. 실제 은행 증명서가 아니며 법적 효력은 없습니다.")
        data.error?.let{ErrorNote(it)}
    }
}
@Composable private fun CertificateView(state:BankState,model:BankViewModel,back:()->Unit) {
    val data by model.serviceState.collectAsStateWithLifecycle()
    val d=data.documents.firstOrNull()
    val context=LocalContext.current
    var saved by remember{mutableStateOf(false)}
    var saveError by remember{mutableStateOf<String?>(null)}
    val body=if(d==null)"" else "<!doctype html><html lang=\"ko\"><meta charset=\"utf-8\"><title>${docName(d.kind)} — 시연</title><style>body{font:16px sans-serif;background:#F5F4F0;color:#20211F;max-width:720px;margin:60px auto;padding:32px}h1{font-size:30px}aside{border:2px solid #EF4A32;padding:14px}td{padding:12px;border-bottom:1px solid #DDDDD5}</style><aside>시연 문서 · 실제 효력 없음</aside><h1>새온은행 ${docName(d.kind)}</h1><p>${escape(d.id)}</p><p>새온 생활통장 110-***-0001</p><p>발급 시점 잔액: ${won(d.amount)}원</p><table>"+(if(d.kind=="history")state.receipts.map{"<tr><td>${escape(it.recipient.name)}</td><td>${if(it.direction==Direction.CREDIT)"+"else"−"}${won(it.amount)}원</td></tr>"}.joinToString("")else"")+"</table><p>가상 데이터입니다. 외부 기관에 제출할 수 없습니다.</p></html>"
    val currentBody by rememberUpdatedState(body)
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")){uri->
        if(uri!=null)runCatching{context.contentResolver.openOutputStream(uri)?.use{it.write(currentBody.toByteArray(Charsets.UTF_8))}?:error("output missing")}
            .onSuccess{saved=true}.onFailure{saveError="파일을 저장하지 못했어요. 다른 위치로 다시 저장해 주세요."}
    }
    Page("문서 확인","certificate",back=back,footer={BankAction("HTML 파일로 저장",Modifier.testTag("certificate_save"),enabled=d!=null){save.launch("${d?.id?:"demo"}.html")}}) {
        if(d==null){EmptyState("발급할 문서를 선택해 주세요.","증명서 발급에서 필요한 내역을 고를 수 있어요.");return@Page}
        QuietSurface {
            Caption("시연 문서 · 실제 효력 없음");Space(20);Text(docName(d.kind),style=MaterialTheme.typography.headlineMedium);Space(12);Caption(d.id);Space(24)
            DetailRow("은행","새온은행");DetailRow("계좌","110-***-0001");DetailRow("발급 시점 잔액","${won(d.amount)}원")
            if(d.kind=="history")state.receipts.take(10).forEach{DetailRow(it.recipient.name,"${if(it.direction==Direction.CREDIT)"+"else"−"}${won(it.amount)}원")}
        }
        Space(20);Body("다운로드한 문서는 브라우저로 열 수 있어요. 실제 은행이 발급한 문서가 아닙니다.",subdued=true)
        if(saved)Caption("선택한 위치에 저장했어요.")
        saveError?.let{ErrorNote(it)}
    }
}
@Composable private fun AccountLockScreen(state:BankState,model:BankViewModel,back:()->Unit) {
    var confirm by remember{mutableStateOf(false)}
    TaskPage("계정 일시잠금","account_lock",back=back,footer={BankAction(if(state.accountLocked)"잠금 해제 확인"else"계정을 잠시 잠그기",Modifier.testTag("account_lock_action")){confirm=true}}) {
        Space(20);AppIcon(BankIcons.Lock,size=36,tint=TraceColors.Ink);Space(24)
        TaskHeadline(if(state.accountLocked)"지금은 돈이\n나가지 않아요."else"확인할 동안,\n계정을 잠가두세요.")
        Space(18);Body(if(state.accountLocked)"잔액과 거래 내역은 볼 수 있어요. 새 송금과 내 계좌 사이의 이동은 중지됩니다."else"잠금은 새 송금과 내 계좌 이동에 함께 적용됩니다. 이미 완료한 거래는 취소되지 않아요.",subdued=true)
        Space(28);TaskDetail("현재 상태",if(state.accountLocked)"잠김"else"사용 중");TaskDetail("출금 가능 잔액","${won(state.balance)}원")
        Space(20);Caption("이 기기의 가상 계좌만 잠급니다. 실제 금융기관의 지급정지나 신고를 대신하지 않습니다.")
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text(if(state.accountLocked)"시연 계정의 잠금을 해제할까요?"else"시연 계정을 잠글까요?")},text={Text("저장된 잔액은 바뀌지 않습니다. 진행 중 인증은 무효화합니다.")},
        confirmButton={QuietButton("확인",Modifier.testTag("account_lock_confirm")){model.act{model.repository.setAccountLocked(!state.accountLocked);confirm=false}}},dismissButton={QuietButton("취소"){confirm=false}},containerColor=TraceColors.Surface)
}
@Composable private fun CardCareScreen(model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val p by model.preferences.collectAsStateWithLifecycle();val data by model.serviceState.collectAsStateWithLifecycle()
    var ask by remember{mutableStateOf(false)}
    Page("카드 분실·재발급","card_service",back=back) {
        Space(16);Headline(if(data.cardLost)"카드를 분실 상태로\n보호하고 있어요."else"카드가 보이지\n않나요?");Space(14)
        Body("먼저 일시 정지하고, 다시 찾으면 해제할 수 있어요.",subdued=true);Space(24)
        OptionRow("카드 일시 정지",p.cardFrozen,"가상 카드의 사용 상태를 저장합니다."){model.preference{cardFrozen(it)}}
        BankRow("분실 신고",if(data.cardLost)"이 기기에 분실 기록됨"else"가상 카드 정지와 분실 기록",icon=BankIcons.Lock,modifier=Modifier.testTag("card_lost")){
            model.act { model.services.lost(true);model.graph.preferences.cardFrozen(true) }
        }
        BankRow("재발급 요청",if(data.reissueRequested)"가상 요청이 접수되어 있어요"else"실제 카드 배송은 진행하지 않음",icon=BankIcons.Card,modifier=Modifier.testTag("card_reissue")){ask=true}
        Space(14);BankRow("내 접수 내역",icon=BankIcons.History){open("requests")}
        Space(20);Caption("실제 카드 승인망은 연결되어 있지 않습니다. 실제 분실 시에는 발급한 카드사의 공식 경로를 이용하세요.")
    }
    if(ask)AlertDialog(onDismissRequest={ask=false},title={Text("가상 재발급 요청을 남길까요?")},text={Text("배송지나 주민등록번호를 입력하지 않습니다. 실제 재발급은 신청되지 않습니다.")},confirmButton={QuietButton("요청 기록",Modifier.testTag("card_reissue_confirm")){model.act{model.services.request("카드 재발급","새온 체크카드 시연 재발급 요청");ask=false}}},dismissButton={QuietButton("취소"){ask=false}})
}
@Composable private fun ScheduleScreen(state:BankState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val data by model.serviceState.collectAsStateWithLifecycle()
    var adding by remember{mutableStateOf(false)};var name by rememberSaveable{mutableStateOf("")};var amount by rememberSaveable{mutableStateOf("")};var day by rememberSaveable{mutableStateOf("25")}
    Page("자동이체","schedules",back=back,footer={BankAction("일정 추가",Modifier.testTag("schedule_add")){adding=true}}) {
        Space(8);Headline("잊지 않도록\n미리 정리해요.");Space(16);Body("시연 앱에서는 일정만 저장합니다. 날짜가 되어도 자동으로 돈을 보내지 않아요.",subdued=true);Space(22)
        BankRow("통신비","매월 25일","68,000원",BankIcons.Calendar){open("recurring")}
        data.schedules.forEach{s->
            QuietSurface {
                OptionRow(s.name,s.enabled,"매월 ${s.day}일 · ${won(s.amount)}원"){model.act{model.services.toggleSchedule(s.id,it)}}
                Row{
                    QuietButton("송금 준비",Modifier.weight(1f)){model.act{val r=state.recipients.find{it.id==s.recipientId}?:Fixtures.seoyeon;model.repository.review(TransferDraft(r,s.amount,Purpose.GENERAL));open("transfer_state")}}
                    QuietButton("일정 삭제",Modifier.weight(1f)){model.act{model.services.removeSchedule(s.id)}}
                }
            };Space(10)
        }
        if(data.schedules.isEmpty())Caption("추가한 일정이 없어요.")
    }
    if(adding)ModalBottomSheet(onDismissRequest={adding=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=TraceColors.Surface){
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).testTag("schedule_form")) {
            Text("새 일정",style=MaterialTheme.typography.headlineSmall);Space(16)
            Field(name,"일정 이름",{name=it.take(24)},Modifier.testTag("schedule_name"));Space(10)
            Field(amount,"금액",{amount=it.filter(Char::isDigit).take(9)},Modifier.testTag("schedule_amount"),keyboard=KeyboardType.Number);Space(10)
            Field(day,"매월 날짜 (1–28)",{day=it.filter(Char::isDigit).take(2)},Modifier.testTag("schedule_day"),keyboard=KeyboardType.Number);Space(12)
            Caption("받는 분: 이서연 · 노을은행 110-***-7421");Space(16)
            BankAction("일정 저장",Modifier.testTag("schedule_save"),enabled=name.isNotBlank()&&(amount.toLongOrNull()?:0)in 1..100000000&&(day.toIntOrNull()?:0)in 1..28){
                model.act{model.services.addSchedule(name,Fixtures.seoyeon.id,amount.toLong(),day.toInt());adding=false;name="";amount=""}
            }
        }
    }
}
@Composable private fun SavingsAdd(state:BankState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    var digits by rememberSaveable{mutableStateOf("100000")};val id=rememberSaveable{newId()};var done by rememberSaveable{mutableStateOf(false)}
    val amount=digits.toLongOrNull()?:0
    Page("저축하기","savings_add",back=back,footer={BankAction(if(done)"저축 내역 보기"else"모아적금으로 옮기기",Modifier.testTag("savings_confirm"),enabled=done||(amount>0&&amount<=state.balance&&!state.accountLocked)){
        if(done)open("savings")else model.act{model.repository.saveToSavings(amount,id);done=true}
    }}) {
        Space(18);Headline(if(done)"모아적금에\n옮겼어요."else"얼마를 더\n모을까요?");Space(24)
        if(done)BankAmount(amount)else Field(digits,"저축할 금액",{digits=it.filter(Char::isDigit).take(9)},Modifier.testTag("savings_amount"),keyboard=KeyboardType.Number)
        Space(20);DetailRow("출금 계좌","생활통장");DetailRow("받는 계좌","모아적금");DetailRow("현재 생활통장","${won(state.balance)}원");DetailRow("현재 모아적금","${won(state.savings)}원")
        Space(20);Caption("내 가상 계좌 사이에서 이동합니다. 합계는 바뀌지 않으며 같은 요청을 두 번 처리하지 않습니다.")
        if(state.accountLocked)ErrorNote("계정이 잠겨 있어요. 해제하기 전에는 옮길 수 없습니다.")
    }
}
@Composable private fun SupportCenter(report:Boolean,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    var text by rememberSaveable{mutableStateOf("")};var requestId by rememberSaveable{mutableStateOf<String?>(null)}
    Page(if(report)"피해 신고·상담"else"고객센터",if(report)"report"else"support",back=back) {
        Space(8);Headline(if(report)"확인하기 전에는\n더 보내지 마세요."else"어떤 도움이\n필요한가요?");Space(16)
        Body("이곳은 가상 은행의 기기 내 접수함입니다. 상담원이나 외부 기관에 전송하지 않습니다.",subdued=true);Space(24)
        BankRow("송금을 잘못 보냈어요","거래 내역부터 확인",icon=BankIcons.Transfer){open("history")}
        BankRow("계정을 잠그고 싶어요","새 송금과 계좌 이동 중지",icon=BankIcons.Lock){open("account_lock")}
        BankRow("공식 경로 확인","상대가 준 번호·링크 사용 금지",icon=BankIcons.Bank){open("safety_guide")}
        Space(20);SectionHeading("상담 내용 기록")
        Field(text,"도움이 필요한 내용",{text=it.take(1000)},Modifier.testTag("support_message"),minLines=3,maxLines=5);Space(12)
        BankAction("기기에 접수 기록",Modifier.testTag("support_submit"),enabled=text.trim().length>=2){model.act{requestId=model.services.request(if(report)"피해 신고"else"문의",text).id;text=""}}
        requestId?.let{Space(12);Text("기록했어요 · $it",Modifier.testTag("support_receipt"),style=MaterialTheme.typography.bodyMedium)}
        BankRow("내 접수 내역",icon=BankIcons.History){open("requests")}
        Space(18);Caption("실제 피해 상황에서는 거래한 은행의 공식 고객센터와 신고기관을 직접 이용하세요. 이 앱의 기록은 실제 신고가 아닙니다.")
    }
}
@Composable private fun RequestHistory(model:BankViewModel,back:()->Unit) {
    val data by model.serviceState.collectAsStateWithLifecycle();var selected by remember{mutableStateOf<LocalRequest?>(null)};var clear by remember{mutableStateOf(false)}
    Page("내 접수 내역","requests",back=back,actions={IconAction(BankIcons.Delete,"접수 기록 삭제"){clear=true}}) {
        Caption("기기에만 저장된 시연 접수 내역")
        if(data.requests.isEmpty())EmptyState("남긴 접수 내역이 없어요.","고객센터에서 도움받을 내용을 기록할 수 있어요.")
        data.requests.forEach{r->BankRow(r.kind,r.id){selected=r}}
    }
    selected?.let{r->AlertDialog(onDismissRequest={selected=null},title={Text(r.kind)},text={Column{Caption(r.id);Space(12);Body(r.text);Space(12);Caption("외부 기관으로 전송하지 않았습니다.")}},confirmButton={QuietButton("확인"){selected=null}})}
    if(clear)AlertDialog(onDismissRequest={clear=false},title={Text("접수 기록을 삭제할까요?")},text={Text("이 기기의 상담 기록만 삭제합니다. 거래 내역은 지우지 않습니다.")},confirmButton={QuietButton("삭제"){model.act{model.services.deleteRequests();clear=false}}},dismissButton={QuietButton("취소"){clear=false}})
}

@Composable private fun FinancialDetails(route:String,state:BankState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val data by model.serviceState.collectAsStateWithLifecycle();var period by rememberSaveable{mutableIntStateOf(0)}
    val titles=mapOf("investments" to "투자 모아보기","investment_detail" to "새온 인덱스","credit" to "신용 관리","insurance" to "보장 내역","spending" to "이번 달 쓴 돈")
    Page(titles[route].orEmpty(),route,back=back) {
        when(route) {
            "investments"->{
                Caption("가상 포트폴리오 · 계좌 잔액과 별도");Space(10);BankAmount(1350000);Space(26)
                QuietSurface{BankRow("새온 인덱스","예시 펀드 10주","1,350,000원",BankIcons.Assets){open("investment_detail")}}
                Space(22);OptionRow("새온 인덱스 관심 등록","index" in data.watch,"이 기기에 관심 항목을 저장해요."){model.act{model.services.watch("index",it)}}
                Space(24);Caption("실제 시세·주문·수익률을 제공하지 않습니다. 매매 기능과 실제 투자금 이동은 없습니다.")
            }
            "investment_detail"->{
                Caption("새온 인덱스 · 가상 데이터");Space(10);BankAmount(135000);Space(20)
                Row{listOf("1주","1개월","3개월").forEachIndexed{i,s->FilterChip(period==i,{period=i},label={Text(s)},modifier=Modifier.weight(1f).padding(end=6.dp))}}
                val points=listOf(listOf(.45f,.4f,.6f,.5f,.7f,.58f,.8f),listOf(.1f,.15f,.45f,.3f,.6f,.55f,.8f),listOf(.12f,.4f,.23f,.56f,.44f,.7f,.8f))[period]
                val ink=TraceColors.Ink;val line=TraceColors.Divider
                Canvas(Modifier.fillMaxWidth().height(160.dp).padding(vertical=18.dp)) {
                    drawLine(line,Offset(0f,size.height),Offset(size.width,size.height),1.dp.toPx())
                    val path=Path();points.forEachIndexed{i,v->val x=size.width*i/(points.size-1);val y=size.height*(1-v);if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
                    drawPath(path,ink,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
                }
                Caption("선택 구간: ${listOf("1주","1개월","3개월")[period]} · 예시 가격 흐름")
                DetailRow("보유 수량","10주");DetailRow("현재 예시 평가금","1,350,000원")
                OptionRow("관심 등록","index" in data.watch){model.act{model.services.watch("index",it)}}
            }
            "credit"->{
                Caption("신용 점수 예시 · 실제 조회 아님");Space(16)
                Text("930",style=MaterialTheme.typography.displayLarge);Space(8);Body("숫자보다, 꾸준한 관리부터.",subdued=true);Space(28)
                OptionRow("점수 변동 알림",data.creditAlerts,"시연 설정만 저장하며 외부 기관을 조회하지 않아요."){model.act{model.services.credit(it)}}
                Space(20);QuietSurface{SectionHeading("기본 관리 습관");Body("납부일을 놓치지 않고, 사용 내역을 확인해요. 본인의 실제 점수는 공인된 조회 경로에서 확인하세요.",subdued=true)}
            }
            "insurance"->{
                Caption("가상 계약 1건");Space(16);Headline("보장은 간단히,\n내용은 분명히.");Space(24)
                QuietSurface{Text("새온 생활안심 보장",style=MaterialTheme.typography.titleMedium);Space(16);DetailRow("월 납입 예시","29,000원");DetailRow("계약 상태","설명용 가상 계약");DetailRow("갱신 예정","2027.03.20")}
                Space(20);BankRow("보험 관련 문의 기록",icon=BankIcons.Phone){open("support")};Caption("실제 보장·보험금 청구·보험 판매 기능이 아닙니다.")
            }
            "spending"->{
                Caption("9월 카드 사용");Space(10);BankAmount(382400);Space(24)
                listOf("식비" to 152400L,"교통" to 60000L,"쇼핑" to 120000L,"기타" to 50000L).forEach{(label,value)->
                    DetailRow(label,"${won(value)}원");LinearProgressIndicator(progress={value/382400f},modifier=Modifier.fillMaxWidth().height(3.dp),color=TraceColors.Ink,trackColor=TraceColors.Divider);Space(12)
                }
                Space(16);BankRow("카드 이용 내역",icon=BankIcons.History){open("card")};Caption("월간 합계에는 요약 예시 데이터가 포함됩니다. 실제 카드 사용을 조회하지 않았습니다.")
            }
        }
    }
}
@Composable private fun BankTerms(back:()->Unit) {
    Page("이용 안내와 약관","terms",back=back) {
        listOf("시연 서비스" to "새온은행은 TRACE 경험을 보여주기 위한 가상 은행입니다. 실제 예금, 대출, 보험, 증권 거래를 제공하지 않습니다.",
            "기기 내 저장" to "가상 거래와 설정, 사용자가 작성한 상담 내용은 이 기기에 저장합니다. 계좌와 이름은 실명 확인을 하지 않습니다.",
            "TRACE 판단" to "현재 시연 판단은 로컬 규칙입니다. 실제 학습 모델의 성능이나 모든 사기 차단을 보증하지 않습니다.",
            "문서와 접수" to "발급 문서에는 시연 표시가 들어갑니다. 상담과 재발급 요청은 외부 은행·경찰·카드사로 접수되지 않습니다.",
            "데이터 삭제" to "앱 데이터 삭제로 모든 로컬 시연 기록이 지워집니다. 상담 기록은 접수 내역에서 따로 삭제할 수 있습니다.").forEach{(title,text)->SectionHeading(title);Body(text,subdued=true);Space(20)}
    }
}
