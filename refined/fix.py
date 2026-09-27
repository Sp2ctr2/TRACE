from pathlib import Path
r=Path('android-native')
p=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design/Components.kt'
s=p.read_text().replace('GlassAction(text,modifier,enabled,onClick)','GlassAction(text,modifier,enabled,onClick=onClick)')
p.write_text(s)
