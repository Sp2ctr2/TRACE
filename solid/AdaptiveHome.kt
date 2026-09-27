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
@Composable fun AdaptiveHome(
    state:BankState,
    preferences:BankPreferences,
    model:BankViewModel,
    open:(String)->Unit
){
    val service by model.services.state.collectAsStateWithLifecycle()
    val spec=LocalAdaptiveSpec.current
    var picker by rememberSaveable{mutableStateOf(false)}
    TaskPage("새온은행","home",root=true,actions={
        PlainIconAction(BankIcons.Bell,"알림"){open("notifications")}
        PlainIconAction(BankIcons.Profile,"내 정보"){open("profile")}
    }){
        val easy=LocalEasyMode.current
        if(easy){
            EasyHome(state,preferences,model,open)
        } else if(spec.twoPane){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(36.dp),verticalAlignment=Alignment.Top){
                Column(Modifier.weight(.88f).widthIn(min=280.dp)){
                    HomeHero(state,preferences,model,{picker=true},open)
                    Space(22)
                    val next=service.schedules.firstOrNull{it.active}
                    HomeSection("다음 일정","관리"){open("recurring")}
                    HomeSchedule(next,preferences.hideBalance){open("recurring")}
                }
                Column(Modifier.weight(1.12f).widthIn(min=320.dp)){
                    HomeFinance(state,preferences.hideBalance,open)
                    Space(18);HomeRule()
                    HomeRecent(state,preferences.hideBalance,open,limit=2)
                }
            }
        } else {
            HomeHero(state,preferences,model,{picker=true},open)
            Space(if(spec.compactHeight)6 else 14)
            HomeFinance(state,preferences.hideBalance,open)
            if(!spec.compactHeight){
                Space(10);HomeRule()
                HomeRecent(state,preferences.hideBalance,open,limit=1)
                if(spec.height>=700.dp){
                    Space(8);HomeRule()
                    val next=service.schedules.firstOrNull{it.active}
                    HomeSection("다음 일정","관리"){open("recurring")}
                    HomeSchedule(next,preferences.hideBalance){open("recurring")}
                }
            }
        }
    }
    if(picker){
        ModalBottomSheet(
            onDismissRequest={picker=false},
            sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
            containerColor=TraceColors.Surface
        ){
            Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=28.dp)){
                Text("내 계좌",style=MaterialTheme.typography.headlineSmall)
                Space(12)
                MenuRow("새온 생활통장",if(preferences.hideBalance)"잔액 숨김"else"${won(state.balance)}원",BankIcons.Bank){picker=false;open("account")}
                MenuRow("모아적금",if(preferences.hideBalance)"잔액 숨김"else"${won(state.savings)}원",BankIcons.Assets){picker=false;open("savings")}
                SecondaryButton("닫기"){picker=false}
            }
        }
    }
}

@Composable private fun HomeHero(
    state:BankState,
    preferences:BankPreferences,
    model:BankViewModel,
    chooseAccount:()->Unit,
    open:(String)->Unit
){
    val spec=LocalAdaptiveSpec.current
    val reduced=LocalReducedMotion.current
    AccountSelector("새온 생활통장",Modifier.testTag("home_account_picker"),chooseAccount)
    Space(if(spec.compactHeight)2 else 5)
    Row(Modifier.fillMaxWidth().heightIn(min=50.dp).testTag("home_balance"),verticalAlignment=Alignment.CenterVertically){
        AnimatedContent(
            targetState=preferences.hideBalance,
            transitionSpec={if(reduced)EnterTransition.None togetherWith ExitTransition.None else fadeIn(tween(170)) togetherWith fadeOut(tween(120))},
            label="home_balance_privacy",
            modifier=Modifier.weight(1f)
        ){hidden->
            Row(verticalAlignment=Alignment.Bottom){
                if(hidden){
                    Text("잔액 숨김",style=MaterialTheme.typography.displaySmall.copy(fontSize=if(spec.compactHeight)28.sp else 38.sp,lineHeight=44.sp,fontWeight=FontWeight.SemiBold))
                }else{
                    Text(
                        won(state.balance),
                        style=MaterialTheme.typography.displaySmall.copy(
                            fontSize=if(spec.compactHeight)30.sp else 40.sp,
                            lineHeight=46.sp,
                            letterSpacing=(-1.3).sp,
                            fontWeight=FontWeight.SemiBold,
                            fontFeatureSettings="tnum"
                        )
                    )
                    Text("원",Modifier.padding(start=5.dp,bottom=5.dp),style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
        PlainIconAction(
            BankIcons.Eye,
            if(preferences.hideBalance)"잔액 보이기"else"잔액 숨기기",
            Modifier.testTag("home_hide_balance").semantics{stateDescription=if(preferences.hideBalance)"숨김"else"표시 중"}
        ){model.preference{hideBalance(!preferences.hideBalance)}}
    }
    if(!spec.compactHeight){
        Caption("110-***-0001")
        Space(10)
    } else Space(4)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Box(Modifier.weight(1f)){
            GradientAction("가져오기",Modifier.testTag("home_bring"),tone=ActionTone.INK){open("bring")}
        }
        Box(Modifier.weight(1f)){
            GradientAction("송금하기",Modifier.testTag("home_transfer"),tone=ActionTone.CORAL){open("transfer")}
        }
    }
}

@Composable private fun HomeFinance(state:BankState,hidden:Boolean,open:(String)->Unit){
    HomeSection("내 금융","전체 보기"){open("assets")}
    HomeAsset("새온 생활통장","입출금",state.balance,BankIcons.Bank,hidden){open("account")};HomeRule()
    HomeAsset("모아적금","예·적금",state.savings,BankIcons.Assets,hidden){open("savings")};HomeRule()
    HomeAsset("투자","보유 자산 2개",1_840_000,BankIcons.History,hidden){open("investments")}
}

@Composable private fun HomeRecent(state:BankState,hidden:Boolean,open:(String)->Unit,limit:Int){
    HomeSection("최근 거래","더보기"){open("history")}
    state.receipts.take(limit).forEachIndexed{i,r->
        Row(
            Modifier.fillMaxWidth().heightIn(min=50.dp).clickable(role=Role.Button){open("receipt/${r.id}")},
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)
        ){
            HomeIcon(if(r.direction==Direction.CREDIT)BankIcons.Download else BankIcons.Card)
            Column(Modifier.weight(1f)){
                Text(r.recipient.name,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
                Caption(dateLabel(r.completedAt))
            }
            Text(if(hidden)"숨김"else"${if(r.direction==Direction.CREDIT)"+"else"−"}${won(r.amount)}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
        }
        if(i<limit-1)HomeRule()
    }
}

@Composable private fun HomeSchedule(schedule:app.saeon.trace.data.ScheduledTransfer?,hidden:Boolean,onClick:()->Unit){
    Row(
        Modifier.fillMaxWidth().heightIn(min=50.dp).clickable(role=Role.Button,onClick=onClick),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(10.dp)
    ){
        HomeIcon(BankIcons.Calendar)
        Column(Modifier.weight(1f)){
            Text(schedule?.let{"${it.title} 자동이체"}?:"예정된 이체 없음",style=MaterialTheme.typography.bodyMedium)
            Caption(schedule?.let{"매월 ${it.day}일"}?:"자동이체 관리")
        }
        if(schedule!=null)Text(if(hidden)"숨김"else"${won(schedule.amount)}원",style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
    }
}

@Composable private fun HomeSection(text:String,action:String,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=40.dp),verticalAlignment=Alignment.CenterVertically){
        Text(text,Modifier.weight(1f).semantics{heading()},style=MaterialTheme.typography.titleSmall)
        TextButton(onClick=onClick,modifier=Modifier.heightIn(min=44.dp),contentPadding=PaddingValues(horizontal=4.dp)){
            Text(action,style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
        }
    }
}
@Composable private fun HomeIcon(icon:ImageVector){
    Box(Modifier.size(34.dp).background(TraceColors.Ink.copy(alpha=.035f),RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){
        AppIcon(icon,size=18,tint=TraceColors.Muted)
    }
}
@Composable private fun HomeRule(){HorizontalDivider(color=TraceColors.Ink.copy(alpha=.075f),thickness=.7.dp)}
@Composable private fun HomeAsset(title:String,subtitle:String,amount:Long,icon:ImageVector,hidden:Boolean,onClick:()->Unit){
    Row(
        Modifier.fillMaxWidth().heightIn(min=50.dp).clickable(role=Role.Button,onClick=onClick),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(10.dp)
    ){
        HomeIcon(icon)
        Column(Modifier.weight(1f)){
            Text(title,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
            Text(subtitle,style=MaterialTheme.typography.labelSmall,color=TraceColors.Muted)
        }
        Text(if(hidden)"숨김"else"${won(amount)}원",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
    }
}

@Composable private fun EasyHome(state:BankState,preferences:BankPreferences,model:BankViewModel,open:(String)->Unit){
    AccountSelector("새온 생활통장"){open("account")}
    Space(6)
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically){
        Text(
            if(preferences.hideBalance)"잔액 숨김"else"${won(state.balance)}원",
            Modifier.weight(1f),
            style=MaterialTheme.typography.displaySmall.copy(fontSize=36.sp,fontWeight=FontWeight.SemiBold)
        )
        PlainIconAction(BankIcons.Eye,if(preferences.hideBalance)"잔액 보이기"else"잔액 숨기기"){model.preference{hideBalance(!preferences.hideBalance)}}
    }
    Space(10)
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Box(Modifier.weight(1f)){GradientAction("가져오기",tone=ActionTone.INK){open("bring")}}
        Box(Modifier.weight(1f)){GradientAction("돈 보내기",tone=ActionTone.CORAL){open("transfer")}}
    }
    Space(18)
    EasyAction("거래 내역","보낸 돈과 받은 돈",BankIcons.History){open("history")}
    EasyAction("내 자산","통장·적금·카드",BankIcons.Assets){open("assets")}
    EasyAction("도움 받기","고객센터와 계좌 보호",BankIcons.Phone){open("support")}
}
@Composable private fun EasyAction(title:String,description:String,icon:ImageVector,onClick:()->Unit){
    Row(
        Modifier.fillMaxWidth().heightIn(min=76.dp).clickable(role=Role.Button,onClick=onClick),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(14.dp)
    ){
        AppIcon(icon,size=27)
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleMedium);Caption(description)}
        AppIcon(BankIcons.Chevron,size=18)
    }
    HomeRule()
}
