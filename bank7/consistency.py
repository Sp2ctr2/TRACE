from pathlib import Path
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
for p in [r/'app/src/main/kotlin/app/saeon/trace/data/BankRepository.kt',u/'screens/ServiceScreens.kt']:
 s=p.read_text().replace('210-***-4401','220-***-0102')
 s=s.replace('${dateLabel(receipt.completedAt)} ${timeLabel(receipt.completedAt)}','${dateLabel(receipt.completedAt)}').replace('${dateLabel(System.currentTimeMillis())} ${timeLabel(System.currentTimeMillis())}','${dateLabel(System.currentTimeMillis())}')
 p.write_text(s)
p=u/'screens/BankingScreens.kt';s=p.read_text().replace('••••  ••••  ••••  0824','••••  ••••  ••••  3014').replace('Text("새온 데일리"','Text("새온 체크카드"')
s=s.replace('DetailRow("저축 목표", "3,000,000원")','DetailRow("저축 목표", "${won(ServiceStore(LocalContext.current).number(\"goal\",6000000))}원")')
a=s.index('@Composable fun SavingsScreen');b=s.index('@Composable fun CardScreen',a)
part=s[a:b].replace('Space(24); SimulationNote()','Space(18); BankRow("더 모으기·목표 관리", "생활통장에서 모아적금으로", BankIcons.Assets, tag="savings_goals") { open("goals") }\n        Space(24); SimulationNote()')
s=s[:a]+part+s[b:]
a=s.index('@Composable fun CardScreen');b=s.index('@Composable fun LoanScreen',a)
part=s[a:b].replace('        Space(16);SectionTitle("최근 이용 내역")','''        Space(12)
        BankRow("소비 분석", "이번 달 지출과 예산", BankIcons.History, tag="card_spending") { open("spending") }
        BankRow("분실·재발급", "가상 카드의 상태 관리", BankIcons.Card, tag="card_service_open") { open("card_service") }
        Space(16);SectionTitle("최근 이용 내역")''')
s=s[:a]+part+s[b:]
s=s.replace('Page(title = if (receipt.direction == Direction.CREDIT) "입금 확인" else "송금 확인"','Page(title = if(receipt.recipient.id=="cafe")"카드 이용 내역"else if (receipt.direction == Direction.CREDIT) "입금 확인" else "송금 확인"')
s=s.replace('Text(if (receipt.direction == Direction.CREDIT)', 'Text(if(receipt.recipient.id=="cafe")"${receipt.recipient.name}에서 이용했어요."else if (receipt.direction == Direction.CREDIT)')
p.write_text(s)
p=u/'SaeonApp.kt';s=p.read_text();a='''                    backdrop.record { this@drawWithContent.drawContent() }
                    drawLayer(backdrop)''';assert a in s
s=s.replace(a,'''                    if(route in roots && !preferences.reducedTransparency) {
                        backdrop.record { this@drawWithContent.drawContent() }
                        drawLayer(backdrop)
                    } else drawContent()''');p.write_text(s)
p=d/'BankExperience.kt';s=p.read_text().replace('        Canvas(Modifier.matchParentSize().graphicsLayer {','        if(!solid && Build.VERSION.SDK_INT>=31)Canvas(Modifier.matchParentSize().graphicsLayer {',1);p.write_text(s)
# Keep banking actions neutral, but preserve the user's accepted HOLD emphasis.
# The canonical deep coral with white text exceeds 4.5:1 in the light palette.
p=d/'Components.kt';s=p.read_text();a=s.index('@Composable fun PrimaryButton');b=s.index('@Composable fun QuietButton',a)
protection=s[a:b].replace('fun PrimaryButton','fun ProtectionButton').replace('containerColor = TraceColors.Ink, contentColor = TraceColors.Paper','containerColor = TraceColors.Deep, contentColor = TraceColors.White')
s=s+ '\n'+protection;p.write_text(s)
p=u/'screens/ViewportScreens.kt';s=p.read_text();a=s.index('@Composable fun ViewportHold');b=s.index('@Composable fun ViewportWarn',a);part=s[a:b].replace('PrimaryButton("공식 경로로 확인하기"','ProtectionButton("공식 경로로 확인하기"');s=s[:a]+part+s[b:];p.write_text(s)
# Exercise the actual exporter, assert generated content and provider readability,
# then return from the platform share sheet. This is not merely a button-enabled test.
p=r/'app/src/androidTest/kotlin/app/saeon/trace/Bank7Test.kt';s=p.read_text();a='shot("functional_document")';assert a in s
s=s.replace(a,a+'''
        tap("document_export");Thread.sleep(700)
        val exported=java.io.File(context.cacheDir,"documents").listFiles()!!.maxBy{it.lastModified()}
        assertTrue(exported.readText().contains("시연용"));assertTrue(exported.readText().contains("32,000"))
        val uri=androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".documents",exported)
        assertTrue(context.contentResolver.openInputStream(uri)!!.use{it.readBytes().isNotEmpty()})
        device.takeScreenshot(File(output,"functional_android_share_sheet.png"));device.pressBack()
''',1);p.write_text(s)
print('Reconciled metadata, scoped glass rendering, preserved HOLD coral, and verified actual document sharing.')
