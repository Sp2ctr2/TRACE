package app.saeon.trace.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import app.saeon.trace.core.*
import app.saeon.trace.ui.design.*
import app.saeon.trace.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaeonApp(
    model: BankViewModel, safety: SafetyViewModel,
    onNavigationReady: (NavHostController) -> Unit,
    onBiometric: (AuthorizationChallenge) -> Unit,
    onVoice: () -> Unit, onStopVoice: () -> Unit, voiceActive: Boolean
) {
    val bank by model.bank.collectAsStateWithLifecycle()
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val interaction by model.interaction.collectAsStateWithLifecycle()
    val fatal by model.fatal.collectAsStateWithLifecycle()
    val shared by safety.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val roots = setOf("home", "assets", "transfer", "safety", "more")
    val haptics = LocalHapticFeedback.current
    var lastState by remember { mutableStateOf<Pair<String, TransferStage>?>(null) }
    LaunchedEffect(nav) { onNavigationReady(nav) }
    LaunchedEffect(shared.delivery) { if (shared.delivery != 0L) nav.navigate("manual") { launchSingleTop = true } }
    LaunchedEffect(bank?.current?.intent?.id, bank?.current?.stage) {
        bank?.current?.let { record ->
            val current = record.intent.id to record.stage
            if (lastState != null && current != lastState) {
                if (record.stage == TransferStage.HOLD) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (record.stage == TransferStage.COMPLETE) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            lastState = current
        }
    }
    val open: (String) -> Unit = { destination -> nav.navigate(destination) { launchSingleTop = true } }
    val back: () -> Unit = { if (!nav.popBackStack()) nav.navigate("home") { launchSingleTop = true } }
    val home: () -> Unit = { nav.navigate("home") { popUpTo("home") { inclusive = false }; launchSingleTop = true } }
    Surface(color = TraceColors.Paper, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            if (fatal != null) {
                Page(title = "새온은행", footer = { PrimaryButton("거래 상태 다시 읽기", enabled = !interaction.busy) { model.retryStorage() } }) {
                    Space(32); Headline("거래 상태를\n먼저 확인해야 해요."); Space(20); Body(fatal!!)
                    Space(24); Caption("저장된 잔액이나 내역을 임의로 초기화하지 않습니다.")
                }
            } else if (bank == null) {
                Page(title = "새온은행") { Space(48); Headline("계좌 정보를\n불러오고 있어요."); Space(20); Caption("이 기기에 저장된 거래 상태를 확인합니다.") }
            } else {
                val state = bank!!
                Box(Modifier.weight(1f)) {
                    NavHost(navController = nav, startDestination = "home",
                        enterTransition = { fadeIn(tween(150)) }, exitTransition = { fadeOut(tween(120)) },
                        popEnterTransition = { fadeIn(tween(150)) }, popExitTransition = { fadeOut(tween(120)) }) {
                        composable("home") { HomeScreen(state, preferences, model, open) }
                        composable("assets") { AssetsScreen(state, open) }
                        composable("account") { AccountScreen(state, open, back) }
                        composable("savings") { SavingsScreen(state, open, back) }
                        composable("card") { CardScreen(state, open, back) }
                        composable("loan") { LoanScreen(state, model, open, back) }
                        composable("bring") { BringScreen(state, model, open, back) }
                        composable("history") { HistoryScreen(state, open, back) }
                        composable("receipt/{receiptId}") { target ->
                            ReceiptScreen(state.receipts.find { it.id == target.arguments?.getString("receiptId") }, open, back)
                        }
                        composable("transfer") { RecipientScreen(state, model, open) }
                        composable("recipient_entry") { RecipientEntryScreen(state, model, open, back) }
                        composable("amount") { AmountScreen(state, model, open, back) }
                        composable("transfer_state") { TransferStateScreen(state, preferences, interaction, model, open, back, home) }
                        composable("safety") { SafetyScreen(state, model, open) }
                        composable("pending") { PendingScreen(state, model, open, back) }
                        composable("timeline") { TimelineScreen(state, back) }
                        composable("safety_guide") { SafetyGuideScreen(state, open, back) }
                        composable("privacy") { PrivacyScreen(state, model, back) }
                        composable("manual") {
                            DisposableEffect(Unit) { onDispose { onStopVoice() } }
                            ManualCheckScreen(safety, open, { onStopVoice(); safety.clear(); back() }, onVoice, onStopVoice, voiceActive)
                        }
                        composable("more") { MoreScreen(open) }
                        composable("profile") { ProfileScreen(back) }
                        composable("security") { SecurityScreen(preferences, model, open, back) }
                        composable("transfer_settings") { TransferSettingsScreen(state, model, open, back) }
                        composable("favorites") { FavoritesScreen(state, model, back) }
                        composable("recurring") { RecurringScreen(state, model, back) }
                        composable("notifications") { NotificationsScreen(state, preferences, model, open, back) }
                        composable("accessibility") { AccessibilityScreen(preferences, model, back) }
                        composable("support") { SupportScreen(open, back) }
                        composable("help") { HelpScreen(back) }
                        composable("app_info") { AppInfoScreen(open, back) }
                        composable("demo_lab") { DemoLabScreen(state, model, home, back) }
                    }
                }
                if (route in roots) {
                    Rule()
                    val tabs = listOf(
                        Triple("home", "홈", BankIcons.Home),
                        Triple("assets", "자산", BankIcons.Assets),
                        Triple("transfer", "송금", BankIcons.Transfer),
                        Triple("safety", "안전", BankIcons.Shield),
                        Triple("more", "전체", BankIcons.More)
                    )
                    Row(Modifier.fillMaxWidth().background(TraceColors.SurfaceRaised)) {
                        tabs.forEach { (destination, label, icon) ->
                            val selected = destination == route
                            Column(
                                Modifier.weight(1f).heightIn(min = 64.dp)
                                    .clickable(role = Role.Tab) {
                                        nav.navigate(destination) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .testTag("nav_$destination")
                                    .semantics { this.selected = selected; contentDescription = label },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(Modifier.height(3.dp).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                                    if (selected) Box(Modifier.width(22.dp).height(2.dp).background(TraceColors.Coral))
                                }
                                Spacer(Modifier.height(7.dp))
                                Icon(icon, null, Modifier.size(21.dp), tint = if (selected) TraceColors.Ink else TraceColors.Muted)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) TraceColors.Ink else TraceColors.Muted,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    interaction.authChallenge?.let { challenge ->
        ModalBottomSheet(onDismissRequest = { model.cancelAuthorization() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = TraceColors.Surface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                Text("보내는 분을 확인할게요.", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Space(12); Body("이 앱은 가상 거래를 시연합니다. 인증을 마치면 거래 앞의 위험 정황을 별도로 확인해요.", subdued = true)
                Space(24)
                PrimaryButton("시연 확인", Modifier.testTag("auth_confirm"), enabled = !interaction.busy) { model.authorize(challenge, AuthMethod.DEMO_CONFIRMATION) }
                QuietButton("기기 생체 인증 사용", Modifier.fillMaxWidth()) { onBiometric(challenge) }
                SecondaryButton("취소") { model.cancelAuthorization() }
            }
        }
        LaunchedEffect(challenge.nonce, preferences.biometric) { if (preferences.biometric) onBiometric(challenge) }
    }
    interaction.error?.let { message ->
        AlertDialog(onDismissRequest = model::dismissError, title = { Text("확인해 주세요.") }, text = { Text(message) },
            confirmButton = { QuietButton("확인", onClick = model::dismissError) }, containerColor = TraceColors.Surface)
    }
}
