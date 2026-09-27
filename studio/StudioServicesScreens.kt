package app.saeon.trace.ui.screens

import android.content.Intent
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.ui.BankViewModel
import app.saeon.trace.ui.design.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun StudioServiceScreen(route:String,state:BankState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val services by model.services.state.collectAsStateWithLifecycle()
    val pref by model.preferences.collectAsStateWithLifecycle()
    val context=LocalContext.current
    var confirm by rememberSaveable{mutableStateOf(false)}
    var title by rememberSaveable{mutableStateOf("")}
    var amount by rememberSaveable{mutableStateOf("")}
    var day by rememberSaveable{mutableStateOf("25")}
    var body by rememberSaveable{mutableStateOf("")}
    var category by rememberSaveable{mutableStateOf("송금 문의")}
    var selected by rememberSaveable{mutableStateOf("잔액 확인서")}
    fun share(text:String) {context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"시연 자료 공유"))}
    when(route) {
        "investments"->Page("투자","investments",back=back){
            Space(12);Caption("보유 자산 평가액");Space(8);MotionMoney(1_840_000,Modifier.fillMaxWidth());Space(12)
            Body("한 곳에서 살펴보는 투자 현황",subdued=true);Space(24)
            MenuRow("새온 시장지수 펀드","20좌 · 평가액 1,240,000원",BankIcons.Assets){open("investment_detail")};Rule()
            MenuRow("새온 단기채 펀드","60좌 · 평가액 600,000원",BankIcons.Bank){open("bond_detail")}
            Space(24);Caption("가상 보유 내역입니다. 시세 조회나 매매를 제공하지 않습니다.")
        }
        "investment_detail","bond_detail"->Page(if(route=="bond_detail")"새온 단기채 펀드"else"새온 시장지수 펀드",route,back=back){
            Space(16);Caption("가상 보유 내역");Space(8);MotionMoney(if(route=="bond_detail")600_000 else 1_240_000,Modifier.fillMaxWidth());Space(24)
            DetailRow("보유 수량",if(route=="bond_detail")"60좌"else"20좌");DetailRow("매입 금액",if(route=="bond_detail")"600,000원"else"1,200,000원");DetailRow("평가 차익",if(route=="bond_detail")"0원"else"40,000원")
            Space(20);Body("투자 정보는 고정된 시연 데이터입니다.",subdued=true);Space(20)
            MenuRow("보유 자산 확인서",icon=BankIcons.History){open("certificates")}
        }
        "insurance"->Page("보험","insurance",back=back){
            Space(12);Headline("나를 위한 보장");Space(8);Body("계약 1건 · 월 28,000원",subdued=true);Space(24)
            SurfaceBox{Text("새온 생활안심보험",style=MaterialTheme.typography.titleMedium);Space(12);DetailRow("계약 상태","유지 중");DetailRow("납입일","매월 20일");DetailRow("보험 기간","2026.03.20 – 2027.03.19")}
            Space(20);MenuRow("계약 내용 보기",icon=BankIcons.History){open("insurance_detail")};MenuRow("납입 확인서",icon=BankIcons.Download){open("certificates")};Space(12);Caption("계약·보장·납입 금액은 가상 정보입니다.")
        }
        "insurance_detail"->Page("계약 내용","insurance_detail",back=back){Space(20);Headline("새온 생활안심보험");Space(24);DetailRow("계약자",pref.displayName);DetailRow("계약번호","SIM-INS-0001");DetailRow("월 납입액","28,000원");Space(24);Body("생활 중 사고 보장을 설명하기 위한 가상 상품입니다. 실제 보장이나 보험금 지급은 발생하지 않습니다.");Space(20);MenuRow("문의 남기기"){open("inquiry")}}
        "credit"->Page("신용 정보","credit",back=back){Space(16);Headline("금융 생활을\n차근차근 확인해요");Space(12);Caption("외부 신용평가사와 연결하지 않은 시연 요약입니다.");Space(24);DetailRow("대출 잔액","${won(state.loanBalance)}원");DetailRow("연체 내역","시연 내역 없음");DetailRow("카드 사용액","382,400원");Space(20);MenuRow("대출 관리",icon=BankIcons.Bank){open("loan")};MenuRow("카드 관리",icon=BankIcons.Card){open("card")}}
        "certificates"->Page("증명서","certificates",back=back){
            Space(8);Headline("필요한 서류를\n바로 확인하세요");Space(18)
            listOf("잔액 확인서","계좌 확인서","거래내역 확인서","보유 자산 확인서","보험 납입 확인서").forEach{kind->MenuRow(kind,icon=BankIcons.History){
                selected=kind
                model.act{val text="새온은행 × TRACE\n시연용 $kind\n\n예금주 ${pref.displayName}\n계좌 110-***-0001\n생활통장 ${won(state.balance)}원\n모아적금 ${won(state.savings)}원\n발급 시각 ${dateLabel(model.repository.clock.now())}\n\n가상 데이터로 작성된 시연용 문서이며 증빙 효력이 없습니다."
                    withContext(Dispatchers.IO){model.services.document(kind,text)};open("document")}
            }}
            MenuRow("송금 확인증","완료된 송금 내역에서 선택",BankIcons.Transfer){open("history")}
            Space(12);Caption("발급 문서는 이 기기에 저장됩니다. 실제 금융기관의 증명서가 아닙니다.")
        }
        "document"->Page("문서 확인","document",back=back,footer={PrimaryButton("시연 확인서 공유",enabled=services.documents.isNotEmpty()){services.documents.firstOrNull()?.let{share(it.text)}}}){
            val doc=services.documents.firstOrNull()
            if(doc==null)EmptyState("발급한 문서가 없어요.","증명서에서 필요한 서류를 선택해 주세요.")
            else{Space(16);SurfaceBox{TraceSignature("새온은행");Space(24);Text(doc.kind,style=MaterialTheme.typography.headlineSmall);Space(24);Text(doc.text,style=MaterialTheme.typography.bodyMedium);Space(20);Caption("문서번호 ${doc.id}")}}
        }
        "card_service"->Page("카드 분실·재발급","card_service",back=back){
            Space(16);AppIcon(BankIcons.Card,size=42);Space(20);Headline(if(services.cardLost)"분실 신고로\n카드를 정지했어요"else"새온 체크카드");Space(12);Caption("끝자리 4201 · ${pref.displayName}");Space(26)
            DetailRow("카드 상태",if(services.cardLost)"분실 신고 · 사용 정지"else if(pref.cardFrozen)"일시 정지"else"사용 가능")
            Space(18)
            PrimaryButton(if(services.cardLost)"카드를 찾았어요"else"분실 신고하고 정지",Modifier.testTag("card_loss")){confirm=true}
            if(services.cardLost){Space(12);SecondaryButton(if(services.replacementRequested)"재발급 요청 내역 보기"else"재발급 요청"){
                model.act{withContext(Dispatchers.IO){model.services.replacement()};open("replacement")}
            }}
            Space(22);Caption("시연에서는 카드 상태와 요청 기록만 바뀝니다. 실제 카드를 정지하거나 발급하지 않습니다.")
        }
        "replacement"->Page("재발급 요청","replacement",back=back,footer={PrimaryButton("확인"){back()}}){Space(30);ResultSeal();Space(24);Headline("재발급 요청을\n기록했어요");Space(20);DetailRow("카드","새온 체크카드");DetailRow("처리 상태","시연 접수 완료");Space(20);Body("배송이나 실제 발급은 진행하지 않습니다. 카드를 찾으면 분실 신고를 해제할 수 있어요.",subdued=true)}
        "account_protection"->Page("계좌 보호","account_protection",back=back){
            Space(20);AppIcon(BankIcons.Lock,size=40,tint=TraceColors.Ink);Space(22)
            Headline(if(state.accountLocked)"송금을 잠가\n계좌를 보호하고 있어요"else"걱정되는 순간에는\n송금을 잠그세요")
            Space(16);Body("잔액과 내역은 확인할 수 있어요. 잠금 중에는 인증을 받아도 송금되지 않습니다.",subdued=true)
            Space(24);DetailRow("현재 상태",if(state.accountLocked)"송금 잠금"else"송금 가능")
            Space(20);PrimaryButton(if(state.accountLocked)"잠금 해제 확인"else"송금 잠그기",Modifier.testTag("account_lock")){confirm=true}
            Space(16);MenuRow("피해 상황 기록",icon=BankIcons.Shield){category="피해 상황";open("inquiry")};MenuRow("안전 확인 가이드",icon=BankIcons.Info){open("safety_guide")}
            Space(16);Caption("실제 계좌 지급정지 기능이 아닌, 이 앱의 가상 송금을 차단하는 기능입니다.")
        }
        "recurring"->Page("자동이체","recurring",back=back,actions={IconAction(BankIcons.Edit,"자동이체 추가"){open("schedule_new")}}){
            Space(12);Headline("정해둔 날에,\n빠짐없이");Space(10);Caption("자동 출금되지 않는 로컬 시연 예약입니다.");Space(20)
            services.schedules.forEach{s->
                Column(Modifier.animateContentSize()){OptionRow(s.title,s.active,"매월 ${s.day}일 · ${won(s.amount)}원"){
                    model.act{withContext(Dispatchers.IO){model.services.toggleSchedule(s.id)}}
                };QuietButton("예약 삭제",Modifier.testTag("schedule_delete_${s.id}")){model.act{withContext(Dispatchers.IO){model.services.deleteSchedule(s.id)}}};Rule()}
            }
            if(services.schedules.isEmpty())EmptyState("예약된 이체가 없어요.","받는 곳과 날짜를 정해 보세요.")
            Space(20);PrimaryButton("자동이체 추가",Modifier.testTag("schedule_add")){open("schedule_new")}
        }
        "schedule_new"->Page("자동이체 추가","schedule_new",back=back,footer={PrimaryButton("예약 저장",Modifier.testTag("schedule_save"),enabled=title.trim().isNotEmpty()&&(amount.toLongOrNull()?:0) in 1..100_000_000&&(day.toIntOrNull()?:0) in 1..28){
            model.act{withContext(Dispatchers.IO){model.services.schedule(title,amount.toLong(),day.toInt())};back()}
        }}){Space(16);Headline("새로운 예약을\n만들어요");Space(22);Field(title,"받는 곳 또는 예약 이름",{title=it.take(24)},Modifier.testTag("schedule_title"));Space(16);Field(amount,"금액",{amount=it.filter(Char::isDigit).take(9)},Modifier.testTag("schedule_amount"),keyboard=KeyboardType.Number);Space(16);Field(day,"매월 이체일 (1–28일)",{day=it.filter(Char::isDigit).take(2)},Modifier.testTag("schedule_day"),keyboard=KeyboardType.Number);Space(20);Caption("예약 정보만 저장하며 실제 출금은 하지 않습니다.")}
        "support"->Page("고객센터","support",back=back){
            Space(10);Headline("어떤 도움이\n필요하세요?");Space(22)
            SurfaceBox{Text("계좌가 걱정되시나요?",style=MaterialTheme.typography.titleMedium);Space(8);Body("먼저 송금을 잠그고 상황을 확인하세요.",subdued=true);Space(12);PrimaryButton("계좌 보호"){open("account_protection")}}
            Space(18);MenuRow("문의 남기기",icon=BankIcons.Phone){open("inquiry")};MenuRow("내 문의 내역",icon=BankIcons.History){open("cases")};MenuRow("잘못 보낸 송금","거래내역에서 송금 확인증 확인",BankIcons.Transfer){open("history")};MenuRow("카드 분실·재발급",icon=BankIcons.Card){open("card_service")};MenuRow("자주 묻는 질문",icon=BankIcons.Info){open("help")};Space(20);Caption("새온은행은 가상 은행입니다. 이 화면에서는 외부 상담이나 신고를 전송하지 않습니다.")
        }
        "inquiry"->Page("문의 남기기","inquiry",back=back,footer={PrimaryButton("문의 기록 저장",Modifier.testTag("inquiry_save"),enabled=body.trim().length in 5..500){model.act{withContext(Dispatchers.IO){model.services.addCase(category,body)};open("cases")}}}){
            Space(16);Headline("상황을 남겨 주세요");Space(20)
            listOf("송금 문의","카드 문의","피해 상황").forEach{type->OptionRow(type,category==type){category=type}}
            Space(16);Field(body,"문의 내용",{body=it.take(500)},Modifier.testTag("inquiry_body"),minLines=4,maxLines=8)
            Space(16);Caption("개인정보·계좌 비밀번호를 입력하지 마세요. 내용은 이 기기에만 저장되며 실제 신고·상담 접수가 아닙니다.")
        }
        "cases"->Page("내 문의 내역","cases",back=back){Space(12)
            if(services.cases.isEmpty())EmptyState("남겨둔 문의가 없어요.","고객센터에서 상황을 기록할 수 있어요.")
            services.cases.forEach{c->SurfaceBox{Text(c.category,style=MaterialTheme.typography.titleMedium);Space(6);Caption(dateLabel(c.createdAt));Space(12);Body(c.body);Space(12);Caption("기기 내 저장 · ${c.id}")};Space(14)}
            Space(18);SecondaryButton("문의 남기기"){open("inquiry")}
        }
        "terms"->Page("이용 안내","terms",back=back){Space(16);Headline("시연 환경 안내");Space(22);Body("새온은행은 TRACE의 송금 보호 경험을 보여주기 위한 가상 은행입니다.");Space(20);SectionTitle("금융 거래");Body("표시되는 잔액·계좌·거래·상품은 모두 가상 데이터입니다. 실제 돈이 이동하지 않습니다.",subdued=true);Space(20);SectionTitle("위험 판단");Body("로컬 정책 엔진으로 시나리오를 평가합니다. 실제 TRACE 학습 가중치나 금융기관 서버와 연결되어 있지 않습니다.",subdued=true);Space(20);SectionTitle("기기 내 저장");Body("거래 내역과 설정, 직접 작성한 문의는 이 기기에 저장됩니다. 앱을 제거하면 삭제됩니다. 시연센터에서 장면을 바꾸면 가상 거래 상태를 초기화합니다.",subdued=true)}
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text(if(route=="account_protection")if(state.accountLocked)"송금 잠금을 해제할까요?"else"송금을 잠글까요?"else if(services.cardLost)"분실 신고를 해제할까요?"else"분실 신고하고 정지할까요?")},
        text={Text(if(route=="account_protection")"이 가상 계좌의 송금 상태를 변경합니다. 실제 은행 계좌에는 영향을 주지 않습니다."else"이 앱의 가상 카드 상태를 변경합니다. 실제 카드사에는 접수되지 않습니다.")},
        confirmButton={QuietButton("확인",Modifier.testTag("service_confirm")){
            model.act{if(route=="account_protection")model.repository.change{s,_->s.copy(accountLocked=!s.accountLocked)}else{
                val lost=!services.cardLost
                withContext(Dispatchers.IO){model.services.cardLost(lost)};model.graph.preferences.cardFrozen(lost)
            };confirm=false}
        }},dismissButton={QuietButton("취소"){confirm=false}},containerColor=TraceColors.Surface)
}
