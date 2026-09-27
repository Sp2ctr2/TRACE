from pathlib import Path
root=Path('android-native')
p=root/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design/Components.kt'
s=p.read_text()
s+='''
@Composable fun QuietButton(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,onClick:()->Unit){
 TextButton(onClick=onClick,modifier=modifier.heightIn(min=48.dp),enabled=enabled,colors=ButtonDefaults.textButtonColors(contentColor=TraceColors.Ink)){
  Text(text,style=MaterialTheme.typography.labelMedium,textAlign=TextAlign.Center)
 }
}
'''
p.write_text(s)
p=root/'app/src/main/kotlin/app/saeon/trace/ui/screens/GlassHome.kt'
s=p.read_text().replace('Space(if(compact)8 else 10)','Space(if(compact)0 else 8)').replace('Space(if(compact)10 else 16)','Space(if(compact)0 else 14)').replace('Space(if(compact)8 else 14)','Space(if(compact)4 else 10)')
s=s.replace(',rowHeight){',',rowHeight,preferences.hideBalance){')
s=s.replace('height:Int,onClick:()->Unit','height:Int,hide:Boolean,onClick:()->Unit')
s=s.replace('Text("${won(amount)}원",style=','Text(if(hide)"숨김"else"${won(amount)}원",style=')
p.write_text(s)
