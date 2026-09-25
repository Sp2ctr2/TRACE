#!/usr/bin/env python3
"""Reproducible design overlay for the known 6b411325 native source.
No production website writes. No bank engine or authorization bypasses.
"""
from pathlib import Path
import shutil
root=Path('android-native')
ui=root/'app/src/main/kotlin/app/saeon/trace/ui'
def edit(p,old,new):
    text=p.read_text(); assert old in text, f'Missing anchor: {p}: {old[:80]}'
    p.write_text(text.replace(old,new))
def write(p,s): p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
edit(root/'build.gradle.kts','id("com.android.application") version "8.9.2" apply false','id("com.android.application") version "8.9.2" apply false\n    id("com.android.library") version "8.9.2" apply false')
with (root/'settings.gradle.kts').open('a') as f:f.write('\ninclude(":trace-ui")\n')
edit(root/'app/build.gradle.kts','implementation(project(":core"))','implementation(project(":core"))\n    implementation(project(":trace-ui"))')
edit(root/'app/build.gradle.kts','versionCode = 1','versionCode = 25')
edit(root/'app/build.gradle.kts','versionName = "1.0.0-demo"','versionName = "2.0.0-experience-demo"')
write(root/'trace-ui/build.gradle.kts','''plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "app.saeon.trace.design"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
''')
lib=root/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
lib.parent.mkdir(parents=True,exist_ok=True)
shutil.move(str(ui/'design'),str(lib))
p=lib/'Theme.kt'
s=p.read_text()
a=s.index('object TraceColors {');b=s.index('val LocalEasyMode',a)
s=s[:a]+'''@Immutable
data class TracePalette(
    val Paper: Color, val Surface: Color, val Ink: Color, val Muted: Color,
    val Divider: Color, val Coral: Color, val Deep: Color, val CoralText: Color,
    val InputOutline: Color, val CoralLight: Color, val White: Color
)
object TracePalettes {
    val Light = TracePalette(Color(0xFFF5F4F0), Color(0xFFFBFAF7), Color(0xFF20211F),
        Color(0xFF676960), Color(0xFFDDDDD5), Color(0xFFEF4A32), Color(0xFFC93824),
        Color(0xFFB83020), Color(0xFF8B8D82), Color(0xFFFAE7DF), Color.White)
    val Dark = TracePalette(Color(0xFF171B19), Color(0xFF202622), Color(0xFFF5F4F0),
        Color(0xFFACB5AE), Color(0xFF3D4840), Color(0xFFFF8A75), Color(0xFFFF8A75),
        Color(0xFFFFAA99), Color(0xFF829086), Color(0xFF3A2823), Color(0xFF251B17))
}
val LocalTracePalette = staticCompositionLocalOf { TracePalettes.Light }
val LocalReducedMotion = staticCompositionLocalOf { false }
object TraceColors {
    val Paper: Color @Composable get() = LocalTracePalette.current.Paper
    val Surface: Color @Composable get() = LocalTracePalette.current.Surface
    val Ink: Color @Composable get() = LocalTracePalette.current.Ink
    val Muted: Color @Composable get() = LocalTracePalette.current.Muted
    val OriginalMuted: Color @Composable get() = LocalTracePalette.current.Muted
    val Divider: Color @Composable get() = LocalTracePalette.current.Divider
    val Coral: Color @Composable get() = LocalTracePalette.current.Coral
    val Deep: Color @Composable get() = LocalTracePalette.current.Deep
    val CoralText: Color @Composable get() = LocalTracePalette.current.CoralText
    val InputOutline: Color @Composable get() = LocalTracePalette.current.InputOutline
    val CoralLight: Color @Composable get() = LocalTracePalette.current.CoralLight
    // Semantic on-action color, not a constant white in dark appearance.
    val White: Color @Composable get() = LocalTracePalette.current.White
}
''' +s[b:]
s=s.replace('fun SaeonTheme(easy: Boolean = false, content:', 'fun SaeonTheme(easy: Boolean = false, dark: Boolean = false, reduced: Boolean = false, content:')
s=s.replace('CompositionLocalProvider(LocalEasyMode provides easy)', 'CompositionLocalProvider(LocalEasyMode provides easy, LocalTracePalette provides if (dark) TracePalettes.Dark else TracePalettes.Light, LocalReducedMotion provides reduced)')
# All semantic channels are explicitly assigned below. Select the proper baseline as well.
s=s.replace('colorScheme = lightColorScheme(', 'colorScheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(')
p.write_text(s)
p=lib/'Components.kt'
edit(p,'Text(number.toString().padStart(2, \'0\'), Modifier.widthIn(min = 25.dp), style = MaterialTheme.typography.labelMedium,\n            color = if (accent) TraceColors.CoralText else TraceColors.Muted)', 'Icon(BankIcons.Info, null, Modifier.size(21.dp).padding(top = 2.dp), tint = if (accent) TraceColors.CoralText else TraceColors.Muted)')
edit(p,'RoundedCornerShape(12.dp)).padding(18.dp), content = content)', 'RoundedCornerShape(18.dp)).padding(20.dp), content = content)')
edit(p,'Caption("시연용 가상 거래 · 실제 자금 이동 없음")','Caption("시연용 가상 거래입니다. 실제 자금은 이동하지 않습니다.")')
# Dynamic appearance, persisted separately from presentation fixtures.
p=root/'app/src/main/kotlin/app/saeon/trace/data/Preferences.kt'
edit(p,'val favoriteIds: Set<String> = setOf("seoyeon", "family", "minjun")','val favoriteIds: Set<String> = setOf("seoyeon", "family", "minjun"),\n    val themeMode: String = "light", val reducedMotion: Boolean = false')
edit(p,'private val favorites = stringSetPreferencesKey("favorites")','private val favorites = stringSetPreferencesKey("favorites")\n    private val theme = stringPreferencesKey("theme")\n    private val motion = booleanPreferencesKey("reduced_motion")')
edit(p,'it[favorites] ?: setOf("seoyeon", "family", "minjun"))','it[favorites] ?: setOf("seoyeon", "family", "minjun"), it[theme] ?: "light", it[motion] ?: false)')
edit(p,'suspend fun easy(value: Boolean)', 'suspend fun theme(value: String) { require(value in setOf("light", "dark", "system")); store.edit { it[theme] = value } }\n    suspend fun motion(value: Boolean) { store.edit { it[motion] = value } }\n    suspend fun easy(value: Boolean)')
p=root/'app/src/main/kotlin/app/saeon/trace/MainActivity.kt'
edit(p,'SaeonTheme(preferences.easyMode) {','''val dark = preferences.themeMode == "dark" || (preferences.themeMode == "system" && androidx.compose.foundation.isSystemInDarkTheme())
            val noMotion = preferences.reducedMotion || !android.animation.ValueAnimator.areAnimatorsEnabled()
            SideEffect {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            SaeonTheme(preferences.easyMode, dark, noMotion) {''')
p=ui/'SaeonApp.kt'
edit(p,'val roots = setOf(', 'val reduced = LocalReducedMotion.current\n    val roots = setOf(')
edit(p,'fadeIn(tween(150))','fadeIn(tween(if (reduced) 0 else 180))')
edit(p,'fadeOut(tween(120))','fadeOut(tween(if (reduced) 0 else 120))')
edit(p,'composable("accessibility") {', 'composable("appearance") { AppearanceScreen(preferences, model, back) }\n                        composable("accessibility") {')
p=ui/'screens/SettingsScreens.kt'
edit(p,'MenuRow("접근성",','MenuRow("화면 설정", "라이트·다크·동작 줄이기", BankIcons.Settings, tag = "appearance_open") { open("appearance") }\n        MenuRow("접근성",')
with p.open('a') as f:f.write('''
@Composable fun AppearanceScreen(preferences: BankPreferences, model: BankViewModel, back: () -> Unit) {
    Page(title = "화면 설정", tag = "appearance", back = back) {
        Space(16); Headline("보기 편한 화면으로"); Space(12)
        Body("선택한 화면은 앱을 다시 열어도 유지돼요.", subdued = true); Space(24)
        listOf("light" to "라이트", "dark" to "다크", "system" to "기기 설정 따르기").forEach { (key, label) ->
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.RadioButton) {
                model.preference { theme(key) }
            }.testTag("theme_$key").semantics { selected = preferences.themeMode == key },
                verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                RadioButton(selected = preferences.themeMode == key, onClick = null)
            }
        }
        Space(24); SurfaceBox {
            Caption("화면 미리보기"); Space(12); Text("새온 생활통장", style = MaterialTheme.typography.titleMedium)
            Space(12); Money(12840000); Space(10); Caption("색상은 잔액과 거래 상태를 바꾸지 않아요.")
        }
        Space(24); Rule(); Space(12)
        OptionRow("동작 줄이기", preferences.reducedMotion, "송금 경로와 화면 전환을 움직임 없이 표시합니다. 기기의 애니메이션 끄기도 존중해요.") { model.preference { motion(it) } }
        Space(20); Caption("시연의 기본 화면은 라이트입니다. 위험을 알릴 때도 화면 전체를 빨강이나 초록으로 바꾸지 않습니다.")
    }
}
''')
# A distinct account object, not a dashboard full of identical cards.
p=ui/'screens/BankingScreens.kt'
edit(p,'        Space(16)\n        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {','        Space(6)\n        SurfaceBox {\n        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {')
edit(p,'        Space(16); Rule(); Space(20)\n        SectionTitle("최근 거래"','        }\n        Space(26)\n        SectionTitle("최근 거래"')
p=ui/'BankViewModel.kt'
edit(p,'delay(550)', 'delay(950)')
edit(p,'graph.preferences.reset()','val appearance = preferences.value\n        graph.preferences.reset()\n        graph.preferences.theme(appearance.themeMode)\n        graph.preferences.motion(appearance.reducedMotion)')
write(lib/'TransferJourney.kt','''package app.saeon.trace.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Visual state only. The host must supply Delivered only AFTER ledger confirmation. */
enum class JourneyPhase { Checking, Held, Delivered }
@Composable fun TransferJourney(recipient: String, phase: JourneyPhase, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val progress = remember(phase) { Animatable(if (phase == JourneyPhase.Delivered) .5f else .08f) }
    val target = if (phase == JourneyPhase.Delivered) .92f else .5f
    LaunchedEffect(phase, reduced) {
        if (reduced || phase == JourneyPhase.Held) progress.snapTo(target)
        else progress.animateTo(target, tween(if (phase == JourneyPhase.Delivered) 1000 else 750, easing = FastOutSlowInEasing))
    }
    val paper = TraceColors.Paper; val line = TraceColors.Divider; val coral = TraceColors.Coral
    val caption = when(phase) { JourneyPhase.Checking -> "송금 맥락 확인 중"; JourneyPhase.Held -> "송금 보류. 돈은 이동하지 않음"; JourneyPhase.Delivered -> "송금 완료 기록 확인됨" }
    Column(modifier.fillMaxWidth().testTag("transfer_journey").semantics { contentDescription = caption }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().height(90.dp)) {
                val y = size.height/2
                drawLine(line, Offset(size.width*.08f,y),Offset(size.width*.92f,y), 3.dp.toPx(), StrokeCap.Round)
                drawLine(coral, Offset(size.width*.08f,y),Offset(size.width*progress.value,y), 3.dp.toPx(), StrokeCap.Round)
                drawCircle(paper, 17.dp.toPx(), Offset(size.width*.5f,y))
                drawCircle(coral, 7.dp.toPx(), Offset(size.width*progress.value,y))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(TraceColors.Surface, CircleShape),contentAlignment = Alignment.Center) { AppIcon(BankIcons.Bank, size=23) }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(42.dp).background(TraceColors.Paper, CircleShape),contentAlignment = Alignment.Center) { AppIcon(if(phase == JourneyPhase.Held) BankIcons.Pause else BankIcons.Trace, tint=TraceColors.Coral,size=26) }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(46.dp).background(TraceColors.Surface, CircleShape),contentAlignment = Alignment.Center) {
                    if(phase==JourneyPhase.Delivered && progress.value>.90f) AppIcon(BankIcons.Check,size=25)
                    else Text(recipient.take(1), style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Caption("내 계좌"); Caption("안전 확인"); Caption(recipient) }
    }
}

/** Bank integration presentation contract. This component never approves a payment. */
enum class ProtectionKind { Warn, Verify, Hold, Unknown }
@Immutable data class ProtectionReason(val title: String, val explanation: String)
@Immutable data class ProtectionPresentation(val kind: ProtectionKind, val recipient: String, val amount: Long,
    val reasons: List<ProtectionReason>, val summary: String)
@Composable fun TraceProtectionPanel(value: ProtectionPresentation, onCancel: () -> Unit,
    onIndependentRoute: () -> Unit, onWarnAcknowledged: () -> Unit, onRetry: () -> Unit) {
    var acknowledged by remember(value) { mutableStateOf(false) }
    val title = when(value.kind) { ProtectionKind.Warn -> "보내기 전에 하나만 확인해 주세요."; ProtectionKind.Verify -> "공식 경로로 확인해 볼까요?"; ProtectionKind.Hold -> "잠깐, 확인하고 보내볼까요?"; ProtectionKind.Unknown -> "확인되지 않으면 보내지 않아요." }
    Page(title="송금 안전 확인", footer={
        when(value.kind) {
            ProtectionKind.Warn -> PrimaryButton("확인한 내용으로 다시 보기",enabled=acknowledged,onClick=onWarnAcknowledged)
            ProtectionKind.Unknown -> PrimaryButton("다시 확인하기",onClick=onRetry)
            else -> PrimaryButton("공식 경로로 확인하기",onClick=onIndependentRoute)
        }
        SecondaryButton("송금 취소",onClick=onCancel)
    }) {
        TraceSignature("Protected by TRACE"); Space(28); Headline(title); Space(14); Body(value.summary,subdued=true)
        Space(20); Body("아직 돈은 나가지 않았습니다."); Space(26)
        SurfaceBox { Money(value.amount,hero=false); Space(8); Body("${value.recipient}님에게") }
        Space(20); value.reasons.forEach { NumberedReason(0,it.title,it.explanation) }
        if(value.kind==ProtectionKind.Warn) OptionRow("다른 경로로 받는 분과 금액을 확인했어요.",acknowledged) { acknowledged=it }
    }
}
''')
p=ui/'screens/TransferScreens.kt'
edit(p,'TransferStage.EVALUATING -> EvaluatingScreen {','TransferStage.EVALUATING -> EvaluatingScreen(record.intent.recipient.name) {')
a=p.read_text();start=a.index('@Composable private fun EvaluatingScreen(');end=a.index('@OptIn(ExperimentalMaterial3Api::class)',start)
a=a[:start]+'''@Composable private fun EvaluatingScreen(recipient: String, cancel: () -> Unit) {
    Page(title = "송금 확인", tag = "trace_evaluating", footer = { SecondaryButton("취소하고 돌아가기", onClick = cancel) }) {
        Space(28); TraceSignature("Protected by TRACE"); Space(28)
        Headline("송금 앞의 맥락을\\n확인하고 있어요."); Space(24)
        TransferJourney(recipient, JourneyPhase.Checking); Space(32)
        Body("요청의 목적과 받는 분을 함께 살펴봐요.", subdued=true)
        Space(14); Caption("이 확인만으로 돈이 이동하지는 않습니다.")
    }
}

'''+a[end:];p.write_text(a)
edit(p,'Space(36); AppIcon(BankIcons.Check, size = 42); Space(30)', 'Space(22); TransferJourney(receipt.recipient.name, JourneyPhase.Delivered); Space(32)')
edit(p,'Headline("잠깐,\\n보내지 않아도 괜찮아요.")','Headline("잠깐, 확인하고\\n보내볼까요?")')
# Static palette contrast tests remain useful; runtime color access is composition-scoped.
p=root/'app/src/androidTest/kotlin/app/saeon/trace/AuditMeasurementTest.kt'
s=p.read_text().replace('import app.saeon.trace.ui.design.TraceColors','import app.saeon.trace.ui.design.TracePalettes').replace('TraceColors.','TracePalettes.Light.').replace('Color(0xFFD93B25)','Color(0xFFC93824)');p.write_text(s)
write(root/'app/src/androidTest/kotlin/app/saeon/trace/ExperienceTest.kt','''package app.saeon.trace
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.saeon.trace.core.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
@RunWith(AndroidJUnit4::class)
class ExperienceTest : UiHarness() {
    @Test fun appearanceAndPolicyScreens() {
        fresh(); capture("experience_light_home",audit=false)
        navigate("appearance"); tap("theme_dark")
        compose.waitUntil(10000) { compose.activity.model.preferences.value.themeMode=="dark" }
        capture("experience_dark_appearance",audit=false)
        navigate("home"); capture("experience_dark_home",audit=false)
        for (scenario in listOf(DemoScenario.NORMAL,DemoScenario.IMPERSONATION,DemoScenario.WARN,DemoScenario.LOAN,DemoScenario.UNKNOWN)) {
            evaluated(scenario)
            runBlocking { graph.preferences.theme("dark") }
            compose.waitUntil(10000) { compose.activity.model.preferences.value.themeMode=="dark" }
            capture("experience_dark_${scenario.name}",audit=false)
            runBlocking { graph.preferences.theme("light") }
            compose.waitUntil(10000) { compose.activity.model.preferences.value.themeMode=="light" }
            capture("experience_light_${scenario.name}",audit=false)
        }
    }
    @Test fun actualAnimatedNormalTransfer() {
        fresh(); createReview(DemoScenario.NORMAL)
        val before=state.balance
        confirmThroughUi("transfer_complete")
        assertEquals(before-32000,state.balance)
        compose.onNodeWithTag("transfer_journey").assertExists()
        capture("experience_animated_complete",audit=false)
    }
    @Test fun reducedMotionStillEnforcesHold() {
        fresh(DemoScenario.IMPERSONATION)
        runBlocking { graph.preferences.motion(true) }
        createReview(DemoScenario.IMPERSONATION)
        val before=state.balance
        confirmThroughUi("trace_hold")
        assertEquals(before,state.balance)
        compose.onAllNodesWithTag("transfer_complete").assertCountEquals(0)
        capture("experience_reduced_hold",audit=false)
    }
}
''')
write(root/'trace-ui/INTEGRATION.md','''# TRACE bank-embedded presentation module

This Android library contains the theme, vector icons, shared components, `TransferJourney`, and `TraceProtectionPanel`. It has no scenario injection or demo banking dependency.

Use `SaeonTheme(dark = hostDark, reduced = userReducedMotion)` around your host or a protection subtree. `TraceProtectionPanel` accepts immutable display data and callbacks only. The bank host must revalidate transaction/context binding, authorization freshness, and the policy result on every payment attempt. A UI callback is NOT a payment authorization. HOLD and UNKNOWN have no continue callback. VERIFY routes to a host-owned independently trusted channel and requires a new transaction and authentication.

`JourneyPhase.Delivered` may only be supplied after a confirmed bank ledger result. Checking stops at the center gate; reduced motion jumps to the same semantic state without crossing the gate.

The `app` module is the fictional demo host. The `core` module and local gateways are demonstration implementations, not a certified banking/TRACE deployment. No cloud AI is added by this redesign. The bank integration must supply its actual inference and trusted transaction services.
''')
print('Applied native experience redesign; production website untouched.')
