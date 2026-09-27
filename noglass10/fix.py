from pathlib import Path
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui'
p=u/'screens/RefinedScreens.kt';s=p.read_text()
a=s.index('        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){')
b=s.index('        if(!loading&&!valid)',a)
block=s[a:b]
s=s[:a]+'        if(!LocalBankLandscape.current){ RegisteredDestination(record,loading,compact) }\n'+s[b:]
s=s.replace('            PrimaryButton(if(loading)"상환 계좌 확인 중"','            if(LocalBankLandscape.current){ RegisteredDestination(record,loading,true);Space(8) }\n            PrimaryButton(if(loading)"상환 계좌 확인 중"',1)
s+='\n@Composable private fun RegisteredDestination(record:TransferRecord,loading:Boolean,compact:Boolean){\n    val route=record.route\n'+block+'}\n'
p.write_text(s)
p=u/'screens/StudioScreens.kt';s=p.read_text()
a=s.index('        Column(Modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(20.dp)).padding(horizontal=16.dp,vertical=8.dp)) {')
b=s.index('        QuietButton("금액·목적 수정"',a)
block=s[a:b]
s=s[:a]+'        if(!LocalBankLandscape.current) LandscapeReviewDetails(tx,compact)\n'+s[b:]
anchor='    TaskPage("송금 확인","transfer_review",back=back,footer={'
s=s.replace(anchor,anchor+'\n        if(LocalBankLandscape.current){ LandscapeReviewDetails(tx,true);Space(8) }',1)
s+='\n@Composable private fun LandscapeReviewDetails(tx:TransactionIntent,compact:Boolean){\n'+block+'}\n'
p.write_text(s)
print('Landscape confirmations keep the actual destination and transaction data beside the action.')
