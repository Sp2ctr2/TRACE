from pathlib import Path
import json,shutil,sys
here=Path(__file__).resolve().parent
root=Path(sys.argv[1] if len(sys.argv)>1 else 'android-native')
(here/'TraceScene.kt').write_text(''.join((here/f'scene_{i}.part').read_text() for i in range(4)))
(here/'StudioIntegration.kt').write_text(''.join((here/f'bridge_{i}.part').read_text() for i in range(2)))
def patch(path,old,new):
 p=root/path;s=p.read_text()
 if old not in s: raise RuntimeError('Patch anchor missing: '+path+' :: '+old[:100])
 p.write_text(s.replace(old,new))
lib=root/'trace-design';(lib/'src/main/kotlin/dev/trace/scene').mkdir(parents=True,exist_ok=True)
shutil.copy2(here/'TraceScene.kt',lib/'src/main/kotlin/dev/trace/scene/TraceScene.kt')
(lib/'src/main/AndroidManifest.xml').write_text('<manifest/>')
(lib/'build.gradle.kts').write_text('''plugins { id("com.android.library"); kotlin("android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "dev.trace.scene"; compileSdk = 35
 defaultConfig { minSdk = 26; consumerProguardFiles("consumer-rules.pro") }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 api("androidx.compose.material3:material3")
 api("androidx.compose.foundation:foundation")
 api("androidx.compose.animation:animation")
}
''')
(lib/'consumer-rules.pro').write_text('# No reflection or generated serialization keep rules.\n')
patch('settings.gradle.kts','include(":app", ":core")','include(":app", ":core", ":trace-design")')
patch('build.gradle.kts','plugins {','plugins {\n    id("com.android.library") version "8.9.2" apply false')
patch('app/build.gradle.kts','implementation(project(":core"))','implementation(project(":core"))\n    implementation(project(":trace-design"))')
patch('app/build.gradle.kts','versionCode = 6','versionCode = 7')
patch('app/build.gradle.kts','versionName = "2.0.0-demo"','versionName = "3.0.0-studio"')
ui='app/src/main/kotlin/app/saeon/trace/ui/'
shutil.copy2(here/'StudioIntegration.kt',root/ui/'screens/StudioIntegration.kt')
shutil.copy2(here/'SceneDeviceTest.kt',root/'app/src/androidTest/kotlin/app/saeon/trace/SceneDeviceTest.kt')
assets=root/'app/src/main/assets/trace';assets.mkdir(parents=True,exist_ok=True)
shutil.copy2(here/'design.json',assets/'design.json')
patch(ui+'SaeonApp.kt','    val bank by model.bank.collectAsStateWithLifecycle()', '''    val context = androidx.compose.ui.platform.LocalContext.current
    val designStore = remember { BankDesignStore.get(context) }
    val studioDesign by designStore.document.collectAsStateWithLifecycle()
    LaunchedEffect(designStore) { designStore.load() }
    val bank by model.bank.collectAsStateWithLifecycle()''')
for route,screen,old in [('home','home','QuietHome(state, preferences, model, open)'),('assets','assets','QuietAssets(state, open)'),('transfer','recipient','QuietRecipients(state, preferences, model, open)'),('amount','amount','QuietAmount(state, preferences, model, open, back)')]:
 patch(ui+'SaeonApp.kt',f'composable("{route}") {{ {old} }}',f'''composable("{route}") {{ val design = studioDesign
                            if(design != null) StudioBankScreen(design, "{screen}", state, preferences, interaction, model, open, back, home)
                            else {old}
                        }}''')
patch(ui+'SaeonApp.kt','composable("transfer_state") { QuietTransfer(state, preferences, interaction, model, open, back, home) }','''composable("transfer_state") { val design = studioDesign
                            if(design != null) StudioTransferScreen(design, state, preferences, interaction, model, open, back, home)
                            else QuietTransfer(state, preferences, interaction, model, open, back, home)
                        }''')
patch(ui+'SaeonApp.kt','composable("demo_center")','composable("studio_design") { StudioDesignScreen(model, back) }\n                        composable("demo_center")')
p=root/ui/'screens/DemoCenterScreens.kt';s=p.read_text();i=s.index('fun QuietMore(');a=s[:i];b=s[i:];idx=b.index('MenuRow("시연 센터",');end=b.index('\n',idx);b=b[:end+1]+'        MenuRow("은행 디자인", "Trace Studio 프로젝트 가져오기", tag = "studio_design_open") { open("studio_design") }\n'+b[end+1:];p.write_text(a+b)
patch('tools/run-device-suite.sh','app.saeon.trace.DemoCenterTest\n','app.saeon.trace.DemoCenterTest,app.saeon.trace.SceneDeviceTest\n')
(root/'STUDIO3_INTEGRATION.md').write_text('''# Trace Studio 3 native integration

The app is named Trace (3.0.0-studio). The two reading modes are standard and senior.
More > 은행 디자인 > 프로젝트 열기 imports a Trace Studio JSON using Android SAF.
Preview is read-only. Apply writes the design with AtomicFile, separately from the banking ledger.
Home/assets/payee/amount/review/HOLD/VERIFY/UNKNOWN/official-route can use the imported design.
Other security/settings screens remain bank-owned. Resetting the design does not reset money.

trace-design is an AAR-capable native Compose renderer. It has no network or financial authority.
The host gates every event against current domain state. Mandatory safety copy and safe actions
are restored even if a JSON document attempts to remove them. Source includes nested frames,
vector paths and boolean operations, fixed/free layouts, images, finite keyframes and block states.

Build: gradle :core:test :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :trace-design:assembleRelease :app:lintDebug
The first build downloads official Android dependencies and the reference app font.
Source bundles must exclude *.ttf and *.otf. Type resources are recreated at build.
''')
print('Studio 3 applied to',root)
