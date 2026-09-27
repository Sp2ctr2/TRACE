package app.saeon.trace.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
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
        GlassIcon(BankIcons.Bell,"알림",Modifier.padding(end=6.dp)){open("notifications")}
        GlassIcon(BankIcons.Profile,"내 정보"){open("profile")}
    }){
        val compact=LocalTaskHeight.current<500.dp
        val easy=LocalEasyMode.current
        val rowHeight=if(compact)48 else 54
        Row(Modifier.fillMaxWidth().padding(top=if(compact)0.dp else 6.dp),verticalAlignment=Alignment.CenterVertically){
            GlassPlate(Modifier.heightIn(min=48.dp).clickable(role=Role.Button){picker=true}.testTag("home_account_picker"),radius=24.dp){
                Row(Modifier.padding(horizontal=14.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("새온 생활통장",style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.Medium)
                    AppIcon(BankIcons.Chevron,size=13,tint=TraceColors.Muted)
                }
            }
            Spacer(Modifier.weight(1f))
            GlassIcon(BankIcons.Eye,if(preferences.hideBalance)"잔액 보이기"else"잔액 숨기기",Modifier.testTag("home_hide_balance")){
                model.preference{hideBalance(!preferences.hideBalance)}
            }
        }
        Space(if(compact)8 else 14)
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("home_balance"),verticalAlignment=Alignment.Bottom){
            if(preferences.hideBalance){
                Text("잔액 숨김",Modifier.weight(1f),style=MaterialTheme.typography.displaySmall.copy(fontSize=if(compact)30.sp else 38.sp))
            }else{
                Text(won(state.balance),style=MaterialTheme.typography.displaySmall.copy(fontSize=if(compact)33.sp else 41.sp,lineHeight=46.sp,letterSpacing=(-1.8).sp,fontWeight=FontWeight.SemiBold,fontFeatureSettings="tnum"))
                Text("원",Modifier.padding(start=6.dp,bottom=6.dp),style=MaterialTheme.typography.bodyMedium)
            }
        }
        if(!compact){Space(3);Caption("110-***-0001")}
        Space(if(compact)10 else 16)
        GlassAction(if(easy)"돈 보내기"else"송금하기",Modifier.testTag("home_transfer")){open("transfer")}
        if(easy){
            Space(16)
            EasyHomeAction("거래 내역","보낸 돈과 받은 돈",BankIcons.History){open("history")}
            EasyHomeAction("내 자산","통장·적금·카드",BankIcons.Assets){open("assets")}
            EasyHomeAction("도움 받기","계좌 보호와 고객센터",BankIcons.Phone){open("support")}
        }else{
            Space(if(compact)8 else 20)
            HomeHeading("내 금융","전체 보기"){open("assets")}
            HomeMoneyRow("새온 생활통장","입출금",state.balance,BankIcons.Bank,rowHeight){open("account")};HomeRule()
            HomeMoneyRow("모아적금","예·적금",state.savings,BankIcons.Assets,rowHeight){open("savings")};HomeRule()
            HomeMoneyRow("투자","보유 자산 2개",1_840_000,BankIcons.History,rowHeight){open("investments")}
            if(!compact){
                Space(10);HomeRule()
                HomeHeading("최근 거래","더보기"){open("history")}
                state.receipts.firstOrNull()?.let{r->
                    Row(Modifier.fillMaxWidth().heightIn(min=52.dp).clickable(role=Role.Button){open("receipt/${r.id}")},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        HomeIcon(if(r.direction==Direction.CREDIT)BankIcons.Download else BankIcons.Card)
                        Column(Modifier.weight(1f)){Text(r.recipient.name,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium);Caption(dateLabel(r.completedAt))}
                        Text("${if(r.direction==Direction.CREDIT)"+"else"−"}${won(r.amount)}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
                    }
                }
                Space(8);HomeRule();HomeHeading("다음 일정","관리"){open("recurring")}
                val schedule=service.schedules.firstOrNull{it.active}
                Row(Modifier.fillMaxWidth().heightIn(min=52.dp).clickable(role=Role.Button){open("recurring")},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    HomeIcon(BankIcons.Calendar)
                    Column(Modifier.weight(1f)){Text(schedule?.let{"${it.title} 자동이체"}?:"예정된 이체 없음",style=MaterialTheme.typography.bodyMedium);Caption(schedule?.let{"매월 ${it.day}일"}?:"자동이체 관리")}
                    if(schedule!=null)Text("${won(schedule.amount)}원",style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
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
@Composable private fun HomeHeading(text:String,action:String,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=40.dp),verticalAlignment=Alignment.CenterVertically){
        Text(text,Modifier.weight(1f).semantics{heading()},style=MaterialTheme.typography.titleSmall)
        TextButton(onClick=onClick,contentPadding=PaddingValues(horizontal=4.dp),modifier=Modifier.heightIn(min=48.dp)){
            Text(action,style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
        }
    }
}
@Composable private fun HomeIcon(icon:ImageVector){
    Box(Modifier.size(36.dp).background(TraceColors.Ink.copy(alpha=.035f),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){AppIcon(icon,size=18,tint=TraceColors.Muted)}
}
@Composable private fun HomeRule(){HorizontalDivider(color=TraceColors.Ink.copy(alpha=.085f),thickness=.7.dp)}
@Composable private fun HomeMoneyRow(name:String,caption:String,amount:Long,icon:ImageVector,height:Int,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=height.dp).clickable(role=Role.Button,onClick=onClick),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        HomeIcon(icon)
        Column(Modifier.weight(1f)){Text(name,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium);Text(caption,style=MaterialTheme.typography.labelSmall,color=TraceColors.Muted)}
        Text("${won(amount)}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
    }
}
@Composable private fun EasyHomeAction(title:String,description:String,icon:ImageVector,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=76.dp).clickable(role=Role.Button,onClick=onClick),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
        AppIcon(icon,size=28)
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium);Caption(description)}
        AppIcon(BankIcons.Chevron,size=19)
    };HomeRule()
}
