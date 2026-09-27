from pathlib import Path
r=Path('android-native');d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
p=d/'Components.kt';s=p.read_text()
if '@Composable fun QuietButton(' not in s:
 s += '''
@Composable fun QuietButton(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,onClick:()->Unit){
 val press=remember{MutableInteractionSource()}
 TextButton(onClick=onClick,interactionSource=press,modifier=modifier.tracePress(press).heightIn(min=48.dp),enabled=enabled,
 shape=RoundedCornerShape(10.dp),colors=ButtonDefaults.textButtonColors(contentColor=TraceColors.Ink),
 contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp)){
  Text(text,style=MaterialTheme.typography.labelMedium,textAlign=TextAlign.Center)
 }
}
'''
 p.write_text(s)
print('Glass primary and neutral secondary actions restored independently.')
