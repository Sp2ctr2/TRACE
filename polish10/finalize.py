from pathlib import Path
r=Path('android-native')
p=r/'app/src/main/kotlin/app/saeon/trace/ui/screens/NoGlassHome.kt'
s=p.read_text()
old='TextButton(onClick=onClick,contentPadding=PaddingValues(horizontal=4.dp))'
assert old in s
s=s.replace(old,'TextButton(onClick=onClick,modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(horizontal=4.dp))')
p.write_text(s)
p=r/'app/src/androidTest/kotlin/app/saeon/trace/UiHarness.kt'
s=p.read_text()
old='        if (scroll) node.performScrollTo()\n        node.performClick()'
assert old in s
s=s.replace(old,'''        if (scroll) {
            var ancestor=node.fetchSemanticsNode().parent
            while(ancestor!=null && !ancestor.config.contains(SemanticsActions.ScrollBy)) ancestor=ancestor.parent
            if(ancestor!=null) node.performScrollTo()
        }
        node.assertIsDisplayed().performClick()''')
p.write_text(s)
p=r/'app/build.gradle.kts'
s=p.read_text().replace('versionCode = 100','versionCode = 101').replace('10.0.0-noglass','10.0.1-noglass')
p.write_text(s)
print('Home actions retain 48dp targets. Fixed-position test buttons no longer receive an invalid scroll operation.')
