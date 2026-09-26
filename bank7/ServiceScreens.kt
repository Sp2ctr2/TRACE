@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.saeon.trace.ui.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.BankViewModel
import app.saeon.trace.ui.design.*
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Service requests are local, explicitly marked simulations. Ledger movements
 * still go through BankRepository's atomic transaction; no screen edits a balance. */
class ServiceStore(context:Context) {
    private val prefs=context.getSharedPreferences("saeon_services_v7",Context.MODE_PRIVATE)
    fun text(key:String,default:String="")=prefs.getString(key,default)?:default
    fun number(key:String,default:Long)=prefs.getLong(key,default)
    fun put(key:String,value:String){check(prefs.edit().putString(key,value).commit()){"설정을 저장하지 못했어요."}}
    fun put(key:String,value:Long){check(prefs.edit().putLong(key,value).commit()){"설정을 저장하지 못했어요."}}
    fun rows(key:String):List<JSONObject> {val a=JSONArray(text(key,"[]"));return (0 until a.length()).map{a.getJSONObject(it)}}
    fun saveRows(key:String,rows:List<JSONObject>) {put(key,JSONArray().apply{rows.forEach{put(it)}}.toString())}
    fun add(key:String,row:JSONObject){saveRows(key,rows(key)+row)}
}
@Composable private fun serviceStore():ServiceStore {val c=LocalContext.current;return remember(c){ServiceStore(c)}}
private fun htmlEscape(x:String)=x.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
private fun shareDocument(context:Context,title:String,body:String) {
    val dir=File(context.cacheDir,"documents").apply{mkdirs()}
    val file=File(dir,"SAEON_${System.currentTimeMillis()}.html")
    file.writeText("<!doctype html><html lang='ko'><meta charset='utf-8'><title>${htmlEscape(title)}</title><style>body{font:16px/1.8 sans-serif;max-width:700px;margin:40px auto;padding:24px;color:#20211f;background:#f5f4f0}h1{font-size:28px}pre{white-space:pre-wrap;font:inherit}.notice{border:1px solid #ddddd5;padding:16px}</style><h1>${htmlEscape(title)}</h1><p class='notice'>새온은행 × TRACE 시연용 문서 · 법적 효력 없음 · 실제 금융기관에서 발급하지 않았습니다.</p><pre>${htmlEscape(body)}</pre></html>")
    val uri=FileProvider.getUriForFile(context,context.packageName+".documents",file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="text/html";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"시연 문서 내보내기"))
}

@Composable fun DocumentsScreen(state:BankState,open:(String)->Unit,back:()->Unit) {
    Page("확인증·증명서","documents",back=back) {
        Space(12);Headline("필요한 내역을\n한 장으로");Space(14);Body("기기에 저장된 가상 거래와 계좌 정보로 시연 문서를 만들어요.",subdued=true);Space(26)
        BankRow("잔액 확인서","현재 잔액과 발급 시각",BankIcons.Bank,tag="document_balance"){open("document/balance")}
        BankRow("계좌 확인서","계좌명과 가상 계좌번호",BankIcons.History,tag="document_account"){open("document/account")}
        Space(20);SectionTitle("송금 확인증")
        state.receipts.filter{it.direction==Direction.DEBIT}.forEach{r->BankRow(r.recipient.name,"${won(r.amount)}원 · ${dateLabel(r.completedAt)}",BankIcons.History,tag="document_${r.id}"){open("document/${r.id}")}}
        Space(18);Caption("모든 문서에 시연용 표시가 들어가며, 외부 기관에 제출할 수 없어요.")
    }
}
@Composable fun DocumentScreen(id:String,state:BankState,back:()->Unit) {
    val context=LocalContext.current
    val title=when(id){"balance"->"잔액 확인서";"account"->"계좌 확인서";else->"송금 확인증"}
    val receipt=state.receipts.find{it.id==id}
    val valid=id in setOf("balance","account")||receipt!=null
    val body=buildString{
        append("발급구분: 시연용\n계좌: 새온 생활통장\n계좌번호: 110-***-0001\n")
        if(id=="balance")append("잔액: ${won(state.balance)}원\n")
        if(receipt!=null)append("받는 분: ${receipt.recipient.name}\n받는 계좌: ${receipt.recipient.bank} ${receipt.recipient.account}\n송금액: ${won(receipt.amount)}원\n수수료: 0원\n거래번호: ${receipt.id}\n거래시각: ${dateLabel(receipt.completedAt)} ${timeLabel(receipt.completedAt)}\n")
        append("발급시각: ${dateLabel(System.currentTimeMillis())} ${timeLabel(System.currentTimeMillis())}\n실제 자금 이동 및 금융망 연결 없음")
    }
    var failure by remember{mutableStateOf<String?>(null)}
    Page(title,"document_detail",back=back,footer={PrimaryButton("시연 문서 내보내기",Modifier.testTag("document_export"),enabled=valid){runCatching{shareDocument(context,title,body)}.onFailure{failure="내보내기를 열지 못했어요. 공유 가능한 앱을 확인해 주세요."}}}) {
        Space(18);SurfaceBox{Caption("새온은행");Space(18);Headline(title);Space(22);Body(if(valid)body else"해당 거래 기록이 없습니다.");Space(18);Caption("시연용 · 법적 효력 없음")}
        failure?.let{Space(14);ErrorNote(it)}
    }
}

@Composable fun SchedulesScreen(state:BankState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val store=serviceStore();var revision by remember{mutableIntStateOf(0)}
    val rows=remember(revision){store.rows("schedules")}
    var adding by rememberSaveable{mutableStateOf(false)}
    var name by rememberSaveable{mutableStateOf("월세")};var amount by rememberSaveable{mutableStateOf("500000")};var day by rememberSaveable{mutableStateOf("25")}
    var error by remember{mutableStateOf<String?>(null)}
    Page("예약·자동이체 관리","schedules",back=back,footer={PrimaryButton("예약 알림 추가",Modifier.testTag("schedule_add")){adding=true}}) {
        Space(12);Headline("잊지 않고,\n보낼 수 있도록");Space(12);Body("시연에서는 예약을 저장하고 송금 직전까지 준비해요. 백그라운드에서 돈을 보내지 않습니다.",subdued=true);Space(24)
        SurfaceBox{Text("통신비",style=MaterialTheme.typography.titleSmall);TaskDetail("금액","68,000원");OptionRow("매월 25일 알림",state.recurringEnabled){model.act{model.repository.recurring(it)}}}
        rows.forEach{row->Space(12);SurfaceBox{
            Text(row.getString("title"),style=MaterialTheme.typography.titleSmall);TaskDetail("금액","${won(row.getLong("amount"))}원");Caption("매월 ${row.getInt("day")}일 · 이서연님에게")
            Row{QuietButton("송금 준비",Modifier.weight(1f)){model.startRecipient(Fixtures.seoyeon){model.storeDraft(TransferDraft(Fixtures.seoyeon,row.getLong("amount"),Purpose.LIVING));open("amount")}}
                QuietButton("삭제",Modifier.weight(1f)){store.saveRows("schedules",rows.filter{it.getString("id")!=row.getString("id")});revision++}}
        }}
        if(rows.isEmpty()){Space(20);Caption("추가한 예약 알림이 없어요. 아래에서 금액과 날짜를 정해보세요.")}
    }
    if(adding)AlertDialog(onDismissRequest={adding=false},title={Text("예약 알림 추가")},text={Column(Modifier.verticalScroll(rememberScrollState())){
        Field(name,"이름",{name=it.take(24)},Modifier.testTag("schedule_name"));Space(10)
        Field(amount,"금액",{amount=it.filter(Char::isDigit).take(9)},Modifier.testTag("schedule_amount"),keyboard=KeyboardType.Number);Space(10)
        Field(day,"매월 날짜 (1–28)",{day=it.filter(Char::isDigit).take(2)},Modifier.testTag("schedule_day"),keyboard=KeyboardType.Number);error?.let{ErrorNote(it)}
    }},confirmButton={QuietButton("저장",Modifier.testTag("schedule_save")){
        val a=amount.toLongOrNull();val d=day.toIntOrNull()
        if(name.isBlank()||a==null||a<=0||a>100_000_000||d==null||d !in 1..28)error="이름, 1원 이상 금액, 1–28일을 입력해 주세요."
        else{store.add("schedules",JSONObject().put("id",newId()).put("title",name.trim()).put("amount",a).put("day",d));revision++;adding=false;error=null}
    }},dismissButton={QuietButton("취소"){adding=false}})
}

@Composable fun CardServiceScreen(p:BankPreferences,model:BankViewModel,back:()->Unit) {
    val store=serviceStore();var status by remember{mutableStateOf(store.text("card_status","normal"))};var confirm by remember{mutableStateOf(false)}
    Page("카드 분실·재발급","card_service",back=back) {
        Space(16);Headline("카드가 보이지\n않으시나요?");Space(16);Body("먼저 사용을 잠시 멈춰두세요. 찾은 뒤 일시정지를 해제할 수 있어요.",subdued=true);Space(24)
        SurfaceBox{Text("새온 체크카드",style=MaterialTheme.typography.titleMedium);TaskDetail("카드번호","•••• •••• •••• 3014");TaskDetail("상태",when(status){"lost"->"분실 등록 · 시연";"reissue"->"재발급 요청 저장 · 시연";else->if(p.cardFrozen)"일시정지"else"이용 가능"})}
        Space(20);OptionRow("카드 일시정지",p.cardFrozen,"기기의 가상 카드 상태를 변경해요."){model.preference{cardFrozen(it)}}
        Space(18);PrimaryButton("분실 등록",Modifier.testTag("card_lost"),enabled=status=="normal"){confirm=true}
        Space(10);SecondaryButton("재발급 요청 저장",Modifier.testTag("card_reissue"),enabled=status=="lost"){status="reissue";store.put("card_status",status)}
        if(status!="normal")QuietButton("시연 상태 복구",Modifier.fillMaxWidth()){status="normal";store.put("card_status",status);model.preference{cardFrozen(false)}}
        Space(18);Caption("실제 카드사에 접수되거나 카드가 발송되지 않습니다. 실제 분실은 해당 카드사의 공식 경로로 신고하세요.")
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("분실 상태로 등록할까요?")},text={Text("가상 카드를 일시정지하고 분실 기록을 저장합니다. 실제 카드에는 영향이 없어요.")},confirmButton={QuietButton("분실 등록",Modifier.testTag("card_lost_confirm")){status="lost";store.put("card_status",status);model.preference{cardFrozen(true)};confirm=false}},dismissButton={QuietButton("취소"){confirm=false}})
}

@Composable fun AccountLockScreen(state:BankState,model:BankViewModel,back:()->Unit) {
    var confirm by remember{mutableStateOf(false)}
    Page("계정 보호","account_lock",back=back,footer={PrimaryButton(if(state.accountLocked)"시연 계정 잠금 해제"else"송금 잠그기",Modifier.testTag("account_lock_toggle")){confirm=true}}) {
        Space(28);AppIcon(BankIcons.Lock,size=38,tint=TraceColors.Coral);Space(24)
        Headline(if(state.accountLocked)"송금이\n잠겨 있어요"else"불안할 때는,\n먼저 잠가두세요")
        Space(18);Body("잠금은 이 기기의 모든 가상 송금 경로에 적용돼요. 인증을 이미 마친 거래도 실행 직전에 다시 확인합니다.",subdued=true)
        Space(24);SurfaceBox{TaskDetail("현재 상태",if(state.accountLocked)"송금 잠금"else"이용 가능");TaskDetail("계좌 잔액","${won(state.balance)}원")}
        Space(24);Caption("실제 은행 계정의 잠금이 아닙니다. 이 데모의 잠금 해제는 실제 본인확인을 대신하지 않습니다.")
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text(if(state.accountLocked)"시연 잠금을 해제할까요?"else"송금을 잠글까요?")},text={Text("기록과 잔액은 유지합니다. 진행 중 인증은 폐기하고 새 확인을 요구합니다.")},confirmButton={QuietButton("확인",Modifier.testTag("account_lock_confirm")){model.act{model.repository.change{s,_->s.copy(accountLocked=!s.accountLocked,records=s.records.map{if(it.stage in setOf(TransferStage.AUTHORIZING,TransferStage.EVALUATING))it.copy(stage=TransferStage.REVIEW,authorization=null,challenge=null,attestation=null)else it})};confirm=false}}},dismissButton={QuietButton("취소"){confirm=false}})
}

@Composable fun SpendingScreen(back:()->Unit) {
    val store=serviceStore();var budget by remember{mutableLongStateOf(store.number("budget",600000))};var edit by remember{mutableStateOf(false)};var value by remember{mutableStateOf("")}
    val categories=listOf("식비" to 124900L,"쇼핑" to 96000L,"통신" to 68000L,"교통" to 43000L,"기타" to 50500L)
    Page("이번 달 소비","spending",back=back) {
        Space(14);Caption("9월 카드 이용");Space(4);BankAmount(382400);Space(24)
        SurfaceBox{TaskDetail("내 예산","${won(budget)}원");LinearProgressIndicator(progress={(382400f/budget).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth(),color=TraceColors.Coral,trackColor=TraceColors.Divider);Space(8);Caption(if(budget>=382400)"${won(budget-382400)}원 남았어요"else"${won(382400-budget)}원 초과했어요");QuietButton("예산 바꾸기",Modifier.testTag("budget_edit")){value=budget.toString();edit=true}}
        Space(24);SectionTitle("어디에 썼을까요?")
        categories.forEach{(label,amount)->TaskDetail(label,"${won(amount)}원");LinearProgressIndicator(progress={amount/382400f},modifier=Modifier.fillMaxWidth().height(3.dp),color=TraceColors.Muted,trackColor=TraceColors.Divider);Space(14)}
        Space(12);Caption("카드 사용 내역은 시연용 데이터입니다. 송금 내역과는 별도로 집계합니다.")
    }
    if(edit)AlertDialog(onDismissRequest={edit=false},title={Text("월 예산")},text={Field(value,"금액",{value=it.filter(Char::isDigit).take(9)},keyboard=KeyboardType.Number)},confirmButton={QuietButton("저장",enabled=(value.toLongOrNull()?:0)>0){budget=value.toLong();store.put("budget",budget);edit=false}},dismissButton={QuietButton("취소"){edit=false}})
}

@Composable fun GoalsScreen(state:BankState,model:BankViewModel,back:()->Unit) {
    val store=serviceStore();var target by remember{mutableLongStateOf(store.number("goal",6000000))};var edit by remember{mutableStateOf(false)};var value by remember{mutableStateOf("")}
    var transfer by remember{mutableStateOf(false)};var amount by remember{mutableStateOf("100000")};var message by remember{mutableStateOf<String?>(null)}
    Page("모으는 목표","goals",back=back,footer={PrimaryButton("내 적금에 더 모으기",Modifier.testTag("goal_deposit")){transfer=true}}) {
        Space(14);Headline("조금씩 모아,\n원하는 순간으로");Space(20);BankAmount(state.savings)
        Space(24);SurfaceBox{TaskDetail("목표","${won(target)}원");LinearProgressIndicator(progress={(state.savings.toFloat()/target).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth(),color=TraceColors.Coral,trackColor=TraceColors.Divider);Space(10);Caption("${(state.savings*100/target)}% 모았어요");QuietButton("목표 바꾸기",Modifier.testTag("goal_edit")){value=target.toString();edit=true}}
        Space(24);Body("새온 생활통장과 모아적금 사이에서만 옮깁니다. 모으는 금액만큼 생활통장 잔액이 줄어요.",subdued=true)
        message?.let{Space(16);Caption(it)}
    }
    if(edit)AlertDialog(onDismissRequest={edit=false},title={Text("모으는 목표")},text={Field(value,"목표 금액",{value=it.filter(Char::isDigit).take(9)},keyboard=KeyboardType.Number)},confirmButton={QuietButton("저장",enabled=(value.toLongOrNull()?:0)>0){target=value.toLong();store.put("goal",target);edit=false}},dismissButton={QuietButton("취소"){edit=false}})
    if(transfer)AlertDialog(onDismissRequest={transfer=false},title={Text("얼마를 더 모을까요?")},text={Column{Field(amount,"금액",{amount=it.filter(Char::isDigit).take(9)},Modifier.testTag("goal_amount"),keyboard=KeyboardType.Number);Caption("생활통장 잔액 ${won(state.balance)}원")}},confirmButton={QuietButton("내 적금으로 옮기기",Modifier.testTag("goal_confirm"),enabled=(amount.toLongOrNull()?:0)>0){val requested=amount.toLong();val requestId=newId();model.act{model.repository.depositToSavings(requested,requestId);message="${won(requested)}원을 모아적금으로 옮겼어요.";transfer=false}}},dismissButton={QuietButton("취소"){transfer=false}})
}

@Composable fun NotesScreen(kind:String,back:()->Unit) {
    val store=serviceStore();val key=if(kind=="investments")"watchlist"else"financial_notes"
    var rows by remember{mutableStateOf(store.rows(key))};var adding by remember{mutableStateOf(false)};var text by remember{mutableStateOf("")}
    Page(if(kind=="investments")"투자 관심목록"else"신용·보험 메모",kind,back=back,footer={PrimaryButton(if(kind=="investments")"관심 항목 추가"else"메모 추가",Modifier.testTag("note_add")){adding=true}}) {
        Space(14);Headline(if(kind=="investments")"관심 있는 것부터,\n천천히 살펴보세요"else"필요한 정보를\n한곳에 적어두세요");Space(16)
        if(kind=="investments"){Caption("보유 투자금");BankAmount(0);Space(12)}
        Body(if(kind=="investments")"시세 조회나 매매 주문은 제공하지 않는 개인 관심목록입니다."else"실제 신용점수·보험 조회가 아닌 기기 내 개인 메모예요. 민감한 인증정보는 입력하지 마세요.",subdued=true);Space(22)
        rows.forEach{row->SurfaceBox{Text(row.getString("text"),style=MaterialTheme.typography.bodyLarge);QuietButton("삭제"){rows=rows.filter{it.getString("id")!=row.getString("id")};store.saveRows(key,rows)}};Space(10)}
        if(rows.isEmpty())EmptyState("아직 저장한 항목이 없어요.","아래 버튼에서 직접 추가할 수 있어요.")
    }
    if(adding)AlertDialog(onDismissRequest={adding=false},title={Text("새 항목")},text={Field(text,"내용",{text=it.take(200)},Modifier.testTag("note_text"))},confirmButton={QuietButton("저장",Modifier.testTag("note_save"),enabled=text.isNotBlank()){rows=rows+JSONObject().put("id",newId()).put("text",text.trim());store.saveRows(key,rows);text="";adding=false}},dismissButton={QuietButton("취소"){adding=false}})
}

@Composable fun SupportRequestsScreen(back:()->Unit) {
    val store=serviceStore();var rows by remember{mutableStateOf(store.rows("requests"))};var body by rememberSaveable{mutableStateOf("")};var sent by remember{mutableStateOf<String?>(null)}
    Page("문의 남기기","support_request",back=back,footer={PrimaryButton("기기에 문의 저장",Modifier.testTag("support_save"),enabled=body.trim().length>=5){val id="DEMO-"+System.currentTimeMillis().toString().takeLast(6);val row=JSONObject().put("id",id).put("text",body.trim());rows=rows+row;store.saveRows("requests",rows);body="";sent=id}}) {
        Space(12);Headline("어떤 도움이\n필요하신가요?");Space(14);Body("이 시연에서는 문의를 기기에만 저장합니다. 상담원에게 전송되거나 답변이 오지는 않아요.",subdued=true);Space(20)
        OutlinedTextField(body,{body=it.take(1000)},label={Text("문의 내용 (5자 이상)")},modifier=Modifier.fillMaxWidth().testTag("support_body"),minLines=4)
        sent?.let{Space(12);Caption("$it 문의를 이 기기에 저장했어요.")}
        Space(24);SectionTitle("저장한 문의")
        rows.asReversed().forEach{r->SurfaceBox{Caption(r.getString("id"));Text(r.getString("text"),style=MaterialTheme.typography.bodyMedium)};Space(10)}
    }
}

@Composable fun ReportScreen(open:(String)->Unit,back:()->Unit) {
    val store=serviceStore();var category by rememberSaveable{mutableStateOf("기관 사칭")};var saved by remember{mutableStateOf(store.text("report"))}
    Page("피해 신고·보호 안내","report",back=back,footer={PrimaryButton("기기에 상황 기록",Modifier.testTag("report_save")){saved="${dateLabel(System.currentTimeMillis())} · $category";store.put("report",saved)}}) {
        Space(12);Headline("보내기 전에,\n연락을 멈추고 확인해요");Space(18)
        Body("상대가 준 링크나 전화번호 대신, 이용 중인 은행의 공식 앱이나 직접 확인한 대표번호를 이용하세요.",subdued=true);Space(22)
        BankRow("송금 먼저 잠그기","이 기기의 가상 송금 차단",BankIcons.Lock){open("account_lock")}
        BankRow("보류한 송금 확인","확인 중인 거래와 이유",BankIcons.Pause){open("pending")}
        Space(18);SectionTitle("어떤 상황이었나요?")
        listOf("기관 사칭","가족 사칭","링크 유도","원격 조작","기타").forEach{c->Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable{category=c},verticalAlignment=Alignment.CenterVertically){RadioButton(category==c,{category=c});Text(c)}}
        if(saved.isNotBlank()){Space(16);Caption("기기 내 기록: $saved")}
        Space(18);Caption("여기서 저장해도 경찰이나 금융기관에 신고되지 않습니다. 실제 피해 대응은 공식 기관을 통해 진행해야 합니다.")
    }
}

@Composable fun TermsScreen(back:()->Unit) {
    Page("시연 약관","terms",back=back) {
        Space(16);Headline("이 앱을 사용하기 전");Space(24)
        listOf("실제 은행이 아닙니다" to "새온은행은 TRACE 시연을 위한 가상 은행입니다. 예금 수신, 대출 실행, 투자 매매, 실제 송금은 제공하지 않습니다.","판단 결과의 범위" to "정해진 가상 맥락을 로컬 규칙으로 평가합니다. 실제 학습 모델의 정확도나 모든 사기 예방을 증명하지 않습니다.","저장과 삭제" to "거래·설정·문의는 기기에 저장됩니다. 앱 데이터를 삭제하면 기록이 사라집니다. 문의와 신고 기록은 외부 기관에 전송되지 않습니다.","증명서와 예약" to "확인증은 시연용이며 법적 효력이 없습니다. 예약 관리는 알림과 송금 준비용이며 백그라운드 자동 출금을 실행하지 않습니다.").forEach{(title,body)->Text(title,style=MaterialTheme.typography.titleMedium);Space(10);Body(body,subdued=true);Space(26)}
    }
}
