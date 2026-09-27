from pathlib import Path
import shutil,re

root=Path("android-native")
ui=root/"app/src/main/kotlin/app/saeon/trace/ui"
screens=ui/"screens"
design=root/"trace-ui/src/main/kotlin/app/saeon/trace/ui/design"

for src,dst in {
    "SurfaceKit.kt": design/"SurfaceKit.kt",
    "AdaptiveHome.kt": screens/"AdaptiveHome.kt",
    "SolidLaunch.kt": design/"StudioLaunch.kt",
    "AdaptiveTimeline.kt": screens/"AdaptiveTimeline.kt",
    "AdaptiveApp.kt": ui/"SaeonApp.kt",
    "AdaptiveTest.kt": root/"app/src/androidTest/kotlin/app/saeon/trace/AdaptiveTest.kt",
}.items():
    shutil.copy2(Path("solid")/src,dst)

def edit(path,old,new):
    p=root/path
    s=p.read_text()
    if old not in s:
        raise RuntimeError(f"Missing anchor: {path}: {old[:100]}")
    p.write_text(s.replace(old,new))

# Remove the experimental glass renderer entirely from the delivered source.
for stale in [design/"GlassKit.kt"]:
    if stale.exists(): stale.unlink()

p=design/"BankExperience.kt"
s=p.read_text()
start=s.find("/** Android rendition of a floating glass navigation layer")
end=s.find("@Composable fun ResultSeal",start)
if start>=0 and end>start:
    s=s[:start]+s[end:]
p.write_text(s)

# Product palette: porcelain/stone instead of blank white; graphite dark, original TRACE coral.
p=design/"Theme.kt"
s=p.read_text()
s=s.replace(
    'TracePalette(Color(0xFFF6F7F5), Color(0xFFFFFFFF), Color(0xFF20211F),\n        Color(0xFF6B6E73), Color(0xFFE2E3E5)',
    'TracePalette(Color(0xFFF2F3F0), Color(0xFFFAFBF9), Color(0xFF20211F),\n        Color(0xFF666B69), Color(0xFFDDE0DB)'
)
s=s.replace(
    'TracePalette(Color(0xFF151517), Color(0xFF212124), Color(0xFFF3F3F5),\n        Color(0xFFACADB5), Color(0xFF37373D)',
    'TracePalette(Color(0xFF141417), Color(0xFF202024), Color(0xFFF4F4F6),\n        Color(0xFFB0B0B7), Color(0xFF35353B)'
)
p.write_text(s)

# Common pages use window-based maximum widths and gutters.
p=design/"Components.kt"
s=p.read_text()
s=s.replace(
    'BoxWithConstraints(Modifier.fillMaxSize().testTag(tag), contentAlignment = Alignment.TopCenter) {\n        // In landscape',
    'BoxWithConstraints(Modifier.fillMaxSize().testTag(tag), contentAlignment = Alignment.TopCenter) {\n        val adaptive = LocalAdaptiveSpec.current\n        // In landscape'
)
s=s.replace('Column(Modifier.widthIn(max = 600.dp).fillMaxSize()', 'Column(Modifier.widthIn(max = adaptive.contentMax).fillMaxSize()')
s=s.replace('padding(horizontal = if (back != null) 8.dp else 20.dp)', 'padding(horizontal = if (back != null) 8.dp else adaptive.horizontalPadding)')
s=s.replace('padding(horizontal = 20.dp)\n                .padding(top = if (title.isEmpty()) 20.dp else 12.dp', 'padding(horizontal = adaptive.horizontalPadding)\n                .padding(top = if (title.isEmpty()) 20.dp else 12.dp')
s=s.replace('background(TraceColors.Paper).padding(horizontal = 20.dp)', 'background(TraceColors.Paper).padding(horizontal = adaptive.horizontalPadding)')
s=s.replace(
    '@Composable fun PrimaryButton(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,onClick:()->Unit) { GlassAction(text,modifier,enabled,onClick=onClick) }',
    '@Composable fun PrimaryButton(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,onClick:()->Unit) { GradientAction(text,modifier,enabled,ActionTone.CORAL,onClick) }'
)
p.write_text(s)

# Short tasks stay focused even on tablets; footer/dock spacing adapts to rail vs bottom navigation.
p=design/"TaskPage.kt"
s=p.read_text()
s=s.replace('    val scale = LocalDensity.current.fontScale\n    val scroll', '    val scale = LocalDensity.current.fontScale\n    val adaptive = LocalAdaptiveSpec.current\n    val scroll')
s=s.replace('Column(Modifier.widthIn(max=560.dp).fillMaxSize()', 'Column(Modifier.widthIn(max=if(adaptive.twoPane)680.dp else 560.dp).fillMaxSize()')
s=s.replace('.padding(bottom=if(root)72.dp else 0.dp)', '.padding(bottom=if(root && !adaptive.useRail)64.dp else 0.dp)')
s=s.replace('padding(horizontal=if(back!=null)6.dp else 20.dp)', 'padding(horizontal=if(back!=null)6.dp else adaptive.horizontalPadding.coerceAtMost(24.dp))')
s=s.replace('.testTag("${tag}_body").padding(horizontal=20.dp)', '.testTag("${tag}_body").padding(horizontal=adaptive.horizontalPadding.coerceAtMost(24.dp))')
s=s.replace('background(TraceColors.Paper).padding(horizontal=20.dp)', 'background(TraceColors.Paper).padding(horizontal=adaptive.horizontalPadding.coerceAtMost(24.dp))')
p.write_text(s)

# Use real Android system authentication in release: strong biometric OR device credential.
p=root/"app/src/main/kotlin/app/saeon/trace/MainActivity.kt"
s=p.read_text()
s=s.replace(
    'val allowed = BiometricManager.Authenticators.BIOMETRIC_STRONG',
    'val allowed = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL'
)
s=s.replace('.setAllowedAuthenticators(allowed).setNegativeButtonText("취소").build())', '.setAllowedAuthenticators(allowed).build())')
s=s.replace('이 기기에서는 생체 인증을 사용할 수 없어요. 다른 인증 방법을 선택하세요.', '기기 잠금 또는 생체 인증을 사용할 수 없습니다.')
s=s.replace('생체 인증을 마치지 못했어요. 다시 시도하거나 다른 인증 방법을 선택하세요.', '기기 인증을 완료하지 못했습니다. 다시 시도해 주세요.')
p.write_text(s)

# Splash and app icon retain the exact TRACE path identity, with the new canvas tone.
res=root/"app/src/main/res"
for path in list(res.glob("values*/trace_launcher.xml")):
    text=path.read_text().replace("#F6F7F5","#F2F3F0").replace("#F5F4F0","#F2F3F0")
    path.write_text(text)
for path in list(res.glob("values*/refined_colors.xml")):
    text=path.read_text().replace("#F6F7F5","#F2F3F0")
    path.write_text(text)

# Version marks the adaptive solid-material pass.
p=root/"app/build.gradle.kts"
s=p.read_text().replace('versionCode = 90','versionCode = 100').replace('9.0.0-refined','10.0.0-adaptive')
p.write_text(s)

print("Applied solid adaptive design, device auth, tablet/landscape shell and responsive page geometry.")
