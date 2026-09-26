#!/usr/bin/env python3
from pathlib import Path
import shutil

root = Path("android-native")
ui = root / "app/src/main/kotlin/app/saeon/trace/ui"
screens = ui / "screens"
design = root / "trace-ui/src/main/kotlin/app/saeon/trace/ui/design"

def edit(path, old, new):
    text = path.read_text()
    if old not in text:
        raise RuntimeError(f"missing anchor in {path}: {old[:100]}")
    path.write_text(text.replace(old, new))

shutil.copy2("final/DemoCenter.kt", ui / "DemoCenter.kt")
shutil.copy2("final/OrbitLoader.kt", design / "OrbitLoader.kt")
shutil.copy2("final/FinalBankScreens.kt", screens / "FinalBankScreens.kt")
shutil.copy2("final/AllScreensTest.kt", root / "app/src/androidTest/kotlin/app/saeon/trace/AllScreensTest.kt")

# Fix one icon alias that is intentionally app-local, not part of the reusable icon set.
p = ui / "DemoCenter.kt"
p.write_text(p.read_text().replace("BankIcons.Device", "BankIcons.Settings"))

# Final demo version.
p = root / "app/build.gradle.kts"
text = p.read_text()
text = text.replace("versionCode = 50", "versionCode = 60")
text = text.replace('versionName = "5.0.0-stage-demo"', 'versionName = "6.0.0-final-demo"')
p.write_text(text)

# Restore the concise circular TRACE gate: no contextual lines, just the orbiting point around the original mark.
p = design / "BankExperience.kt"
text = p.read_text()
text = text.replace("@Composable fun ContextLoading(cancel:()->Unit) { ContextWeave(cancel) }",
                    "@Composable fun ContextLoading(cancel:()->Unit) { OrbitContextLoading(cancel) }")
p.write_text(text)

# Final bank screens + hidden demo-center destination.
p = ui / "SaeonApp.kt"
text = p.read_text()
text = text.replace("ViewportHome(state, preferences, model, open)", "FinalHome(state, preferences, model, open)")
text = text.replace('motionScreen("presenter") { PresenterScreen(model,open) }',
                    'motionScreen("presenter") { DemoCenterScreen(model,open) }')
text = text.replace('if(stage.unlocked)Triple("presenter","발표",BankIcons.Play)else Triple("more","전체",BankIcons.More)',
                    'if(stage.unlocked)Triple("presenter","시연",BankIcons.Trace)else Triple("more","전체",BankIcons.More)')
text = text.replace('Page(title="앱 정보",back=back){Body("발표자 모드를 먼저 활성화해 주세요.")}',
                    'Page(title="앱 정보",back=back){Body("시연센터를 먼저 활성화해 주세요.")}')
p.write_text(text)

# Transfer confirmation uses the last polished layout. Leave HOLD/VERIFY state logic intact.
p = screens / "TransferScreens.kt"
text = p.read_text()
text = text.replace("ViewportReview(state, record, interaction, model, open, back)",
                    "FinalReview(state, record, interaction, model, open, back)")
text = text.replace("ReviewScreen(state, record, interaction, model, open, back)",
                    "FinalReview(state, record, interaction, model, open, back)")
p.write_text(text)

# App-info secret now describes the demo center, not a presenter console.
p = screens / "SettingsScreens.kt"
text = p.read_text()
text = text.replace("발표자 모드까지", "시연센터까지")
text = text.replace("발표자 모드를 켤까요?", "시연센터를 켤까요?")
text = text.replace("하단의 전체 탭을 발표 탭으로 바꿉니다. 시연 준비 기능은 가상 계좌를 초기화할 수 있어요. 앱을 완전히 종료하면 다시 숨겨집니다.",
                    "하단의 전체 탭을 시연 탭으로 바꿉니다. 각 시연은 가상 계좌와 맥락을 준비하지만 송금 확인·인증·TRACE 판단은 그대로 거칩니다. 앱을 완전히 종료하면 다시 숨겨집니다.")
p.write_text(text)

# Slight HOLD polish only: preserve the accepted layout and timeline, reduce duplicate chrome.
p = screens / "ViewportScreens.kt"
text = p.read_text()
text = text.replace('TaskHeadline("잠깐, 확인하고\\n보내볼까요?","확인하고 보내세요")',
                    'TaskHeadline("잠깐, 확인하고\\n보내볼까요?","확인하고 보내세요")')
text = text.replace('TaskGap(12);Body(if(LocalTaskDense.current)"직전 요청과 연결된 송금이에요."else"방금 전의 요청이\\n이 송금과 이어져 있어요.",subdued=true);TaskGap(16)',
                    'TaskGap(10);Body(if(LocalTaskDense.current)"직전 요청과 연결된 송금이에요."else"방금 전의 요청이\\n이 송금과 이어져 있어요.",subdued=true);TaskGap(14)')
p.write_text(text)

# Exact TRACE web mark for launcher. The second lower-left path is the small point-like detail in the real site mark.
launcher = root / "app/src/main/res/drawable/ic_launcher.xml"
launcher.parent.mkdir(parents=True, exist_ok=True)
launcher.write_text("""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#F5F4F0" android:pathData="M0,0H108V108H0Z"/>
    <group android:scaleX="1.82" android:scaleY="1.82" android:translateX="17.6" android:translateY="17.6">
        <path android:fillColor="#EF4A32" android:pathData="M6,9h28v7H23.5v17h-7V16H6z"/>
        <path android:fillColor="#EF4A32" android:pathData="M6,23h7v10H6z"/>
    </group>
</vector>
""")

# Preserve the canonical mark as a standalone vector asset too.
mark = root / "app/src/main/res/drawable/trace_mark.xml"
mark.write_text("""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="40dp" android:height="40dp"
    android:viewportWidth="40" android:viewportHeight="40">
    <path android:fillColor="#EF4A32" android:pathData="M6,9h28v7H23.5v17h-7V16H6z"/>
    <path android:fillColor="#EF4A32" android:pathData="M6,23h7v10H6z"/>
</vector>
""")

print("Applied final bank polish, circular TRACE gate, hidden demo center, exact website launcher mark, and full-screen capture suite.")
