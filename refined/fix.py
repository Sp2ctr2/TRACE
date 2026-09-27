from pathlib import Path
r=Path('android-native')
d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
u=r/'app/src/main/kotlin/app/saeon/trace/ui'
p=d/'Components.kt'
s=p.read_text().replace('GlassAction(text,modifier,enabled,onClick)','GlassAction(text,modifier,enabled,onClick=onClick)')
p.write_text(s)
p=d/'GlassKit.kt'
p.write_text(p.read_text().replace('Color(0xFFAF786B).copy(alpha=.88f)','Color(0xFFC49180).copy(alpha=.90f)'))
p=d/'Icons.kt'
p.write_text(p.read_text().replace('    val Back =','    val Refresh = stroke("새로고침", "M20 7V3M20 7H16M20 7A9 9 0 1 0 21 14")\n    val Back ='))
p=u/'screens/RefinedScreens.kt'
s=p.read_text()
s=s.replace('actions={AppIcon(BankIcons.Trace,size=24,tint=TraceColors.Coral)},footer={','actions={IconAction(BankIcons.Refresh,"상환 계좌 새로고침"){if(!interaction.busy)model.resolveRoute(record.intent.id)}},footer={')
s=s.replace('Text(if(loading)"상환 계좌를\\n확인하고 있어요"else"상환 계좌를\\n다시 확인했어요",','Text(if(compact){if(loading)"상환 계좌 확인 중"else"상환 계좌 확인"}else if(loading)"상환 계좌를\\n확인하고 있어요"else"상환 계좌를\\n다시 확인했어요",')
s=s.replace('Body("대출 상환은 은행에 등록된 계좌로 보내야 합니다.",subdued=true)','Body(if(compact)"은행 등록 계좌로 상환합니다."else"대출 상환은 은행에 등록된 계좌로 보내야 합니다.",subdued=true)')
s=s.replace('if(!loading)QuietButton("계좌 정보 새로고침"','if(!loading&&!compact)QuietButton("계좌 정보 새로고침"')
p.write_text(s)
p=r/'app/src/androidTest/kotlin/app/saeon/trace/RefinedTest.kt'
s=p.read_text().replace('capture(name,audit=false)','capture(name,audit=true)').replace('capture("fit_$tag",audit=false)','capture("fit_$tag",audit=true)')
p.write_text(s)
p=r/'app/src/androidTest/kotlin/app/saeon/trace/UiHarness.kt'
s=p.read_text().replace('    fun fresh(scenario: DemoScenario = DemoScenario.NORMAL) {','    fun fresh(scenario: DemoScenario = DemoScenario.NORMAL) {\n        device.executeShellCommand("am broadcast -a com.android.systemui.demo -e command exit")\n        device.executeShellCommand("settings put global sysui_demo_on 0")')
p.write_text(s)
# Ordinary screens use service language. About, terms, demo center, authentication,
# copied account text and financial records retain their simulation disclosures.
p=u/'screens/BankingScreens.kt'
s=p.read_text().replace('Space(12); SimulationNote()','').replace('Space(24); SimulationNote()','')
copy={
 '가상 계좌 정보 복사':'계좌 정보 복사',
 '마스킹된 시연 계좌 정보를 복사했어요.':'계좌 정보를 복사했습니다.',
 '가상 자유저축':'자유저축',
 '이 시연 계좌에서는 생활통장으로 돈을 가져올 수 있어요. 두 계좌의 합계는 바뀌지 않습니다.':'생활통장으로 가져올 수 있습니다.',
 '이 기기에 저장된 시연 카드의 상태를 변경해요.':'카드 사용을 잠시 중지합니다.',
 '등록된 대출의 가상 상환을 마쳤어요.':'대출 상환이 완료됐습니다.',
 '상환은 은행에 등록된 경로에서만 진행하세요. 개인에게 안내받은 계좌와는 다를 수 있어요.':'등록된 상환계좌로 상환할 수 있습니다.',
 '공식 경로 확인 후 새 거래 내역과 인증을 다시 확인합니다.':'상환 금액을 확인한 뒤 인증해 주세요.',
 '시연 데이터가 초기화되었을 수 있어요. 송금은 새로 실행하지 않았습니다. 전체 내역에서 확인해 주세요.':'거래 내역에서 다시 확인해 주세요. 송금은 실행하지 않았습니다.'}
for a,b in copy.items():s=s.replace(a,b)
s=s.replace('        Caption("가상 카드입니다. 실제 승인망이나 카드 정지 요청은 연결되지 않습니다.")','')
p.write_text(s)
p=u/'screens/SafetyScreens.kt';s=p.read_text()
s=s.replace('평소에는 조용하게, 위험할 땐 분명하게.','송금 전 요청과 거래를 함께 확인합니다.').replace('아직 돈은 나가지 않았습니다. 이유와 확인할 경로를 정리해 두었어요.','보류된 송금의 맥락을 확인하세요.').replace('메시지를 붙여 넣거나 말로 입력해요.','메시지 또는 음성으로 입력').replace('"데이터 경계", "원문 대신 필요한 위험 신호만 남깁니다."','"개인정보 처리", "처리하는 정보와 보관 기간"').replace('상대가 준 경로와 확인할 경로는 달라야 해요.','공식 경로로 확인하는 방법').replace('"쉬운 모드", "큰 글자와 하나씩 확인하는 안내"','"쉬운 사용", "화면과 안내를 간단하게"')
s=s.replace('        Space(24); Caption("TRACE는 경찰이나 은행 상담원을 대신하지 않습니다. 이 앱은 로컬 규칙과 가상 정책으로 보호 흐름을 시연합니다.")','')
p.write_text(s)
p=u/'screens/TransferScreens.kt';s=p.read_text().replace('새로운 가상 계좌를 확인해요.','은행과 계좌번호로 찾기').replace('Space(20); Caption("은행과 수취인은 모두 시연용 가상 데이터입니다.")','')
s=s.replace('시연에서는 가상 계좌의 끝 네 자리만 입력합니다.','저장된 계좌의 끝 4자리로 찾습니다.').replace('가상 계좌 끝 4자리','계좌 끝 4자리')
p.write_text(s)
p=u/'screens/StudioServicesScreens.kt';s=p.read_text()
s=s.replace('Space(20);Body("투자 정보는 고정된 시연 데이터입니다.",subdued=true);Space(20)','Space(20)')
s=s.replace('Space(12);Caption("계약·보장·납입 금액은 가상 정보입니다.")','')
s=s.replace('금융 생활을\\n차근차근 확인해요','내 금융 현황').replace('Caption("외부 신용평가사와 연결하지 않은 시연 요약입니다.")','Caption("대출과 카드 이용 현황")').replace('"연체 내역","시연 내역 없음"','"연체 내역","연체 없음"')
s=s.replace('"처리 상태","시연 접수 완료"','"처리 상태","요청 기록됨"')
p.write_text(s)
print('Final secondary-screen language, compact route, dark contrast, all-screen audits and natural status bar capture applied.')
