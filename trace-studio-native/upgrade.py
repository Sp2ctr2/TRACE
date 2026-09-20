"""Reproducible Trace branding/senior migration; never changes transaction policy."""
from pathlib import Path
import re,sys
root=Path(sys.argv[1]) if len(sys.argv)>1 else Path('android-native')
for base in ['app/src','core/src','tools']:
 for p in (root/base).rglob('*'):
  if p.is_file() and p.suffix in {'.kt','.xml','.py','.sh','.json','.txt','.md'}:
   s=p.read_text().replace('새온은행','Trace').replace('새온 ','Trace ').replace('새온스튜디오','트레이스스튜디오')
   s=s.replace('큰 글씨·어린이 화면','노약자 모드').replace('큰 글씨·어린이','노약자')
   p.write_text(s)
p=root/'app/src/main/kotlin/app/saeon/trace/data/Preferences.kt';s=p.read_text()
s=s.replace('STANDARD("기본"), LARGE("큰 글씨"), CHILD("어린이")','STANDARD("기본"), LARGE("노약자 모드")')
s=s.replace(',\n    val childMode: Boolean = false','')
s=s.replace('when { childMode -> ReadingMode.CHILD; easyMode -> ReadingMode.LARGE; else -> ReadingMode.STANDARD }','if (easyMode) ReadingMode.LARGE else ReadingMode.STANDARD')
s=s.replace('private val child = booleanPreferencesKey("child_mode")','// Migrate legacy preference without changing ledger data.\n    private val legacyChild = booleanPreferencesKey("child_mode")')
s=s.replace('it[child]','it[legacyChild]').replace(', it[legacyChild] ?: false)',')')
s=s.replace('it[legacyChild] = value == ReadingMode.CHILD','it.remove(legacyChild)');p.write_text(s)
p=root/'app/src/main/kotlin/app/saeon/trace/ui/screens/QuietBankScreens.kt';s=p.read_text()
s=s.replace('if (preferences.childMode) "돈 보내기" else "송금하기"','"송금하기"')
s=s.replace('if (preferences.childMode) "모르는 사람이 돈을 보내 달라고 했나요?\\n먼저 보호자와 확인해요." else ', '')
s=s.replace('if (preferences.childMode) Caption("대신 보내 달라는 부탁은 보호자와 먼저 확인해요.")','if (preferences.easyMode) Caption("상대가 재촉한다면 먼저 전화를 끊고 확인하세요.")')
s=s.replace('if (preferences.childMode) "이 금액을 보낼까요?" else "보낼까요?"','"보낼까요?"');p.write_text(s)
p=root/'app/src/main/kotlin/app/saeon/trace/ui/screens/QuietProtectionScreens.kt';s=p.read_text()
s=s.replace('preferences.childMode -> "믿을 수 있는 어른과 확인"; ', '')
s=s.replace('preferences.childMode -> "지금은 돈을\\n보내지 마세요."; ', '')
s=s.replace('if (preferences.childMode) "아직 돈은 나가지 않았어요." else ', '')
s=s.replace('if (preferences.childMode) "기관 직원인 척하며 돈을 요구했을 수 있어요. 보호자나 선생님에게 보여 주세요."\n                else ', '')
a=s.index('@Composable fun QuietGuide(')
s=s[:a]+'''@Composable fun QuietGuide(state: BankState, preferences: BankPreferences, model: BankViewModel, open: (String) -> Unit, back: () -> Unit) {
    val record = state.current?.takeIf { it.stage in setOf(TransferStage.HOLD, TransferStage.WARN, TransferStage.VERIFY, TransferStage.UNKNOWN, TransferStage.ROUTE) }
    var step by rememberSaveable(record?.intent?.id) { mutableIntStateOf(0) }
    val titles = listOf("상대가 준 번호나\\n링크는 쓰지 마세요.", "은행 앱을\\n직접 열어 주세요.", "확인하기 전에는\\n보내지 마세요.")
    val descriptions = listOf("통화 중이라면 먼저 전화를 끊어도 됩니다. 상대가 보내 준 연락처로 확인하지 마세요.",
        "평소 쓰던 은행 앱의 고객센터나 대출 상환 메뉴에서 직접 확인하세요.",
        "확인이 안 되면 송금을 취소하세요. 화면을 닫아도 돈은 자동으로 나가지 않습니다.")
    val goBack: () -> Unit = { if (step > 0) step -= 1 else back() }
    BackHandler(onBack = goBack)
    Page(title = "안전하게 확인하기", tag = "trace_safety_guide", back = goBack, footer = {
        PrimaryButton(if (step < 2) "다음" else "은행 앱에서 확인하기", Modifier.testTag("safety_official_channel")) {
            if (step < 2) step++ else open("support")
        }
        if (record != null) SecondaryButton("송금 취소") { model.cancelTransfer(record.intent.id) { open("home") } }
    }) {
        Space(16); Caption("${step + 1} / 3"); Space(22); Headline(titles[step]); Space(24)
        Body(descriptions[step]); Space(28); Rule(); Space(20)
        if (record != null) {
            Text("아직 돈은 나가지 않았습니다.", style = MaterialTheme.typography.titleSmall)
            Space(14); Money(record.intent.amount, hero = false); Space(8); Caption("${record.intent.recipient.name}님에게 보내려던 돈")
        } else Caption("이 화면에서는 송금을 실행하지 않습니다.")
    }
}
''';p.write_text(s)
p=root/'app/src/main/kotlin/app/saeon/trace/ui/screens/DemoCenterScreens.kt';s=p.read_text()
s=re.sub(r'^\s*ReadingMode.CHILD -> .*\n','\n',s,flags=re.M)
s=s.replace('화면 모드가 달라져도 송금 보류는 풀리지 않습니다. 어린이 모드는 안내 방식이며, 보호자의 인증이나 허락을 대신하지 않습니다.', '노약자 모드는 큰 글자와 넓은 버튼, 한 단계씩 진행하는 안내를 제공합니다. 화면 모드를 바꿔도 송금 보류는 풀리지 않습니다.')
s=s.replace('"큰 글씨"', '"노약자 모드"').replace('쉬운 모드로 보류 경험','노약자 모드로 보류 경험');p.write_text(s)
for p in (root/'app/src/androidTest').rglob('*.kt'):
 s=p.read_text().replace('ReadingMode.CHILD','ReadingMode.LARGE').replace('.value.childMode','.value.easyMode')
 s=s.replace('mode_CHILD','mode_LARGE').replace('childModeKeepsHoldAndDoesNotImplyGuardianAuthorization','seniorModeKeepsHoldAndNeverAuthorizesMoney')
 s=s.replace('"지금은 돈을\\n보내지 마세요."','"송금을 잠시\\n멈췄습니다."')
 s=s.replace('"이 앱이 어른에게 연락하거나 허락을 받은 것은 아니에요. 직접 함께 확인해 주세요."','"확인이 안 되면 송금을 취소하세요. 화면을 닫아도 돈은 자동으로 나가지 않습니다."')
 s=s.replace('tap("safety_official_channel"); waitScreen("safety_center")','tap("safety_official_channel"); waitScreen("support")')
 s=s.replace('Child_HOLD','Senior_HOLD').replace('Child_Guide','Senior_Guide');p.write_text(s)
for p in (root/'tools').glob('*.py'):
 p.write_text(p.read_text().replace('Child_HOLD','Senior_HOLD').replace('Child_Guide','Senior_Guide'))
p=root/'app/build.gradle.kts';p.write_text(p.read_text().replace('versionCode = 4','versionCode = 6').replace('versionName = "1.1.0-demo"','versionName = "2.0.0-demo"'))
for p in (root/'app/src/main').rglob('*'):
 if p.is_file() and p.suffix in {'.kt','.xml'}:
  assert '어린이' not in p.read_text(),str(p)
  assert 'childMode' not in p.read_text(),str(p)
print('Trace branding and standard/senior migration applied; all original safety tests retained.')
