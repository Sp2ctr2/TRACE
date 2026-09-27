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
@Composable fun NoGlassHome(state:BankState,preferences:BankPreferences,model:BankViewModel,open:(String)->Unit){
    val services by model.services.state.collectAsStateWithLifecycle()
    var picker by rememberSaveable{mutableStateOf(false)}
    TaskPage("새온은행","home",root=true,actions={
        BankIconButton(BankIcons.Bell,"알림"){open("notifications")}
        BankIconButton(BankIcons.Profile,"내 정보"){open("profile")}
    }){
        val landscape=LocalBankLandscape.current
        val tablet=LocalBankWide.current
        val height=LocalTaskHeight.current
        val short=height<440.dp&&!landscape
        val easy=LocalEasyMode.current
        val hero:@Composable ColumnScope.()->Unit={
            Row(Modifier.heightIn(min=48.dp).clickable(role=Role.Button){picker=true}.testTag("home_account_picker"),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Text("새온 생활통장",style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.Medium,color=TraceColors.Muted)
                AppIcon(BankIcons.Chevron,size=12,tint=TraceColors.Muted)
            }
            BoxWithConstraints(Modifier.fillMaxWidth().testTag("home_balance")){
                val large=maxWidth>=400.dp
                val amountSize=if(large)44.sp else if(maxWidth>=335.dp)38.sp else 32.sp
                Row(Modifier.fillMaxWidth().heightIn(min=50.dp),verticalAlignment=Alignment.CenterVertically){
                    Row(Modifier.weight(1f),verticalAlignment=Alignment.Bottom){
                        if(preferences.hideBalance)Text("잔액 숨김",style=MaterialTheme.typography.displaySmall.copy(fontSize=if(large)36.sp else 30.sp,lineHeight=46.sp))
                        else{
                            Text(won(state.balance),style=MaterialTheme.typography.displaySmall.copy(fontSize=amountSize,lineHeight=48.sp,letterSpacing=(-1.4).sp,fontWeight=FontWeight.SemiBold))
                            Text("원",Modifier.padding(start=5.dp,bottom=6.dp),style=MaterialTheme.typography.bodyMedium)
                        }
                    }
                    BankIconButton(BankIcons.Eye,if(preferences.hideBalance)"잔액 보이기"else"잔액 숨기기",Modifier.testTag("home_hide_balance")){
                        model.preference{hideBalance(!preferences.hideBalance)}
                    }
                }
            }
            if(!short){Caption("110-***-0001");Space(if(tablet)12 else 8)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Box(Modifier.weight(1f)){BankAction("가져오기",Modifier.testTag("home_bring"),smoked=true){open("bring")}}
                Box(Modifier.weight(1f)){BankAction(if(easy)"돈 보내기"else"송금하기",Modifier.testTag("home_transfer")){open("transfer")}}
            }
        }
        val feed:@Composable ColumnScope.()->Unit={
            if(easy){
                EasyAction("거래 내역","보낸 돈과 받은 돈",BankIcons.History){open("history")}
                EasyAction("내 자산","통장·적금·카드",BankIcons.Assets){open("assets")}
                EasyAction("도움 받기","고객센터와 계좌 보호",BankIcons.Phone){open("support")}
            }else{
                FeedHeading("내 금융","전체 보기"){open("assets")}
                FeedRule()
                AssetRow("새온 생활통장","입출금",state.balance,BankIcons.Bank,preferences.hideBalance,tablet){open("account")};FeedRule()
                AssetRow("모아적금","예·적금",state.savings,BankIcons.Assets,preferences.hideBalance,tablet){open("savings")}
                if(!short){FeedRule();AssetRow("투자","보유 자산 2개",1_840_000,BankIcons.History,preferences.hideBalance,tablet){open("investments")}}
                if(!short){
                    Space(if(tablet)18 else 8);FeedHeading("최근 거래","더보기"){open("history")};FeedRule()
                    state.receipts.firstOrNull()?.let{r->
                        val sign=if(r.direction==Direction.CREDIT)"+"else"−"
                        SimpleRow(r.recipient.name,dateLabel(r.completedAt),if(preferences.hideBalance)"숨김"else"$sign${won(r.amount)}원",if(r.direction==Direction.CREDIT)BankIcons.Download else BankIcons.Card,tablet){open("receipt/${r.id}")}
                    }
                    if(height>=600.dp||landscape){
                        Space(if(tablet)18 else 8);FeedHeading("다음 일정","관리"){open("recurring")};FeedRule()
                        val next=services.schedules.firstOrNull{it.active}
                        SimpleRow(next?.let{"${it.title} 자동이체"}?:"예정된 이체 없음",next?.let{"매월 ${it.day}일"}?:"자동이체 관리",if(next==null)""else if(preferences.hideBalance)"숨김"else"${won(next.amount)}원",BankIcons.Calendar,tablet){open("recurring")}
                    }
                }
            }
        }
        if(landscape){
            Row(Modifier.fillMaxWidth().height((height-16.dp).coerceAtLeast(120.dp)),horizontalArrangement=Arrangement.spacedBy(32.dp)){
                Column(Modifier.weight(.92f).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("home_hero_pane").padding(top=8.dp),content=hero)
                Column(Modifier.weight(1.08f).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("home_feed_pane").padding(bottom=16.dp),content=feed)
            }
        }else{
            hero();Space(if(short)2 else if(tablet)24 else 12);feed()
        }
    }
    if(picker)ModalBottomSheet(onDismissRequest={picker=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=TraceColors.Surface){
        Column(Modifier.widthIn(max=640.dp).fillMaxWidth().padding(horizontal=24.dp).padding(bottom=24.dp)){
            Text("내 계좌",style=MaterialTheme.typography.headlineSmall);Space(12)
            MenuRow("새온 생활통장",if(preferences.hideBalance)"잔액 숨김"else"${won(state.balance)}원",BankIcons.Bank){picker=false;open("account")}
            MenuRow("모아적금",if(preferences.hideBalance)"잔액 숨김"else"${won(state.savings)}원",BankIcons.Assets){picker=false;open("savings")}
            SecondaryButton("닫기"){picker=false}
        }
    }
}
@Composable private fun FeedHeading(title:String,action:String,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically){
        Text(title,Modifier.weight(1f).semantics{heading()},style=MaterialTheme.typography.titleSmall)
        TextButton(onClick=onClick,contentPadding=PaddingValues(horizontal=4.dp)){Text(action,style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)}
    }
}
@Composable private fun FeedRule(){HorizontalDivider(thickness=.7.dp,color=TraceColors.Divider)}
@Composable private fun AssetRow(title:String,sub:String,value:Long,icon:ImageVector,hidden:Boolean,tablet:Boolean,onClick:()->Unit){
    SimpleRow(title,sub,if(hidden)"숨김"else"${won(value)}원",icon,tablet,onClick)
}
@Composable private fun SimpleRow(title:String,sub:String,value:String,icon:ImageVector,tablet:Boolean,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=if(tablet)58.dp else 50.dp).clickable(role=Role.Button,onClick=onClick),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Box(Modifier.size(34.dp).background(TraceColors.Ink.copy(alpha=.045f),RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){AppIcon(icon,size=18,tint=TraceColors.Muted)}
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium);Text(sub,style=MaterialTheme.typography.labelSmall,color=TraceColors.Muted)}
        Text(value,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
    }
}
@Composable private fun EasyAction(title:String,sub:String,icon:ImageVector,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=76.dp).clickable(role=Role.Button,onClick=onClick),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
        AppIcon(icon,size=26);Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium);Caption(sub)};AppIcon(BankIcons.Chevron,size=16)
    };FeedRule()
}
