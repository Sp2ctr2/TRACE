package dev.sp2ctr2.saeon.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import dev.sp2ctr2.saeon.BankViewModel
import dev.sp2ctr2.saeon.domain.*

private data class Tab(val route:String,val label:String,val icon:String)
private val tabs=listOf(Tab("home","홈","home"),Tab("assets","자산","assets"),Tab("transfer","송금","transfer"),Tab("safety","안전","shield"),Tab("more","전체","menu"))
@Composable fun SaeonApp(vm:BankViewModel,authenticate:((()->Unit),(String)->Unit)->Unit,canAuthenticate:()->Boolean) {
    val s by vm.state.collectAsStateWithLifecycle()
    val options by vm.options.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val storageFailure by vm.storageFailure.collectAsStateWithLifecycle()
    var resetConfirm by remember{mutableStateOf(false)}
    val nav=rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route=entry?.destination?.route ?: "home"
    val navigate:(String)->Unit={destination->nav.navigate(destination){launchSingleTop=true;if(destination=="home")popUpTo("home"){inclusive=false}}}
    LaunchedEffect(vm,s!=null){if(s!=null)vm.navigation.collect{navigate(it)}}
    val back:()->Unit={if(!nav.popBackStack())navigate("home")}
    TraceTheme(options){
        Surface(Modifier.fillMaxSize(),color=TraceColors.Surface){
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()){
                Box(Modifier.weight(1f)){
                    val state=s
                    if(state==null){
                        Screen("새온은행",brand=true){
                            Gap(48);Title(if(storageFailure==null)"계좌를 불러오고 있어요" else "계좌를 열지 못했어요")
                            Gap();Copy(storageFailure ?: "저장된 시연 계좌를 확인합니다.")
                            if(storageFailure!=null){Gap(24);Primary("다시 불러오기"){vm.retryLoad()};Secondary("시연 데이터 초기화"){resetConfirm=true}}
                        }
                    }else NavHost(navController=nav,startDestination="home",enterTransition={if(options.reducedMotion)EnterTransition.None else fadeIn(tween(160))},exitTransition={if(options.reducedMotion)ExitTransition.None else fadeOut(tween(120))}){
                        composable("home"){HomeScreen(state,options,navigate)}
                        composable("assets"){AssetsScreen(state,navigate)}
                        composable("transfer"){RecipientScreen(state,vm,navigate)}
                        composable("safety"){SafetyScreen(state,options,navigate)}
                        composable("more"){MoreScreen(navigate)}
                        composable("account/{id}"){AccountScreen(state,it.arguments?.getString("id").orEmpty(),navigate,back)}
                        composable("amount"){AmountScreen(state,vm,back)}
                        composable("review"){ReviewScreen(state,options,busy,vm,authenticate,canAuthenticate,back)}
                        composable("result"){ResultScreen(state,options,busy,vm,navigate,back)}
                        composable("timeline"){TimelineScreen(state,back)}
                        composable("guide"){SafetyGuideScreen(state,navigate,back)}
                        composable("privacy"){PrivacyScreen(state,vm,back)}
                        composable("share"){ShareScreen(vm,back)}
                        composable("history"){HistoryScreen(state,navigate,back)}
                        composable("receipt/{id}"){ReceiptScreen(state,it.arguments?.getString("id").orEmpty(),back)}
                        composable("bring"){BringScreen(state,vm,back)}
                        composable("notifications"){NoticesScreen(state,vm,back)}
                        composable("settings/{section}"){SettingsScreen(it.arguments?.getString("section").orEmpty(),state,options,vm,navigate,back)}
                        composable("schedules"){SchedulesScreen(state,vm,navigate,back)}
                        composable("schedule-new"){ScheduleEditor(state,vm,back)}
                        composable("manual-account"){ManualAccountScreen(vm,back)}
                        composable("support"){SupportScreen(navigate,back)}
                        composable("support-case"){SupportCaseScreen(state,back)}
                        composable("about"){AboutScreen(back)}
                        composable("demo"){DemoLab(state,options,vm,back)}
                    }
                }
                if(s!=null && tabs.any{it.route==route}){
                    Rule();Row(Modifier.fillMaxWidth().background(TraceColors.Surface).heightIn(min=68.dp)){
                        tabs.forEach{tab->
                            val selected=route==tab.route
                            Column(Modifier.weight(1f).heightIn(min=68.dp).clickable(role=Role.Tab){navigate(tab.route)}.testTag("tab_${tab.route}").semantics{this.selected=selected}.padding(top=12.dp,bottom=8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
                                Glyph(tab.icon,if(selected)TraceColors.Action else TraceColors.Faint,22)
                                Text(tab.label,fontSize=11.sp,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal,color=if(selected)TraceColors.Action else TraceColors.Muted)
                            }
                        }
                    }
                }
            }
            if(error!=null)AlertDialog(onDismissRequest=vm::dismissError,containerColor=TraceColors.Surface,title={Text("확인이 필요해요")},text={Copy(error.orEmpty())},confirmButton={TextButton(vm::dismissError){Text("확인")}})
            if(resetConfirm)AlertDialog(onDismissRequest={resetConfirm=false},containerColor=TraceColors.Surface,title={Text("시연 데이터를 초기화할까요?")},text={Copy("저장된 가상 거래 내역이 삭제됩니다.")},confirmButton={TextButton({resetConfirm=false;vm.reset(Scenario.NORMAL)}){Text("초기화")}},dismissButton={TextButton({resetConfirm=false}){Text("취소")}})
        }
    }
}
