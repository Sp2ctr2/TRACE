from pathlib import Path
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
# The same savings account and card must remain identifiable on all screens.
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
part=s[a:b]
part=part.replace('        Space(16);SectionTitle("최근 이용 내역")','''        Space(12)
        BankRow("소비 분석", "이번 달 지출과 예산", BankIcons.History, tag="card_spending") { open("spending") }
        BankRow("분실·재발급", "가상 카드의 상태 관리", BankIcons.Card, tag="card_service_open") { open("card_service") }
        Space(16);SectionTitle("최근 이용 내역")''')
s=s[:a]+part+s[b:]
# Native card payments are not labelled as person-to-person transfers.
s=s.replace('Page(title = "송금 확인", tag = "receipt"','Page(title = if(receipt.recipient.id=="cafe")"카드 이용 내역"else"송금 확인", tag = "receipt"')
s=s.replace('Text(if (receipt.direction == Direction.CREDIT)', 'Text(if(receipt.recipient.id=="cafe")"${receipt.recipient.name}에서 이용했어요."else if (receipt.direction == Direction.CREDIT)')
p.write_text(s)
# Record a backdrop only while visible glass needs it; detail pages and the
# solid accessibility fallback draw directly rather than allocate another layer.
p=u/'SaeonApp.kt';s=p.read_text();a='''                    backdrop.record { this@drawWithContent.drawContent() }
                    drawLayer(backdrop)''';assert a in s
s=s.replace(a,'''                    if(route in roots && !preferences.reducedTransparency) {
                        backdrop.record { this@drawWithContent.drawContent() }
                        drawLayer(backdrop)
                    } else drawContent()''');p.write_text(s)
p=d/'BankExperience.kt';s=p.read_text().replace('        Canvas(Modifier.matchParentSize().graphicsLayer {','        if(!solid && Build.VERSION.SDK_INT>=31)Canvas(Modifier.matchParentSize().graphicsLayer {',1);p.write_text(s)
print('Reconciled savings/card identities, native service entry points, date formatting and conditional glass work.')
