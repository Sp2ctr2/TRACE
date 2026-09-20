from pathlib import Path
import sys
root = Path(sys.argv[1] if len(sys.argv) > 1 else 'android-native')
p = root / 'trace-design/src/main/kotlin/dev/trace/scene/TraceScene.kt'
s = p.read_text()
s = s.replace('val state:String="content",val canSubmit:Boolean=true,val blockStates:', 'val state:String="content",val canSubmit:Boolean=true,val hideBalance:Boolean=false,val blockStates:')
s = s.replace('Money(v.balance+if(p.optString("mode")=="total")2_400_000 else 0);', 'if(v.hideBalance)Text("잔액 숨김") else Money(v.balance+if(p.optString("mode")=="total")2_400_000 else 0);')
s = s.replace('Item(it.title,it.subtitle,it.value,it.action)', 'Item(it.title,it.subtitle,if(v.hideBalance)"금액 숨김" else it.value,it.action)')
s = s.replace('background(d.color("surface","#FFFFFF"),RoundedCornerShape(d.token("radius",14,0,32).dp))', 'background(if(type in SceneDocument.mandatory[screen].orEmpty())d.color("background","#FBFAF7") else d.color("surface","#FFFFFF"),RoundedCornerShape(d.token("radius",14,0,32).dp))')
p.write_text(s)
p = root / 'app/src/main/kotlin/app/saeon/trace/ui/screens/StudioIntegration.kt'
s = p.read_text().replace('val data=SceneData(amount=amount,balance=state.balance,loan=state.loanBalance,', 'val data=SceneData(amount=amount,balance=state.balance,loan=state.loanBalance,hideBalance=prefs.hideBalance,')
p.write_text(s)
print('User visibility preference is preserved by imported designs')
