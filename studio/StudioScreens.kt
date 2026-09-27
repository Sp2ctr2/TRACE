package app.saeon.trace.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.BankViewModel
import app.saeon.trace.ui.InteractionState
import app.saeon.trace.ui.design.*

/** Banking shell. The transaction engine remains the sole owner of balances and decisions. */
@Composable
fun StudioHome(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String)->Unit) {
    val sendPress=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}
    val bringPress=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}
    TaskPage("새온은행", "home", root=true, actions={
        IconAction(BankIcons.Bell,"알림"){open("notifications")}
        IconAction(BankIcons.Profile,"내 정보"){open("profile")}
    }) {
        val compact=LocalTaskHeight.current<500.dp
        val gap=if(compact)8 else 16
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(TraceColors.Surface)
            .padding(horizontal=18.dp,vertical=if(compact)10.dp else 14.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(role=Role.Button){open("account")},verticalArrangement=Arrangement.Center) {
                    Text("새온 생활통장",style=MaterialTheme.typography.titleSmall)
                    Text("110-***-0001",style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
                }
                IconAction(BankIcons.Eye,if(preferences.hideBalance)"잔액 보이기"else"잔액 숨기기"){
                    model.preference{hideBalance(!preferences.hideBalance)}
                }
            }
            if(preferences.hideBalance) Text("잔액 숨김",Modifier.padding(vertical=4.dp),style=MaterialTheme.typography.headlineLarge)
            else AnimatedDigits(state.balance,Modifier.fillMaxWidth().padding(vertical=4.dp).testTag("home_balance"),fontSize=if(compact)30.sp else 34.sp)
            Space(if(compact)8 else 14)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={open("transfer")},interactionSource=sendPress,modifier=Modifier.weight(1.4f).heightIn(min=50.dp).tracePress(sendPress).testTag("home_transfer"),
                    shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=TraceColors.Ink,contentColor=TraceColors.Paper)) {
                    Text("송금",style=MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(onClick={open("bring")},interactionSource=bringPress,modifier=Modifier.weight(1f).heightIn(min=50.dp).tracePress(bringPress).testTag("home_bring"),
                    shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.filledTonalButtonColors(containerColor=TraceColors.Paper,contentColor=TraceColors.Ink)) {
                    Text("가져오기",style=MaterialTheme.typography.labelLarge)
                }
            }
        }
        Space(gap)
        Row(Modifier.fillMaxWidth().heightIn(min=if(compact)52.dp else 64.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(role=Role.Button){open("card")}.heightIn(min=48.dp),verticalArrangement=Arrangement.Center) {
                Caption("이번 달 쓴 돈");Text("382,400원",style=MaterialTheme.typography.titleMedium.copy(fontSize=18.sp))
            }
            Box(Modifier.width(1.dp).height(28.dp).background(TraceColors.Divider))
            Column(Modifier.weight(1f).padding(start=22.dp).clickable(role=Role.Button){open("savings")}.heightIn(min=48.dp),verticalArrangement=Arrangement.Center) {
                Caption("모아둔 돈");Text("${won(state.savings)}원",style=MaterialTheme.typography.titleMedium.copy(fontSize=18.sp))
            }
        }
        Space(if(compact)4 else 12)
        Row(Modifier.fillMaxWidth().heightIn(min=40.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("최근 거래",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
            QuietButton("전체",Modifier.testTag("home_history")){open("history")}
        }
        state.receipts.take(if(compact)1 else 2).forEach { r ->
            Row(Modifier.fillMaxWidth().heightIn(min=if(compact)48.dp else 54.dp).clickable(role=Role.Button){open("receipt/${r.id}")},
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(34.dp).background(TraceColors.Surface,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){
                    AppIcon(if(r.direction==Direction.CREDIT)BankIcons.Download else BankIcons.Card,size=17,tint=TraceColors.Muted)
                }
                Column(Modifier.weight(1f)) {
                    Text(r.recipient.name,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
                    if(!compact)Text(r.memo.ifBlank{r.purpose.label},style=MaterialTheme.typography.labelSmall,color=TraceColors.Muted)
                }
                Text("${if(r.direction==Direction.DEBIT)"−"else"+"}${won(r.amount)}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
            }
        }
        if(!compact) {
            Space(8)
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(role=Role.Button){open("recurring")},verticalAlignment=Alignment.CenterVertically) {
                AppIcon(BankIcons.Calendar,size=18,tint=TraceColors.Muted)
                Text("  다음 자동이체",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
                Text(if(state.recurringEnabled)"통신비 68,000원"else"예정 내역 보기",style=MaterialTheme.typography.bodySmall)
                AppIcon(BankIcons.Chevron,size=14,tint=TraceColors.Muted)
            }
        }
        Space(if(compact)4 else 10)
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).background(TraceColors.Surface,RoundedCornerShape(16.dp))
            .clickable(role=Role.Button){open("safety")}.padding(horizontal=14.dp,vertical=8.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            AppIcon(BankIcons.Trace,size=26,tint=TraceColors.Coral)
            Text(if(state.accountLocked)"계좌를 보호하고 있어요"else if(state.pending.isNotEmpty())"보류한 송금 ${state.pending.size}건"else"보내기 전, 한 번 더.",
                Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
            AppIcon(BankIcons.Chevron,size=15,tint=TraceColors.Muted)
        }
    }
}

@Composable
fun StudioAssets(state:BankState,open:(String)->Unit) {
    Page("자산","assets",actions={IconAction(BankIcons.History,"전체 거래내역"){open("history")}}) {
        Space(8); Caption("내가 보유한 자산")
        Space(8); MotionMoney(state.balance+state.savings+1_840_000,Modifier.fillMaxWidth())
        Space(10); Caption("대출을 뺀 순자산 ${won(state.balance+state.savings+1_840_000-state.loanBalance)}원")
        Space(26)
        StudioAssetRow("입출금","새온 생활통장",state.balance,BankIcons.Bank){open("account")}
        StudioAssetRow("예·적금","모아적금",state.savings,BankIcons.Assets){open("savings")}
        StudioAssetRow("투자","보유 자산 2개",1_840_000,BankIcons.History){open("investments")}
        Space(18);Rule();Space(16)
        StudioAssetRow("카드","이번 달 사용 금액",382_400,BankIcons.Card){open("card")}
        StudioAssetRow("대출","생활안심대출 잔액",state.loanBalance,BankIcons.Bank){open("loan")}
        Space(18);Rule();Space(12)
        MenuRow("보험","계약과 납입 내역",BankIcons.Shield){open("insurance")}
        MenuRow("신용 정보","시연 신용 현황",BankIcons.Profile){open("credit")}
    }
}
@Composable private fun StudioAssetRow(title:String,caption:String,value:Long,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=76.dp).clickable(role=Role.Button,onClick=onClick),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(42.dp).background(TraceColors.Surface,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){AppIcon(icon,size=21,tint=TraceColors.Ink)}
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleSmall);Caption(caption)}
        Text("${won(value)}원",style=MaterialTheme.typography.titleSmall);AppIcon(BankIcons.Chevron,size=14,tint=TraceColors.Muted)
    }
}

@Composable
fun StudioReview(state:BankState,record:TransferRecord,interaction:InteractionState,model:BankViewModel,open:(String)->Unit,back:()->Unit) {
    val tx=record.intent
    val error=runCatching{BankEngine.validateAmount(state,tx.amount,tx.purpose,model.repository.clock.now())}.exceptionOrNull()?.message
    TaskPage("송금 확인","transfer_review",back=back,footer={
        Row(Modifier.fillMaxWidth().heightIn(min=28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
            AppIcon(BankIcons.Trace,size=17,tint=TraceColors.Coral)
            Text("보내기 전 맥락까지 확인해요",Modifier.padding(start=6.dp),style=MaterialTheme.typography.labelMedium,color=TraceColors.Muted)
        }
        PrimaryButton("${won(tx.amount)}원 보내기",Modifier.testTag("transfer_confirm"),enabled=error==null&&!interaction.busy&&!state.accountLocked&&record.stage==TransferStage.REVIEW){model.requestAuthorization(tx.id)}
    }) {
        val compact=LocalTaskHeight.current<500.dp
        Space(if(compact)0 else 18)
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
            PersonBadge(tx.recipient.name,if(compact)38 else 48,false)
            Space(if(compact)6 else 14)
            RecipientTitle(tx.recipient.id,tx.recipient.name)
            Space(3)
            Box(Modifier.traceShared("amount/${tx.recipient.id}/${tx.amount}")){AnimatedDigits(tx.amount,fontSize=if(compact)30.sp else 36.sp)}
            Text("보낼까요?",style=MaterialTheme.typography.titleLarge.copy(fontSize=if(compact)20.sp else 23.sp,lineHeight=30.sp,letterSpacing=(-.4).sp))
        }
        Space(if(compact)14 else 28)
        Column(Modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(20.dp)).padding(horizontal=16.dp,vertical=8.dp)) {
            StudioDetail("받는 계좌",tx.recipient.bank,tx.recipient.account)
            Rule();StudioDetail("출금 계좌","새온 생활통장","110-***-0001")
            Rule();StudioDetail("목적 · 수수료","${tx.purpose.label} · 0원")
        }
        QuietButton("금액·목적 수정",Modifier.fillMaxWidth().testTag("review_edit"),enabled=!interaction.busy&&record.stage==TransferStage.REVIEW){model.editReview(tx.id){open("amount")}}
        if(state.accountLocked) ErrorNote("계좌가 잠겨 있어 송금할 수 없어요. 계좌 보호에서 확인해 주세요.")
        if(tx.officialRouteId!=null)Caption("공식 경로로 바뀐 새 거래입니다. 다시 인증해 주세요.")
        error?.let{ErrorNote(it)};record.error?.takeIf{it!=error}?.let{ErrorNote(it)}
    }
}
@Composable private fun StudioDetail(label:String,value:String,secondary:String?=null) {
    Row(Modifier.fillMaxWidth().heightIn(min=if(secondary==null)46.dp else 58.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
        Text(label,Modifier.weight(.85f),style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
        Column(Modifier.weight(1.5f),horizontalAlignment=Alignment.End){
            Text(value,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium,textAlign=TextAlign.End)
            if(secondary!=null)Text(secondary,style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
        }
    }
}

@Composable
fun StudioMore(preferences:BankPreferences,open:(String)->Unit) {
    Page("전체","settings",actions={IconAction(BankIcons.Settings,"화면 설정"){open("appearance")}}){
        MenuRow("${preferences.displayName}님","내 정보와 설정",BankIcons.Profile){open("profile")}
        Space(14)
        Row(Modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(20.dp)).padding(6.dp)) {
            listOf(Triple("증명서","certificates",BankIcons.History),Triple("고객센터","support",BankIcons.Phone),Triple("계좌 보호","account_protection",BankIcons.Lock)).forEach{(label,path,icon)->
                Column(Modifier.weight(1f).heightIn(min=88.dp).clickable(role=Role.Button){open(path)},verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
                    AppIcon(icon,size=23,tint=TraceColors.Ink);Space(8);Text(label,style=MaterialTheme.typography.labelMedium)
                }
            }
        }
        Space(24);SectionTitle("내 금융")
        MenuRow("송금 설정",icon=BankIcons.Transfer){open("transfer_settings")}
        MenuRow("자주 쓰는 계좌",icon=BankIcons.Profile){open("favorites")}
        MenuRow("자동이체",icon=BankIcons.Calendar){open("recurring")}
        MenuRow("카드 분실·재발급",icon=BankIcons.Card){open("card_service")}
        Space(16);Rule();Space(16);SectionTitle("설정과 도움")
        MenuRow("보안 및 인증",icon=BankIcons.Lock){open("security")}
        MenuRow("알림",icon=BankIcons.Bell){open("notifications")}
        MenuRow("개인정보",icon=BankIcons.Shield){open("privacy")}
        MenuRow("화면 설정",icon=BankIcons.Settings,tag="appearance_open"){open("appearance")}
        MenuRow("접근성",icon=BankIcons.Settings){open("accessibility")}
        MenuRow("도움말",icon=BankIcons.Info){open("help")}
        MenuRow("이용 안내 및 약관",icon=BankIcons.History){open("terms")}
        MenuRow("앱 정보",icon=BankIcons.More,tag="app_info_open"){open("app_info")}
    }
}

@Composable fun StudioVerify(record:TransferRecord,interaction:InteractionState,model:BankViewModel,back:()->Unit,home:()->Unit) {
    TaskPage("상환 경로 확인","trace_verify",back=back,actions={AppIcon(BankIcons.Trace,size=23,tint=TraceColors.Coral)},footer={
        PrimaryButton(if(interaction.routeLoading)"공식 경로 확인 중…"else"공식 상환 경로 확인",Modifier.testTag("verify_route"),enabled=!interaction.busy){model.resolveRoute(record.intent.id)}
        SecondaryButton("송금 취소"){model.cancelTransfer(record.intent.id,home)}
    }){
        val compact=LocalTaskHeight.current<470.dp
        if(!compact){Box(Modifier.size(48.dp).background(TraceColors.Surface,RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center){AppIcon(BankIcons.Bank,size=25)};Space(16)}
        TaskHeadline("대출 상환은\n은행에 등록된 경로로","상환 경로를 확인하세요")
        TaskGap(12);Body("안내받은 곳은 개인 계좌예요.\n상환 목적과 받는 곳이 다릅니다.",subdued=true);TaskGap(18)
        Column(Modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(18.dp)).padding(16.dp)){
            Caption("안내받은 개인 계좌");Space(6)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(record.intent.recipient.name,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall);Text("${won(record.intent.amount)}원",style=MaterialTheme.typography.titleSmall)}
            if(!compact){Space(4);Caption("${record.intent.recipient.bank} ${record.intent.recipient.account}")}
        }
        TaskGap(10)
        Row(Modifier.fillMaxWidth().heightIn(min=54.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
            AppIcon(BankIcons.Bank,size=23,tint=TraceColors.Ink)
            Column(Modifier.weight(1f)){Text("은행에 등록된 상환 경로",style=MaterialTheme.typography.titleSmall);Caption("은행의 정보로 다시 확인해요")}
        }
        TaskGap(8);Caption("상대가 준 번호나 링크는 이용하지 않아요.\n아직 돈은 나가지 않았습니다.")
    }
}
