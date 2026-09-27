package app.saeon.trace.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import app.saeon.trace.BuildConfig
import app.saeon.trace.core.*
import app.saeon.trace.ui.design.*
import app.saeon.trace.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SaeonApp(
    model: BankViewModel, safety: SafetyViewModel,
    onNavigationReady: (NavHostController) -> Unit,
    onBiometric: (AuthorizationChallenge) -> Unit,
    onVoice: () -> Unit, onStopVoice: () -> Unit, voiceActive: Boolean
) {
    val stage by model.stage.collectAsStateWithLifecycle()
    val view=LocalView.current
    DisposableEffect(stage.unlocked){view.keepScreenOn=stage.unlocked;onDispose{view.keepScreenOn=false}}
    val bank by model.bank.collectAsStateWithLifecycle()
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val interaction by model.interaction.collectAsStateWithLifecycle()
    val fatal by model.fatal.collectAsStateWithLifecycle()
    val shared by safety.state.collectAsStateWithLifecycle()
    val nav=rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route=entry?.destination?.route?:"home"
    val reduced=LocalReducedMotion.current
    val roots=listOf("home","assets","transfer","safety","more","presenter")
    val haptics=LocalHapticFeedback.current
    var lastState by remember{mutableStateOf<Pair<String,TransferStage>?>(null)}
    LaunchedEffect(nav){onNavigationReady(nav)}
    LaunchedEffect(shared.delivery){if(shared.delivery!=0L)nav.navigate("manual"){launchSingleTop=true}}
    LaunchedEffect(bank?.current?.intent?.id,bank?.current?.stage){
        bank?.current?.let{record->
            model.stageObserve(record)
            val current=record.intent.id to record.stage
            if(!reduced&&lastState!=null&&current!=lastState){
                if(record.stage==TransferStage.HOLD)haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if(record.stage==TransferStage.COMPLETE)haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            lastState=current
        }
    }
    val open:(String)->Unit={destination->nav.navigate(destination){launchSingleTop=true}}
    val back:()->Unit={if(!nav.popBackStack())nav.navigate("home"){launchSingleTop=true}}
    val home:()->Unit={nav.navigate(if(stage.unlocked)"presenter"else"home"){popUpTo("home"){inclusive=false};launchSingleTop=true}}

    Surface(color=TraceColors.Paper,modifier=Modifier.fillMaxSize()){
        BoxWithConstraints(Modifier.fillMaxSize()){
            val adaptive=adaptiveSpec(maxWidth,maxHeight)
            val rootRoute=route in roots
            val selectedRoute=if(stage.unlocked&&route=="more")"presenter"else route
            val tabs=listOf(
                Triple("home","홈",BankIcons.Home),
                Triple("assets","자산",BankIcons.Assets),
                Triple("transfer","송금",BankIcons.Transfer),
                Triple("safety","안전",BankIcons.Shield),
                if(stage.unlocked)Triple("presenter","시연",BankIcons.Trace)else Triple("more","전체",BankIcons.More)
            )
            SolidAmbient(Modifier.matchParentSize())

            if(rootRoute&&adaptive.useRail){
                SolidNavigationRail(
                    tabs,selectedRoute,
                    Modifier.align(Alignment.CenterStart).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                ){destination->
                    nav.navigate(destination){
                        popUpTo(nav.graph.findStartDestination().id){saveState=true}
                        launchSingleTop=true;restoreState=true
                    }
                }
            }

            CompositionLocalProvider(
                LocalAdaptiveSpec provides adaptive,
                LocalReducedTransparency provides preferences.reducedTransparency,
                LocalPageBottomInset provides if(rootRoute&&!adaptive.useRail)88 else 28
            ){
                Box(
                    Modifier.fillMaxSize()
                        .padding(start=if(rootRoute&&adaptive.useRail)78.dp else 0.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .imePadding()
                ){
                    if(fatal!=null){
                        Page(title="새온은행",footer={PrimaryButton("거래 상태 다시 읽기",enabled=!interaction.busy){model.retryStorage()}}){
                            Space(32);Headline("거래 상태를\n먼저 확인해야 해요.");Space(20);Body(fatal!!);Space(24);Caption("저장된 잔액이나 내역은 임의로 초기화하지 않습니다.")
                        }
                    }else if(bank==null){
                        Page(title="새온은행"){Space(22);DeferredSkeleton(true,rows=5){}}
                    }else{
                        val state=bank!!
                        SharedTransitionLayout{
                            CompositionLocalProvider(LocalMotionShared provides this){
                                NavHost(
                                    navController=nav,
                                    startDestination="home",
                                    enterTransition={
                                        if(reduced)EnterTransition.None
                                        else{
                                            val from=roots.indexOf(initialState.destination.route)
                                            val to=roots.indexOf(targetState.destination.route)
                                            val tab=from>=0&&to>=0
                                            val direction=if(tab&&to<from)-1 else 1
                                            fadeIn(tween(if(tab)220 else 260,delayMillis=15))+slideInHorizontally(tween(285,easing=TraceMotion.Enter)){18*direction}
                                        }
                                    },
                                    exitTransition={if(reduced)ExitTransition.None else fadeOut(tween(145))+slideOutHorizontally(tween(220)){ -6 }},
                                    popEnterTransition={if(reduced)EnterTransition.None else fadeIn(tween(220))+slideInHorizontally(tween(270,easing=TraceMotion.Enter)){ -18 }},
                                    popExitTransition={if(reduced)ExitTransition.None else fadeOut(tween(150))+slideOutHorizontally(tween(230)){ 18 }}
                                ){
                                    motionScreen("home"){AdaptiveHome(state,preferences,model,open)}
                                    motionScreen("assets"){StudioAssets(state,open)}
                                    motionScreen("account"){AccountScreen(state,open,back)}
                                    motionScreen("savings"){SavingsScreen(state,open,back)}
                                    motionScreen("card"){CardScreen(state,preferences,model,open,back)}
                                    motionScreen("loan"){LoanScreen(state,model,open,back)}
                                    motionScreen("bring"){RefinedBring(state,model,open,back)}
                                    motionScreen("history"){HistoryScreen(state,open,back)}
                                    motionScreen("receipt/{receiptId}"){target->ReceiptScreen(state.receipts.find{it.id==target.arguments?.getString("receiptId")},open,back)}
                                    motionScreen("transfer"){ViewportRecipient(state,model,open)}
                                    motionScreen("recipients_all"){RecipientScreen(state,model,open)}
                                    motionScreen("comparison"){if(stage.unlocked)ComparisonScreen(model,open,back)else StageLocked(back)}
                                    motionScreen("recipient_entry"){RecipientEntryScreen(state,model,open,back)}
                                    motionScreen("amount"){ViewportAmount(state,model,open,back)}
                                    motionScreen("transfer_state"){TransferStateScreen(state,preferences,interaction,model,open,back,home)}
                                    motionScreen("safety"){SafetyScreen(state,model,open)}
                                    motionScreen("pending"){PendingScreen(state,model,open,back)}
                                    motionScreen("timeline"){AdaptiveTimeline(state,back)}
                                    motionScreen("safety_guide"){RefinedSafetyGuide(state,open,back)}
                                    motionScreen("privacy"){PrivacyScreen(state,model,back)}
                                    motionScreen("manual"){
                                        DisposableEffect(Unit){onDispose{onStopVoice()}}
                                        ManualCheckScreen(safety,open,{onStopVoice();safety.clear();back()},onVoice,onStopVoice,voiceActive)
                                    }
                                    motionScreen("more"){StudioMore(preferences,open)}
                                    motionScreen("profile"){ProfileScreen(preferences,model,back)}
                                    motionScreen("security"){SecurityScreen(preferences,model,open,back)}
                                    motionScreen("transfer_settings"){TransferSettingsScreen(state,model,open,back)}
                                    motionScreen("favorites"){FavoritesScreen(state,model,back)}
                                    motionScreen("recurring"){StudioServiceScreen("recurring",state,model,open,back)}
                                    motionScreen("notifications"){NotificationsScreen(state,preferences,model,open,back)}
                                    motionScreen("appearance"){RefinedAppearance(preferences,model,back)}
                                    motionScreen("accessibility"){AccessibilityScreen(preferences,model,back)}
                                    motionScreen("support"){StudioServiceScreen("support",state,model,open,back)}
                                    motionScreen("help"){RefinedHelp(open,back)}
                                    motionScreen("demo_lab"){if(stage.unlocked)DemoLabScreen(state,model,open,home,back)else StageLocked(back)}
                                    motionScreen("app_info"){StageAppInfoScreen(model,open,back)}
                                    motionScreen("presenter"){StudioDemoCenter(model,open)}
                                    motionScreen("stage_context"){if(stage.unlocked)StageContextScreen(model,open,back)else StageLocked(back)}
                                    motionScreen("scenario/{id}"){target->if(stage.unlocked)ScenarioStoryScreen(runCatching{DemoScenario.valueOf(target.arguments?.getString("id").orEmpty())}.getOrDefault(DemoScenario.NORMAL),model,open,back)else StageLocked(back)}
                                    motionScreen("investments"){StudioServiceScreen("investments",state,model,open,back)}
                                    motionScreen("investment_detail"){StudioServiceScreen("investment_detail",state,model,open,back)}
                                    motionScreen("bond_detail"){StudioServiceScreen("bond_detail",state,model,open,back)}
                                    motionScreen("insurance"){StudioServiceScreen("insurance",state,model,open,back)}
                                    motionScreen("insurance_detail"){StudioServiceScreen("insurance_detail",state,model,open,back)}
                                    motionScreen("credit"){StudioServiceScreen("credit",state,model,open,back)}
                                    motionScreen("certificates"){StudioServiceScreen("certificates",state,model,open,back)}
                                    motionScreen("document"){StudioServiceScreen("document",state,model,open,back)}
                                    motionScreen("card_service"){StudioServiceScreen("card_service",state,model,open,back)}
                                    motionScreen("replacement"){StudioServiceScreen("replacement",state,model,open,back)}
                                    motionScreen("account_protection"){StudioServiceScreen("account_protection",state,model,open,back)}
                                    motionScreen("schedule_new"){StudioServiceScreen("schedule_new",state,model,open,back)}
                                    motionScreen("inquiry"){StudioServiceScreen("inquiry",state,model,open,back)}
                                    motionScreen("cases"){StudioServiceScreen("cases",state,model,open,back)}
                                    motionScreen("terms"){StudioServiceScreen("terms",state,model,open,back)}
                                }
                            }
                        }
                    }

                    if(rootRoute&&!adaptive.useRail){
                        SolidBottomNavigation(
                            tabs,selectedRoute,
                            Modifier.align(Alignment.BottomCenter).padding(horizontal=14.dp,vertical=8.dp)
                        ){destination->
                            nav.navigate(destination){
                                popUpTo(nav.graph.findStartDestination().id){saveState=true}
                                launchSingleTop=true;restoreState=true
                            }
                        }
                    }
                }
            }
        }
    }

    interaction.authChallenge?.let{challenge->
        ModalBottomSheet(
            onDismissRequest={model.cancelAuthorization()},
            sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
            containerColor=TraceColors.Surface
        ){
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=24.dp)){
                Text("송금 인증",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics{heading()})
                Space(12);Body("기기 잠금 화면에서 본인을 확인합니다.",subdued=true)
                Space(18)
                if(BuildConfig.DEBUG){
                    PrimaryButton("시연 인증",Modifier.testTag("auth_confirm"),enabled=!interaction.busy){model.authorize(challenge,AuthMethod.DEMO_CONFIRMATION)}
                }else{
                    PrimaryButton("기기 인증 다시 열기",enabled=!interaction.busy){onBiometric(challenge)}
                }
                SecondaryButton("취소"){model.cancelAuthorization()}
            }
        }
        LaunchedEffect(challenge.nonce){
            if(!BuildConfig.DEBUG||preferences.biometric)onBiometric(challenge)
        }
    }

    interaction.error?.let{message->
        AlertDialog(
            onDismissRequest=model::dismissError,
            title={Text("확인해 주세요.")},
            text={Text(message)},
            confirmButton={QuietButton("확인",onClick=model::dismissError)},
            containerColor=TraceColors.Surface
        )
    }
}

@Composable private fun StageLocked(back:()->Unit){Page(title="앱 정보",back=back){Body("시연센터를 먼저 활성화해 주세요.")}}
private fun NavGraphBuilder.motionScreen(route:String,content:@Composable (NavBackStackEntry)->Unit){
    composable(route){entry->CompositionLocalProvider(LocalMotionVisibility provides this){content(entry)}}
}
