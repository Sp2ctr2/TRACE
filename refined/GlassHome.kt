package app.saeon.trace.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.BankViewModel
import app.saeon.trace.ui.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GlassHome(state:BankState,preferences:BankPreferences,model:BankViewModel,open:(String)->Unit){
    val service by model.services.state.collectAsStateWithLifecycle()
    var picker by rememberSaveable{mutableStateOf(false)}
    TaskPage("새온은행","home",root=true,actions={
        GlassIcon(BankIcons.Bell,"알림"){open("notifications")}
        GlassIcon(BankIcons.Profile,"내 정보"){open("profile")}
    }){
        val compact=LocalTaskHeight.current<510.dp
        val easy=LocalEasyMode.current
        val reduced=LocalReducedMotion.current
        Box(Modifier.heightIn(min=44.dp).testTag("home_account_picker").clickable(role=Role.Button){picker=true},contentAlignment=Alignment.CenterStart){
            GlassPlate(radius=17.dp){
                Row(Modifier.padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    Text("새온 생활통장",style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.Medium)
                    AppIcon(BankIcons.Chevron,size=12,tint=TraceColors.Muted)
                }
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("home_balance"),verticalAlignment=Alignment.CenterVertically){
            AnimatedContent(preferences.hideBalance,transitionSpec={if(reduced)EnterTransition.None togetherWith ExitTransition.None else fadeIn(tween(180)) togetherWith fadeOut(tween(120))},label="balance-privacy",modifier=Modifier.weight(1f)) { hidden ->
                Row(verticalAlignment=Alignment.Bottom){
                    if(hidden)Text("잔액 숨김",style=MaterialTheme.typography.displaySmall.copy(fontSize=if(compact)30.sp else 36.sp,lineHeight=44.sp,fontWeight=FontWeight.SemiBold))
                    else{
                        Text(won(state.balance),style=MaterialTheme.typography.displaySmall.copy(fontSize=if(compact)32.sp else 39.sp,lineHeight=44.sp,letterSpacing=(-1.25).sp,fontWeight=FontWeight.SemiBold,fontFeatureSettings="tnum"))
                        Text("원",Modifier.padding(start=5.dp,bottom=5.dp),style=MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            GlassIcon(BankIcons.Eye,if(preferences.hideBalance)"잔액 보이기"else"잔액 숨기기",Modifier.testTag("home_hide_balance").semantics{stateDescription=if(preferences.hideBalance)"숨김"else"표시 중"}){
                model.preference{hideBalance(!preferences.hideBalance)}
            }
        }
        if(!compact){Caption("110-***-0001");Space(8)}
        Space(if(compact)4 else 4)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Box(Modifier.weight(1f)){GlassAction("가져오기",Modifier.testTag("home_bring"),smoked=true){open("bring")}}
            Box(Modifier.weight(1f)){GlassAction(if(easy)"돈 보내기"else"송금하기",Modifier.testTag("home_transfer")){open("transfer")}}
        }
        if(easy){
            Space(12)
            HomeEasyAction("거래 내역","보낸 돈과 받은 돈",BankIcons.History){open("history")}
            HomeEasyAction("내 자산","통장·적금·카드",BankIcons.Assets){open("assets")}
            HomeEasyAction("도움 받기","고객센터와 계좌 보호",BankIcons.Phone){open("support")}
        }else{
            Space(if(compact)4 else 12)
            HomeSection("내 금융","전체 보기"){open("assets")}
            HomeAsset("새온 생활통장","입출금",state.balance,BankIcons.Bank,preferences.hideBalance){open("account")};HomeSeparator()
            HomeAsset("모아적금","예·적금",state.savings,BankIcons.Assets,preferences.hideBalance){open("savings")};HomeSeparator()
            HomeAsset("투자","보유 자산 2개",1_840_000,BankIcons.History,preferences.hideBalance){open("investments")}
            if(!compact){
                Space(8);HomeSeparator();HomeSection("최근 거래","더보기"){open("history")}
                state.receipts.firstOrNull()?.let{r->
                    Row(Modifier.fillMaxWidth().heightIn(min=50.dp).clickable(role=Role.Button){open("receipt/${r.id}")},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        HomeSmallIcon(if(r.direction==Direction.CREDIT)BankIcons.Download else BankIcons.Card)
                        Column(Modifier.weight(1f)){Text(r.recipient.name,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium);Caption(dateLabel(r.completedAt))}
                        Text(if(preferences.hideBalance)"숨김"else"${if(r.direction==Direction.CREDIT)"+"else"−"}${won(r.amount)}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
                    }
                }
                if(LocalTaskHeight.current>=600.dp){
                    Space(8);HomeSeparator();HomeSection("다음 일정","관리"){open("recurring")}
                    val next=service.schedules.firstOrNull{it.active}
                    Row(Modifier.fillMaxWidth().heightIn(min=50.dp).clickable(role=Role.Button){open("recurring")},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        HomeSmallIcon(BankIcons.Calendar)
                        Column(Modifier.weight(1f)){Text(next?.let{"${it.title} 자동이체"}?:"예정된 이체 없음",style=MaterialTheme.typography.bodyMedium);Caption(next?.let{"매월 ${it.day}일"}?:"자동이체 관리")}
                        if(next!=null)Text(if(preferences.hideBalance)"숨김"else"${won(next.amount)}원",style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
                    }
                }
            }
        }
    }
    if(picker)ModalBottomSheet(onDismissRequest={picker=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=TraceColors.Surface){
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp).padding(bottom=24.dp)){
            Text("내 계좌",style=MaterialTheme.typography.headlineSmall);Space(14)
            MenuRow("새온 생활통장",if(preferences.hideBalance)"잔액 숨김"else"${won(state.balance)}원",BankIcons.Bank){picker=false;open("account")}
            MenuRow("모아적금",if(preferences.hideBalance)"잔액 숨김"else"${won(state.savings)}원",BankIcons.Assets){picker=false;open("savings")}
            SecondaryButton("닫기"){picker=false}
        }
    }
}
@Composable private fun HomeSection(text:String,action:String,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=40.dp),verticalAlignment=Alignment.CenterVertically){
        Text(text,Modifier.weight(1f).semantics{heading()},style=MaterialTheme.typography.titleSmall)
        TextButton(onClick=onClick,contentPadding=PaddingValues(horizontal=4.dp),modifier=Modifier.heightIn(min=48.dp)){Text(action,style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)}
    }
}
@Composable private fun HomeSmallIcon(icon:ImageVector){Box(Modifier.size(34.dp).background(TraceColors.Ink.copy(alpha=.035f),RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){AppIcon(icon,size=18,tint=TraceColors.Muted)}}
@Composable private fun HomeSeparator(){HorizontalDivider(color=TraceColors.Ink.copy(alpha=.08f),thickness=.7.dp)}
@Composable private fun HomeAsset(title:String,subtitle:String,amount:Long,icon:ImageVector,hidden:Boolean,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=50.dp).clickable(role=Role.Button,onClick=onClick),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        HomeSmallIcon(icon)
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium);Text(subtitle,style=MaterialTheme.typography.labelSmall,color=TraceColors.Muted)}
        Text(if(hidden)"숨김"else"${won(amount)}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
    }
}
@Composable private fun HomeEasyAction(title:String,description:String,icon:ImageVector,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=76.dp).clickable(role=Role.Button,onClick=onClick),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
        AppIcon(icon,size=27);Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium);Caption(description)};AppIcon(BankIcons.Chevron,size=18)
    };HomeSeparator()
}
