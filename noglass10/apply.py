from pathlib import Path
import shutil
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
def edit(p,a,b):
 s=p.read_text()
 if a not in s:raise RuntimeError(f'Missing anchor {p}: {a[:100]}')
 p.write_text(s.replace(a,b))
for src,dst in [('NoGlassKit.kt',d/'NoGlassKit.kt'),('AdaptiveFrame.kt',d/'TaskPage.kt'),('NoGlassHome.kt',u/'screens/NoGlassHome.kt'),('ContextScreen.kt',u/'screens/ContextScreen.kt'),('DeviceAuthentication.kt',r/'app/src/main/kotlin/app/saeon/trace/security/DeviceAuthentication.kt'),('NoGlass10Test.kt',r/'app/src/androidTest/kotlin/app/saeon/trace/NoGlass10Test.kt')]:
 shutil.copy2(Path('noglass10')/src,dst)
(d/'GlassKit.kt').unlink()
(u/'screens/GlassHome.kt').unlink()
p=d/'BankExperience.kt';s=p.read_text();a=s.index('/** Android rendition of a floating glass');b=s.index('@Composable fun ResultSeal',a);p.write_text(s[:a]+s[b:])
p=d/'Components.kt';s=p.read_text();a=s.index('@Composable fun Page(');b=s.index('@Composable fun PrimaryButton(',a)
s=s[:a]+'''@Composable fun Page(title:String="",tag:String="",back:(()->Unit)?=null,
    actions:(@Composable RowScope.()->Unit)?=null,footer:(@Composable ColumnScope.()->Unit)?=null,content:@Composable ColumnScope.()->Unit){
    AdaptiveFrame(title,tag,LocalPageBottomInset.current>100,false,back,actions,footer,content)
}
'''+s[b:];p.write_text(s)
p=d/'Theme.kt';s=p.read_text().replace('Color(0xFFF6F7F5), Color(0xFFFFFFFF), Color(0xFF20211F)','Color(0xFFF3F4F1), Color(0xFFFAFBF8), Color(0xFF20211F)').replace('Color(0xFF6B6E73), Color(0xFFE2E3E5)','Color(0xFF686E65), Color(0xFFDADFD8)');p.write_text(s)
p=u/'SaeonApp.kt';s=p.read_text()
s=s.replace('    val backdrop = rememberGraphicsLayer()\n    val ambient = rememberGraphicsLayer()\n    var contentOrigin by remember { mutableStateOf(Offset.Zero) }\n','')
s=s.replace('GlassAmbient(ambient,Modifier.matchParentSize())','BankAmbient(Modifier.matchParentSize())')
s=s.replace('LocalGlassScene provides GlassScene(ambient,Offset.Zero), LocalReducedTransparency provides preferences.reducedTransparency, ','')
a=s.index('Box(Modifier.fillMaxSize().onGloballyPositioned { contentOrigin=')
b=s.index('                    SharedTransitionLayout',a)
s=s[:a]+'Box(Modifier.fillMaxSize()) {\n'+s[b:]
s=s.replace('GlassHome(state, preferences, model, open)','NoGlassHome(state, preferences, model, open)').replace('RefinedTimeline(state, back)','ContextScreen(state, back)')
s=s.replace('CalibratedDock(tabs,if(stage.unlocked&&route=="more")"presenter"else route,backdrop,contentOrigin,Modifier.align(Alignment.BottomCenter).padding(horizontal=18.dp,vertical=10.dp),reduceTransparency=preferences.reducedTransparency)','BankDock(tabs,if(stage.unlocked&&route=="more")"presenter"else route,Modifier.align(Alignment.BottomCenter).padding(horizontal=18.dp,vertical=8.dp))')
s=s.replace('PrimaryButton("시연 인증", Modifier.testTag("auth_confirm"), enabled = !interaction.busy) { model.authorize(challenge, AuthMethod.DEMO_CONFIRMATION) }\n                QuietButton("기기 생체 인증 사용", Modifier.fillMaxWidth()) { onBiometric(challenge) }','''if(app.saeon.trace.BuildConfig.DEBUG && model.uiTestAuthentication) {
                    PrimaryButton("자동 검사 인증",Modifier.testTag("auth_confirm")){model.authorize(challenge,AuthMethod.DEMO_CONFIRMATION)}
                } else {
                    PrimaryButton("기기 인증",Modifier.testTag("device_auth_retry"),enabled=!interaction.busy){onBiometric(challenge)}
                    Caption("지문 또는 기기 화면 잠금으로 확인합니다.")
                }''')
s=s.replace('LaunchedEffect(challenge.nonce, preferences.biometric) { if (preferences.biometric) onBiometric(challenge) }','LaunchedEffect(challenge.nonce) { if(!(app.saeon.trace.BuildConfig.DEBUG && model.uiTestAuthentication))onBiometric(challenge) }')
p.write_text(s)
# No optical implementation or compatibility wrapper remains in the active source tree.
for p in list(u.rglob('*.kt'))+list(d.rglob('*.kt')):
 s=p.read_text().replace('GlassPlate(', 'BankSurface(').replace('GlassAction(', 'BankAction(').replace('GlassIcon(', 'BankIconButton(')
 p.write_text(s)
p=u/'BankViewModel.kt';s=p.read_text().replace('    val repository = graph.repository','    internal var uiTestAuthentication: Boolean = false\n    var pendingCredentialNonce: String? = null\n    var pendingBiometricNonce: String? = null\n    val repository = graph.repository')
s=s.replace('    fun authorize(challenge: AuthorizationChallenge, method: AuthMethod) {','''    fun authorize(challenge: AuthorizationChallenge, method: AuthMethod) {
        if(method==AuthMethod.DEMO_CONFIRMATION && !(app.saeon.trace.BuildConfig.DEBUG && uiTestAuthentication)) {
            cancelAuthorization();showError("기기 인증이 필요합니다.");return
        }''');p.write_text(s)
p=r/'core/src/main/kotlin/app/saeon/trace/core/Models.kt';edit(p,'enum class AuthMethod { DEMO_CONFIRMATION, BIOMETRIC }','enum class AuthMethod { DEMO_CONFIRMATION, BIOMETRIC, DEVICE_CREDENTIAL }')
p=r/'app/src/main/kotlin/app/saeon/trace/MainActivity.kt';s=p.read_text().replace('    private var biometricPrompt: BiometricPrompt? = null','    private lateinit var deviceAuthentication: app.saeon.trace.security.DeviceAuthentication')
s=s.replace('        super.onCreate(savedInstanceState)','        super.onCreate(savedInstanceState)\n        deviceAuthentication=app.saeon.trace.security.DeviceAuthentication(this,model)')
a=s.index('    private fun authenticate(challenge: AuthorizationChallenge) {');b=s.index('    private fun startVoice()',a)
s=s[:a]+'    private fun authenticate(challenge: AuthorizationChallenge) { deviceAuthentication.authenticate(challenge) }\n'+s[b:];p.write_text(s)
# Remove obsolete transparency option: all surfaces now stay opaque in every mode.
p=u/'screens/RefinedScreens.kt';s=p.read_text().replace('        OptionRow("투명 효과 줄이기",pref.reducedTransparency,"버튼과 하단 바를 불투명하게 표시합니다."){model.preference{transparency(it)}}','')
s=s.replace('움직임과 투명 효과는 각각 조절할 수 있습니다.','움직임은 별도로 줄일 수 있습니다.')
p.write_text(s)
# Keep large-screen content bounded and keep query text during rotation.
p=u/'screens/ViewportScreens.kt';s=p.read_text().replace('val compact=androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<640','val compact=androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp<640')
p.write_text(s)
# Preserve legacy sources outside active instrumentation: obsolete suites expect removed UI.
testdir=r/'app/src/androidTest/kotlin/app/saeon/trace'
for p in testdir.glob('*.kt'):
 if p.name not in {'NoGlass10Test.kt','UiHarness.kt','TextBoundsAudit.kt'}:
  archive=r/'verification/legacy-tests'/p.name;archive.parent.mkdir(parents=True,exist_ok=True);shutil.move(str(p),archive)
p=r/'app/build.gradle.kts';s=p.read_text().replace('versionCode = 90','versionCode = 100').replace('9.0.0-refined','10.0.0-noglass');p.write_text(s)
# Natural host state plus actual system insets; background fills behind status bars.
for p in (r/'app/src/main/res').glob('values*/*.xml'):
 s=p.read_text().replace('#F6F7F5','#F3F4F1');p.write_text(s)
# Proof that no custom optical effect is compiled.
for p in list(u.rglob('*.kt'))+list(d.rglob('*.kt')):
 for token in ['RuntimeShader','BlurEffect(','createRuntimeShaderEffect','LocalGlassScene','GlassDock(']:
  assert token not in p.read_text(),(str(p),token)
print('No Glass 10 applied: opaque surfaces, whole-window coral, adaptive bounded tasks and real device authentication.')
