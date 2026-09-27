from pathlib import Path
r=Path('android-native');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'trace-ui/src/main/kotlin/app/saeon/trace/ui/design'
p=u/'SaeonApp.kt';s=p.read_text()
s=s.replace('    val navBackdrop=rememberGraphicsLayer()\n','')
s=s.replace('BankAmbient(Modifier.matchParentSize().drawWithContent { navBackdrop.record { this@drawWithContent.drawContent() };drawLayer(navBackdrop) })','BankAmbient(Modifier.matchParentSize())')
s=s.replace('navBackdrop,Offset.Zero,Modifier.align','Modifier.align')
p.write_text(s)
p=d/'LiquidNavigation.kt';s=p.read_text()
s=s.replace('import androidx.compose.ui.graphics.layer.*\n','')
s=s.replace('    source:GraphicsLayer,sourceOrigin:Offset,modifier:Modifier=Modifier,','    modifier:Modifier=Modifier,')
s=s.replace('    var origin by remember{mutableStateOf(Offset.Zero)}','    var origin by remember{mutableStateOf(Offset.Zero)}\n    var rootExtent by remember{mutableStateOf(IntSize.Zero)}')
s=s.replace('    val divider=TraceColors.Divider','    val paper=TraceColors.Paper\n    val coral=TraceColors.Coral\n    val divider=TraceColors.Divider')
s=s.replace('if(p!=origin)origin=p}', 'if(p!=origin)origin=p;val e=it.findRootCoordinates().size;if(e!=rootExtent)rootExtent=e}')
a=s.index('        if(!reducedTransparency)Canvas(Modifier.matchParentSize().graphicsLayer{')
b=s.index('        Box(Modifier.matchParentSize().background(',a)
s=s[:a]+s[b:]
a=s.index('                if(!reducedTransparency)Canvas(Modifier.matchParentSize()){')
b=s.index('                Box(Modifier.matchParentSize().background(',a)
s=s[:a]+'''                if(!reducedTransparency)Canvas(Modifier.matchParentSize()){
                    // All content reserves this band. Re-evaluate its known background at
                    // magnified coordinates: no screenshot texture or text duplication is needed.
                    val lensOrigin=origin+Offset(x,y)
                    scale(1.045f,1.045f){navigationScene(rootExtent,lensOrigin,paper,coral,dark)}
                }
'''+s[b:]
s=s.replace('else Color.White.copy(alpha=.68f)','else Color.White.copy(alpha=.48f)')
s=s.replace('/** A sampled navigation material. No bank content or action buttons use this effect.\n * The reserved navigation background is recorded independently; the banking content stays uncached.\n * This is an Android interpretation of a liquid material, not an Apple framework.\n */','''/** A liquid-like selection material for a reserved navigation band.
 * Its known background is evaluated at refracted coordinates rather than captured into
 * a screen-sized GPU buffer. Text and icons remain above the optical layers.
 * Arbitrary moving content behind this reserved band is intentionally unsupported.
 * This is not an Apple framework implementation.
 */''')
s+='''
private fun DrawScope.navigationScene(extent:IntSize,origin:Offset,paper:Color,coral:Color,dark:Boolean){
    if(extent.width<=0||extent.height<=0)return
    drawRect(paper)
    val width=extent.width.toFloat();val height=extent.height.toFloat()
    val radius=(width*1.12f).coerceIn(430.dp.toPx(),1000.dp.toPx())
    drawRect(Brush.radialGradient(
        0f to coral.copy(alpha=if(dark).15f else .29f),
        .24f to coral.copy(alpha=if(dark).095f else .18f),
        .55f to coral.copy(alpha=if(dark).025f else .055f),
        .88f to Color.Transparent,
        center=Offset(width*1.04f,-height*.025f)-origin,radius=radius))
    if(!dark)drawRect(Brush.radialGradient(listOf(Color(0xFFFFB596).copy(alpha=.12f),Color.Transparent),Offset(width*.78f,0f)-origin,radius*.52f))
}
'''
p.write_text(s)
print('Bounded vector optical material: transparent dock, magnified analytic scene, reflective moving lens; no full-screen render texture or blur dependency.')
