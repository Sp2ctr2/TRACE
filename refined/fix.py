from pathlib import Path
r=Path('android-native')
d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
u=r/'app/src/main/kotlin/app/saeon/trace/ui'
p=d/'Components.kt'
s=p.read_text().replace('GlassAction(text,modifier,enabled,onClick)','GlassAction(text,modifier,enabled,onClick=onClick)')
p.write_text(s)
p=d/'GlassKit.kt'
s=p.read_text().replace('Color(0xFFAF786B).copy(alpha=.88f)','Color(0xFFC49180).copy(alpha=.90f)')
p.write_text(s)
p=d/'Icons.kt'
s=p.read_text().replace('    val Back =','    val Refresh = stroke("새로고침", "M20 7V3M20 7H16M20 7A9 9 0 1 0 21 14")\n    val Back =')
p.write_text(s)
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
print('Corrected compact registered account composition, contrast and full secondary-screen text/target audits.')
