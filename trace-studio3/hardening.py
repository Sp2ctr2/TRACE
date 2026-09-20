from pathlib import Path
import sys
root = Path(sys.argv[1] if len(sys.argv) > 1 else 'android-native')
p = root / 'trace-design/src/main/kotlin/dev/trace/scene/TraceScene.kt'
s = p.read_text()
s = s.replace('val state:String="content",val canSubmit:Boolean=true,val blockStates:', 'val state:String="content",val canSubmit:Boolean=true,val hideBalance:Boolean=false,val blockStates:')
s = s.replace('Money(v.balance+if(p.optString("mode")=="total")2_400_000 else 0);', 'if(v.hideBalance)Text("잔액 숨김") else Money(v.balance+if(p.optString("mode")=="total")2_400_000 else 0);')
s = s.replace('Item(it.title,it.subtitle,it.value,it.action)', 'Item(it.title,it.subtitle,if(v.hideBalance)"금액 숨김" else it.value,it.action)')
s = s.replace('background(d.color("surface","#FFFFFF"),RoundedCornerShape(d.token("radius",14,0,32).dp))', 'background(if(type in SceneDocument.mandatory[screen].orEmpty())d.color("background","#FBFAF7") else d.color("surface","#FFFFFF"),RoundedCornerShape(d.token("radius",14,0,32).dp))')
# LocalTextStyle has an explicit ink color. It must not override the button's foreground.
s = s.replace('fontSize=if(senior)20.sp else 16.sp,textAlign=TextAlign.Center)', 'fontSize=if(senior)20.sp else 16.sp,textAlign=TextAlign.Center,color=if(document.tokens.optString("buttonStyle")=="outline")primary else Color.White)')
s = s.replace('Text("송금하기",fontSize=20.sp)', 'Text("송금하기",fontSize=20.sp,color=Color.White)')
s = s.replace('Text(text.ifBlank{"확인"})', 'Text(text.ifBlank{"확인"},color=Color.White)')
s = s.replace('import androidx.compose.ui.text.font.FontFamily', 'import androidx.compose.ui.text.buildAnnotatedString\nimport androidx.compose.ui.text.SpanStyle\nimport androidx.compose.ui.text.withStyle\nimport androidx.compose.ui.text.font.FontFamily')
s = s.replace('Text(money(a),fontSize=size.sp,lineHeight=(size*1.3).sp,fontWeight=FontWeight.Bold,maxLines=1)', 'Text(buildAnnotatedString { withStyle(SpanStyle(fontSize=size.sp,fontWeight=FontWeight.Bold)){append(String.format(Locale.KOREA,"%,d",a))};withStyle(SpanStyle(fontSize=(size*.5f).sp,fontWeight=FontWeight.Medium)){append("원")} },fontSize=size.sp,lineHeight=(size*1.3).sp,maxLines=1)')
s = s.replace('TextButton(onClick={action(dest)},Modifier.heightIn(min=52.dp)){Text(label,color=if(dest=="recipient")primary else ink)}', 'TextButton(onClick={action(dest)},Modifier.weight(if(dest=="recipient")1.3f else 1f).heightIn(min=52.dp),shape=RoundedCornerShape(d.token("buttonRadius",12,0,32).dp),contentPadding=PaddingValues(horizontal=4.dp,vertical=12.dp),colors=ButtonDefaults.textButtonColors(containerColor=if(dest=="recipient")primary else Color.Transparent)){Text(label,color=if(dest=="recipient")Color.White else ink)}')
p.write_text(s)
p = root / 'app/src/main/kotlin/app/saeon/trace/ui/screens/StudioIntegration.kt'
s = p.read_text().replace('val data=SceneData(amount=amount,balance=state.balance,loan=state.loanBalance,', 'val data=SceneData(amount=amount,balance=state.balance,loan=state.loanBalance,hideBalance=prefs.hideBalance,')
p.write_text(s)
p = root / 'app/src/androidTest/kotlin/app/saeon/trace/SceneDeviceTest.kt'
s = p.read_text(); pos=s.rfind('}')
s=s[:pos]+'''    @Test fun importedPrimaryLabelRetainsWhiteForeground() {
        evaluated(DemoScenario.IMPERSONATION)
        runBlocking { BankDesignStore.get(context).apply(raw()) }
        waitScreen("studio_native_hold")
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNode(
            hasText("안전하게 확인하기") and hasAnyAncestor(hasTestTag("scene_primary")),
            useUnmergedTree = true
        ).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        Assert.assertTrue(layouts.isNotEmpty())
        Assert.assertEquals(androidx.compose.ui.graphics.Color.White, layouts.first().layoutInput.style.color)
        capture("34_Studio_Hold_Design")
    }
'''+s[pos:]
p.write_text(s)
print('Imported designs preserve balance privacy, button contrast and amount hierarchy')
