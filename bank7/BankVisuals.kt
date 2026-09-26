@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.saeon.trace.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.*
import app.saeon.trace.ui.design.*

@Composable fun BankAmount(value:Long,modifier:Modifier=Modifier,center:Boolean=false,maximum:Float=38f) {
    val scale=LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth(),contentAlignment=if(center)Alignment.Center else Alignment.CenterStart) {
        val size=(maxWidth.value/(scale*(won(value).length*.61f+1.1f))).coerceIn(18f,maximum)
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontSize=size.sp,fontWeight=FontWeight.SemiBold)){append(won(value))}
            withStyle(SpanStyle(fontSize=(size*.57f).sp,fontWeight=FontWeight.Medium)){append(" 원")}
        },style=MaterialTheme.typography.displaySmall.copy(lineHeight=(size*1.28f).sp,letterSpacing=(-.45).sp,fontFeatureSettings="tnum"),
            color=TraceColors.Ink,modifier=Modifier.semantics{contentDescription="${won(value)}원"})
    }
}

@Composable fun BankHome(state:BankState,p:BankPreferences,model:BankViewModel,open:(String)->Unit) {
    TaskPage("새온은행","home",root=true,actions={
        IconAction(BankIcons.Bell,"알림",Modifier.testTag("home_notifications")){open("notifications")}
        IconAction(BankIcons.Profile,"내 정보",Modifier.testTag("home_profile")){open("profile")}
    }) {
        val compact=LocalTaskHeight.current<465.dp
        val short=LocalTaskHeight.current<570.dp
        Column(Modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(24.dp))
            .padding(horizontal=18.dp,vertical=if(compact)10.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(role=Role.Button){open("account")},verticalArrangement=Arrangement.Center) {
                    Text("새온 생활통장",style=MaterialTheme.typography.titleSmall)
                    if(!compact)Text("110-***-0001",style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
                }
                IconAction(BankIcons.Eye,if(p.hideBalance)"잔액 보이기"else"잔액 숨기기"){model.preference{hideBalance(!p.hideBalance)}}
            }
            if(p.hideBalance)Text("잔액 숨김",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.heightIn(min=44.dp))
            else BankAmount(state.balance,Modifier.testTag("home_balance"))
            Space(if(compact)8 else 16)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1.45f)){PrimaryButton("송금",Modifier.testTag("home_transfer")){open("transfer")}}
                Box(Modifier.weight(1f)){SecondaryButton("가져오기",Modifier.testTag("home_bring")){open("bring")}}
            }
        }
        Space(if(compact)6 else 14)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
            listOf(Triple("이번 달 쓴 돈","382,400원","card"),Triple("모아둔 돈","${won(state.savings)}원","savings")).forEach{(label,value,route)->
                Column(Modifier.weight(1f).heightIn(min=if(compact)48.dp else 64.dp).clickable(role=Role.Button){open(route)},verticalArrangement=Arrangement.Center) {
                    Caption(label);Text(value,style=MaterialTheme.typography.titleSmall)
                }
            }
        }
        Space(if(compact)0 else 6)
        Row(Modifier.fillMaxWidth().heightIn(min=if(compact)48.dp else 58.dp).clickable(role=Role.Button){open("safety")},
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            AppIcon(BankIcons.Trace,tint=TraceColors.Coral,size=25)
            Column(Modifier.weight(1f)) {
                Text(if(state.pending.isEmpty())"보내기 전, 한 번 더."else"확인이 필요한 송금 ${state.pending.size}건",style=MaterialTheme.typography.titleSmall)
                if(!compact)Caption(if(state.pending.isEmpty())"송금 앞의 맥락을 확인해요"else"돈을 보내지 않은 상태로 보관 중이에요")
            }
            AppIcon(BankIcons.Chevron,size=15,tint=TraceColors.Muted)
        }
        if(!compact){Space(10);Rule();Space(6)}
        if(compact) {
            val last=state.receipts.firstOrNull()
            BankRow("최근 거래",last?.let{"${it.recipient.name}  ${if(it.direction==Direction.CREDIT)"+"else"−"}${won(it.amount)}원"}?:"아직 거래가 없어요",BankIcons.History,tag="home_recent") { open("history") }
        } else {
            Row(Modifier.fillMaxWidth().heightIn(min=44.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("최근 거래",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                QuietButton("전체",Modifier.testTag("home_history")){open("history")}
            }
            state.receipts.take(if(short)1 else 2).forEach{r->
                Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(role=Role.Button){open("receipt/${r.id}")},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(34.dp).background(TraceColors.Surface,CircleShape),contentAlignment=Alignment.Center){AppIcon(if(r.direction==Direction.CREDIT)BankIcons.Download else BankIcons.Card,size=17,tint=TraceColors.Muted)}
                    Column(Modifier.weight(1f)){Text(r.recipient.name,style=MaterialTheme.typography.bodyMedium);Caption(r.memo.ifEmpty{r.purpose.label})}
                    Text("${if(r.direction==Direction.CREDIT)"+"else"−"}${won(r.amount)}원",style=MaterialTheme.typography.titleSmall)
                }
            }
            if(!short){Space(6);BankRow("다가오는 일정",if(state.recurringEnabled)"통신비 68,000원 · 매월 25일"else"예약 알림이 없어요",BankIcons.Calendar,tag="home_scheduled"){open("schedules")}}
        }
    }
}

@Composable fun BankRow(title:String,subtitle:String?=null,icon:androidx.compose.ui.graphics.vector.ImageVector?=null,tag:String="",trailing:String?=null,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=if(subtitle==null)52.dp else 64.dp).clickable(role=Role.Button,onClick=onClick).testTag(tag).padding(vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        if(icon!=null)AppIcon(icon,size=21,tint=TraceColors.Muted)
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Medium);if(subtitle!=null)Caption(subtitle)}
        if(trailing!=null)Text(trailing,style=MaterialTheme.typography.bodyMedium)
        AppIcon(BankIcons.Chevron,size=15,tint=TraceColors.Muted)
    }
}

@Composable fun BankReview(state:BankState,record:TransferRecord,interaction:InteractionState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val i=record.intent
    val amountError=runCatching{BankEngine.validateAmount(state,i.amount,i.purpose,model.repository.clock.now())}.exceptionOrNull()?.message
    TaskPage("송금 확인","transfer_review",back=back,footer={
        Row(Modifier.fillMaxWidth().heightIn(min=28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
            AppIcon(BankIcons.Trace,size=17,tint=TraceColors.Coral)
            Text("보내기 전 맥락까지 확인해요",Modifier.padding(start=6.dp),style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
        }
        PrimaryButton("${won(i.amount)}원 보내기",Modifier.testTag("transfer_confirm"),enabled=record.stage==TransferStage.REVIEW&&!interaction.busy&&amountError==null){model.requestAuthorization(i.id)}
    }) {
        val compact=LocalTaskHeight.current<425.dp
        Space(if(compact)0 else 18)
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
            if(!compact){PersonBadge(i.recipient.name,46,true);Space(12)}
            RecipientTitle(i.recipient.id,i.recipient.name)
            Space(4)
            BankAmount(i.amount,Modifier.traceShared("amount/${i.recipient.id}/${i.amount}"),center=true,maximum=40f)
            Text("보낼까요?",style=MaterialTheme.typography.headlineSmall.copy(fontWeight=FontWeight.SemiBold,letterSpacing=(-.4).sp))
        }
        Space(if(compact)18 else 28)
        Column(Modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(18.dp)).padding(horizontal=16.dp,vertical=6.dp)) {
            ReviewDetail("받는 계좌","${i.recipient.bank}\n${i.recipient.account}")
            ReviewDetail("출금 계좌","새온 생활통장")
            ReviewDetail("목적 · 수수료","${i.purpose.label} · 0원")
        }
        QuietButton("금액·목적 수정",Modifier.fillMaxWidth().testTag("review_edit"),enabled=record.stage==TransferStage.REVIEW&&!interaction.busy){model.editReview(i.id){open("amount")}}
        if(i.officialRouteId!=null)Caption("받는 곳이 바뀌어 새 거래로 인증해요.")
        amountError?.let{ErrorNote(it)}
        record.error?.takeIf{it!=amountError}?.let{ErrorNote(it)}
    }
}
@Composable private fun ReviewDetail(label:String,value:String) {
    Row(Modifier.fillMaxWidth().heightIn(min=46.dp).padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(label,Modifier.weight(.8f),style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)
        Text(value,Modifier.weight(1.45f),style=MaterialTheme.typography.bodyMedium.copy(lineHeight=22.sp),textAlign=TextAlign.End,fontWeight=FontWeight.Medium)
    }
}

@Composable fun BankAssets(state:BankState,open:(String)->Unit) {
    Page(title="자산",tag="assets") {
        Space(14);Caption("보유 자산");Space(4);BankAmount(state.balance+state.savings)
        Space(8);Caption("대출 ${won(state.loanBalance)}원은 별도로 표시해요.")
        Space(26)
        SurfaceBox {
            Text("입출금",style=MaterialTheme.typography.titleSmall)
            BankRow("새온 생활통장","바로 보낼 수 있는 돈",BankIcons.Bank,trailing="${won(state.balance)}원"){open("account")}
        }
        Space(12)
        BankRow("모아적금","목표를 향해 차곡차곡",BankIcons.Assets,trailing="${won(state.savings)}원"){open("savings")}
        BankRow("새온 체크카드","이번 달 382,400원 사용",BankIcons.Card){open("card")}
        BankRow("생활안심대출","남은 원금 ${won(state.loanBalance)}원",BankIcons.Bank){open("loan")}
        Space(18);Rule();Space(12)
        SectionTitle("함께 관리하기")
        BankRow("소비 분석","이번 달 지출과 예산",BankIcons.History){open("spending")}
        BankRow("모으는 목표","저축 목표와 달성률",BankIcons.Assets){open("goals")}
        BankRow("투자 관심목록","보유 투자금 0원 · 모의 관심목록",BankIcons.Search){open("investments")}
        BankRow("신용·보험","등록한 메모와 서류 관리",BankIcons.Shield){open("financial_notes")}
    }
}

@Composable fun BankMore(p:BankPreferences,open:(String)->Unit) {
    var query by rememberSaveable{mutableStateOf("")}
    val groups=listOf(
        "내 금융" to listOf(Triple("내 정보","profile",BankIcons.Profile),Triple("계좌 관리","account",BankIcons.Bank),Triple("송금 확인증·증명서","documents",BankIcons.History),Triple("예약·자동이체 관리","schedules",BankIcons.Calendar)),
        "안전과 관리" to listOf(Triple("보안 및 인증","security",BankIcons.Lock),Triple("계정 잠금","account_lock",BankIcons.Lock),Triple("카드 분실·재발급","card_service",BankIcons.Card),Triple("송금 한도·자주 쓰는 계좌","transfer_settings",BankIcons.Transfer),Triple("피해 신고·보호 안내","report",BankIcons.Shield)),
        "설정과 도움" to listOf(Triple("알림","notifications",BankIcons.Bell),Triple("화면 설정","appearance",BankIcons.Settings),Triple("접근성","accessibility",BankIcons.Eye),Triple("개인정보","privacy",BankIcons.Shield),Triple("고객센터","support",BankIcons.Phone),Triple("자주 묻는 질문","help",BankIcons.Info),Triple("시연 약관","terms",BankIcons.History),Triple("앱 정보","app_info",BankIcons.More))
    )
    Page("전체","settings") {
        Space(8);Text("${p.displayName}님",style=MaterialTheme.typography.headlineSmall);Space(18)
        Field(query,"메뉴 검색",{query=it},Modifier.testTag("menu_search"));Space(20)
        var shown=0
        groups.forEach{(title,items)->
            val matches=items.filter{query.isBlank()||it.first.contains(query)}
            if(matches.isNotEmpty()) {
                SectionTitle(title)
                matches.forEach{(label,route,icon)->shown++;BankRow(label,icon=icon,tag="menu_$route"){open(route)}}
                Space(16);Rule();Space(16)
            }
        }
        if(shown==0)EmptyState("찾는 메뉴가 없어요.","송금, 카드, 증명서처럼 기능 이름으로 검색해 주세요.")
    }
}
