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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.data.BankPreferences
import app.saeon.trace.ui.BankViewModel
import app.saeon.trace.ui.InteractionState
import app.saeon.trace.ui.design.*

/** The registered destination is repository output, never an account supplied by the requester. */
@Composable fun RefinedVerify(record:TransferRecord,interaction:InteractionState,model:BankViewModel,back:()->Unit,home:()->Unit){
    var requested by rememberSaveable(record.intent.id){mutableStateOf(false)}
    LaunchedEffect(record.intent.id,record.stage,interaction.busy){
        if(record.stage==TransferStage.VERIFY&&!interaction.busy&&!requested){requested=true;model.resolveRoute(record.intent.id)}
    }
    val route=record.route
    val valid=route!=null&&route.sourceIntentId==record.intent.id&&route.expiresAt>model.repository.clock.now()
    val loading=interaction.routeLoading||record.stage==TransferStage.VERIFY
    TaskPage("상환 경로 확인",if(route==null)"trace_verify"else"trace_official_route",back=back,
        actions={AppIcon(BankIcons.Trace,size=24,tint=TraceColors.Coral)},footer={
            PrimaryButton(if(loading)"상환 계좌 확인 중"else"이 계좌로 상환하기",Modifier.testTag("official_route_use"),enabled=valid&&!interaction.busy){model.useRoute(record.intent.id)}
            SecondaryButton("송금 취소"){model.cancelTransfer(record.intent.id,home)}
        }){
        val compact=LocalTaskHeight.current<500.dp
        Space(if(compact)0 else 12)
        Text("상환 계좌를\n다시 확인했어요",style=MaterialTheme.typography.headlineMedium.copy(fontSize=if(compact)25.sp else 28.sp,lineHeight=35.sp))
        Space(10)
        Body("대출 상환은 은행에 등록된 계좌로 보내야 합니다.",subdued=true)
        Space(if(compact)12 else 24)
        Caption("안내받은 개인 계좌")
        Row(Modifier.fillMaxWidth().heightIn(min=44.dp),verticalAlignment=Alignment.CenterVertically){
            Text(record.intent.recipient.name,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
            Text("${won(record.intent.amount)}원",style=MaterialTheme.typography.titleSmall)
        }
        if(!compact)Caption("${record.intent.recipient.bank}  ${record.intent.recipient.account}")
        Space(if(compact)10 else 18);Rule();Space(if(compact)12 else 22)
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            AppIcon(BankIcons.Bank,size=20);Text("은행 등록 상환계좌",style=MaterialTheme.typography.titleSmall)
        }
        Space(12)
        DeferredSkeleton(loading,rows=2){
            if(route!=null){
                Column(Modifier.fillMaxWidth().testTag("verified_account").background(TraceColors.Surface,RoundedCornerShape(18.dp)).padding(horizontal=16.dp,vertical=if(compact)12.dp else 18.dp)){
                    Text(route.recipient.name,style=MaterialTheme.typography.titleSmall)
                    Space(7);Text("${route.recipient.bank}  ${route.recipient.account}",style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium)
                    if(!compact){Space(9);Caption(route.productName)}
                }
            }
        }
        if(!loading&&!valid){Space(8);Caption("계좌 정보를 다시 확인해 주세요.")}
        if(!loading)QuietButton("계좌 정보 새로고침",Modifier.testTag("official_route_refresh"),enabled=!interaction.busy){model.resolveRoute(record.intent.id)}
    }
}

private data class ContextItem(val time:Long,val title:String,val detail:String,val final:Boolean=false)
@Composable fun RefinedTimeline(state:BankState,back:()->Unit){
    val record=state.current?:state.pending.lastOrNull()
    val events=if(record==null)state.events else RiskContext(state.events).relevant(record.intent,record.intent.createdAt)
    val items=events.sortedBy{it.createdAt}.map{ContextItem(it.createdAt,it.type.label,it.summary)}.toMutableList()
    if(record!=null)items+=ContextItem(record.intent.createdAt,"${won(record.intent.amount)}원 송금 시도","${record.intent.recipient.name} · ${record.intent.recipient.bank}",true)
    Page("연결된 맥락","trace_timeline",back=back){
        Space(12);Headline("요청에서 송금까지");Space(10)
        if(record!=null)Caption("${record.intent.recipient.name} · ${won(record.intent.amount)}원")
        Space(24)
        if(items.isEmpty())EmptyState("연결된 내역이 없습니다.","확인한 요청과 거래가 시간순으로 표시됩니다.")
        items.forEachIndexed{i,item->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Text(timeLabel(item.time),Modifier.width(46.dp).padding(top=3.dp),style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
                Column(Modifier.width(8.dp).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally){
                    Box(Modifier.padding(top=8.dp).size(6.dp).background(if(item.final)TraceColors.CoralText else TraceColors.Divider,RoundedCornerShape(3.dp)))
                    if(i<items.lastIndex)Box(Modifier.width(1.dp).weight(1f).background(TraceColors.Divider))
                }
                Column(Modifier.weight(1f).padding(bottom=26.dp)){
                    Text(item.title,style=MaterialTheme.typography.titleSmall,color=if(item.final)TraceColors.CoralText else TraceColors.Ink)
                    Space(6);Text(item.detail,style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)
                }
            }
        }
    }
}

@Composable fun RefinedSafetyGuide(state:BankState,open:(String)->Unit,back:()->Unit){
    val loan=state.current?.intent?.purpose==Purpose.LOAN
    Page("안전하게 확인하기","trace_safety_guide",back=back,footer={
        PrimaryButton(if(loan)"대출 상환 메뉴"else"고객센터 열기",Modifier.testTag("safety_official_channel")){open(if(loan)"loan"else"support")}
    }){
        Space(12);Headline("확인하고 결정하세요");Space(22)
        GuideItem(BankIcons.Phone,"통화는 잠시 종료","상대의 요청을 따르기 전에 확인할 시간을 확보하세요.")
        GuideItem(BankIcons.Bank,"연락처는 직접 확인","은행 앱의 고객센터나 기존에 저장한 번호를 이용하세요.")
        GuideItem(BankIcons.Profile,"가까운 사람과 함께 확인","가족이나 신뢰하는 사람에게 요청 내용과 계좌를 보여 주세요.")
        Space(18)
        MenuRow("송금 잠금", "확인하는 동안 추가 송금을 제한합니다.",BankIcons.Lock){open("account_protection")}
    }
}
@Composable private fun GuideItem(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,body:String){
    Row(Modifier.fillMaxWidth().padding(vertical=16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.Top){
        Box(Modifier.size(40.dp).background(TraceColors.Surface,RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){AppIcon(icon,size=21)}
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.titleSmall);Space(7);Text(body,style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted)}
    };Rule()
}

@Composable fun RefinedHelp(open:(String)->Unit,back:()->Unit){
    var expanded by rememberSaveable{mutableIntStateOf(-1)}
    val reduced=LocalReducedMotion.current
    val entries=listOf(
        Triple("송금이 보류됐어요","연결된 요청에서 위험 신호가 확인되면 송금을 보류합니다. 연결된 맥락을 확인하고 고객센터를 이용해 주세요.","safety_guide"),
        Triple("상환 계좌를 다시 확인하는 이유","대출 상환을 개인 명의 계좌로 보내려는 경우 은행 등록 계좌를 확인합니다. 받는 계좌가 바뀌면 내용을 확인하고 다시 인증해야 합니다.","loan"),
        Triple("송금 완료 여부를 확인하고 싶어요","거래 내역에서 금액과 받는 분을 확인하세요. 결과가 확인되지 않았다면 같은 송금을 다시 시도하지 마세요.","history"),
        Triple("송금 한도는 어디서 바꾸나요?","전체 메뉴의 송금 설정에서 하루 송금 한도를 변경할 수 있습니다.","transfer_settings"),
        Triple("화면을 보기 쉽게 바꾸고 싶어요","접근성에서 쉬운 사용을 켜면 주요 기능을 중심으로 화면이 바뀝니다. 움직임과 투명 효과는 각각 조절할 수 있습니다.","accessibility")
    )
    Page("도움말","help",back=back){
        Space(12);Headline("자주 묻는 질문");Space(20)
        entries.forEachIndexed{i,(question,answer,route)->
            Column(Modifier.fillMaxWidth()){
                Row(Modifier.fillMaxWidth().heightIn(min=62.dp).clickable(role=Role.Button){expanded=if(expanded==i)-1 else i}
                    .testTag("help_$i").semantics{stateDescription=if(expanded==i)"펼침"else"접힘"},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Text(question,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
                    val rotation by animateFloatAsState(if(expanded==i)90f else 0f,if(reduced)snap()else tween(220),label="help-arrow")
                    Icon(BankIcons.Chevron,null,Modifier.size(17.dp).rotate(rotation),tint=TraceColors.Muted)
                }
                AnimatedVisibility(expanded==i,enter=if(reduced)EnterTransition.None else expandVertically(tween(220))+fadeIn(tween(180)),exit=if(reduced)ExitTransition.None else shrinkVertically(tween(180))+fadeOut(tween(120))){
                    Column(Modifier.padding(bottom=14.dp)){
                        Text(answer,style=MaterialTheme.typography.bodyMedium,color=TraceColors.Muted);Space(10)
                        QuietButton(when(route){"safety_guide"->"안전하게 확인하기";"loan"->"대출 관리";"history"->"거래 내역";"transfer_settings"->"송금 설정";else->"접근성 설정"}){open(route)}
                    }
                }
            };Rule()
        }
        Space(24);MenuRow("고객센터",icon=BankIcons.Phone){open("support")}
    }
}

@Composable fun RefinedAppearance(pref:BankPreferences,model:BankViewModel,back:()->Unit){
    Page("화면 설정","appearance",back=back){
        Space(12);Text("화면 모드",style=MaterialTheme.typography.titleMedium);Space(16)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            ThemeTile("라이트","light",pref.themeMode=="light",false,Modifier.weight(1f)){model.preference{theme("light")}}
            ThemeTile("다크","dark",pref.themeMode=="dark",true,Modifier.weight(1f)){model.preference{theme("dark")}}
        }
        Row(Modifier.fillMaxWidth().heightIn(min=62.dp).clickable(role=Role.RadioButton){model.preference{theme("system")}}.testTag("theme_system").semantics{selected=pref.themeMode=="system"},verticalAlignment=Alignment.CenterVertically){
            Text("기기 설정 따르기",Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge);RadioButton(pref.themeMode=="system",null)
        }
        Space(20);Rule();Space(16);Text("화면 효과",style=MaterialTheme.typography.titleMedium)
        OptionRow("움직임 줄이기",pref.reducedMotion,"이동과 확대 효과를 줄입니다."){model.preference{motion(it)}}
        OptionRow("투명 효과 줄이기",pref.reducedTransparency,"버튼과 하단 바를 불투명하게 표시합니다."){model.preference{transparency(it)}}
    }
}
@Composable private fun ThemeTile(label:String,id:String,selected:Boolean,dark:Boolean,modifier:Modifier,onClick:()->Unit){
    Column(modifier.clip(RoundedCornerShape(19.dp)).clickable(role=Role.RadioButton,onClick=onClick).testTag("theme_$id").semantics{this.selected=selected}.padding(3.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Box(Modifier.fillMaxWidth().height(118.dp).border(if(selected)2.dp else 1.dp,if(selected)TraceColors.Ink else TraceColors.Divider,RoundedCornerShape(17.dp)).padding(5.dp)){
            Column(Modifier.fillMaxSize().background(if(dark)Color(0xFF151517)else Color(0xFFF6F7F5),RoundedCornerShape(13.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                val ink=if(dark)Color(0xFFCCCCD3)else Color(0xFF65656D)
                Box(Modifier.width(27.dp).height(5.dp).background(ink,RoundedCornerShape(3.dp)))
                Box(Modifier.width(62.dp).height(9.dp).background(ink,RoundedCornerShape(4.dp)))
                Box(Modifier.fillMaxWidth().height(20.dp).background(if(dark)Color(0xFFC99583)else Color(0xFFF5A18A),RoundedCornerShape(8.dp)))
                Box(Modifier.fillMaxWidth(.65f).height(5.dp).background(ink.copy(alpha=.35f),RoundedCornerShape(3.dp)))
            }
        }
        Text(label,Modifier.padding(vertical=12.dp),style=MaterialTheme.typography.bodyMedium,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable fun RefinedBring(state:BankState,model:BankViewModel,open:(String)->Unit,back:()->Unit){
    var step by rememberSaveable{mutableIntStateOf(0)}
    var amount by rememberSaveable{mutableStateOf("")}
    val operationId=rememberSaveable{newId()}
    val interaction by model.interaction.collectAsStateWithLifecycle()
    val number=amount.toLongOrNull()?:0
    val valid=number>0&&number<=state.savings&&!interaction.busy
    val reduced=LocalReducedMotion.current
    Page("가져오기","bring",back={if(step>0)step=0 else back()},footer={
        if(step==1)PrimaryButton("${won(number)}원 가져오기",Modifier.testTag("bring_confirm"),enabled=valid){model.act{
            val after=model.repository.bring(number,operationId)
            after.receipts.find{it.intentId==operationId}?.let{open("receipt/${it.id}")}
        }}
    }){
        AnimatedContent(step,transitionSpec={if(reduced)EnterTransition.None togetherWith ExitTransition.None else (fadeIn(tween(220))+slideInHorizontally(tween(280)){24}) togetherWith fadeOut(tween(140))},label="funding-steps"){current->
            Column(Modifier.fillMaxWidth()){
                Space(12)
                if(current==0){
                    Headline("어느 계좌에서\n가져올까요?");Space(24)
                    MenuRow("모아적금","새온은행 · ${won(state.savings)}원",BankIcons.Assets,tag="bring_source"){step=1}
                    Space(20);Caption("본인 명의로 연결된 계좌만 표시됩니다.")
                }else{
                    Headline("얼마를 가져올까요?");Space(22)
                    DetailRow("보내는 계좌","모아적금");DetailRow("받는 계좌","새온 생활통장");Space(20)
                    Field(amount,"가져올 금액",{amount=it.filter(Char::isDigit).take(9)},Modifier.testTag("bring_amount"),keyboard=KeyboardType.Number)
                    Space(12);Caption("가져올 수 있는 금액 ${won(state.savings)}원")
                    if(number>state.savings){Space(8);ErrorNote("계좌 잔액을 초과했습니다.")}
                }
            }
        }
    }
}
