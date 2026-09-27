package app.saeon.trace.ui.design

import android.os.Build
import android.graphics.RuntimeShader
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

data class GlassScene(val layer:GraphicsLayer,val origin:Offset)
val LocalGlassScene=staticCompositionLocalOf<GlassScene?>{null}

private const val LENS="""
uniform shader scene;
uniform float2 extent;
uniform float corner;
half4 main(float2 p) {
    float2 c=extent*.5;
    float2 v=p-c;
    float2 q=abs(v)-(c-float2(corner));
    float d=length(max(q,float2(0)))+min(max(q.x,q.y),0.0)-corner;
    float edge=1.0-smoothstep(0.0,16.0,-d);
    float2 n=normalize(v/max(c,float2(1))+float2(.0001));
    float2 uv=clamp(p-n*edge*8.0,float2(1),extent-float2(1));
    half4 s=scene.eval(uv);
    return s;
}
"""

/** Real source sampling is separate from the optical surface. Text is never blurred.
 * This is an Android interpretation, not Apple's platform Liquid Glass renderer. */
@Composable fun GlassPlate(modifier:Modifier=Modifier,tinted:Boolean=false,radius:Dp=22.dp,
    scene:GlassScene?=LocalGlassScene.current,pressed:Boolean=false,smoked:Boolean=false,
    contact:Offset=Offset(.24f,.12f),content:@Composable BoxScope.()->Unit) {
    var origin by remember{mutableStateOf(Offset.Zero)}
    val solid=LocalReducedTransparency.current
    val reduced=LocalReducedMotion.current
    val dark=TraceColors.Paper.luminance()<.25f
    val palette=LocalTracePalette.current
    val shader=remember{if(Build.VERSION.SDK_INT>=33)RuntimeShader(LENS)else null}
    val shape=RoundedCornerShape(radius)
    val pressure by animateFloatAsState(if(pressed)1f else 0f,if(reduced)snap()else tween(if(pressed)85 else 260),label="surface-pressure")
    val lightX by animateFloatAsState(if(pressed)contact.x else .22f,if(reduced)snap()else tween(260),label="surface-reflection")
    val scale=if(reduced)1f else 1f-.012f*pressure
    val tones=when {
        solid&&smoked->listOf(Color(0xFF27272B),Color(0xFF27272B))
        solid&&tinted->listOf(if(dark)Color(0xFFC99583)else Color(0xFFF5A18A),if(dark)Color(0xFFC99583)else Color(0xFFF5A18A))
        solid->listOf(palette.Surface,palette.Surface)
        smoked->listOf(Color(0xFF656569).copy(alpha=if(dark).58f else .72f),Color(0xFF232327).copy(alpha=.88f))
        tinted&&dark->listOf(Color(0xFFD1A795).copy(alpha=.83f),Color(0xFFAF786B).copy(alpha=.88f))
        tinted->listOf(Color(0xFFFFC3AC).copy(alpha=.67f),Color(0xFFF17C5D).copy(alpha=.75f))
        dark->listOf(Color(0xFF4A4A51).copy(alpha=.46f),Color(0xFF26262B).copy(alpha=.64f))
        else->listOf(Color.White.copy(alpha=.50f),Color(0xFFF8F9F7).copy(alpha=.27f))
    }
    Box(modifier.onGloballyPositioned{val o=it.positionInRoot();if(o!=origin)origin=o}
        .graphicsLayer{scaleX=scale;scaleY=scale}
        .shadow(if(smoked||tinted)8.dp else 5.dp,shape,ambientColor=Color.Black.copy(alpha=.035f),spotColor=Color.Black.copy(alpha=.08f)).clip(shape)) {
        if(scene!=null&&!solid)Canvas(Modifier.matchParentSize().graphicsLayer{
            if(Build.VERSION.SDK_INT>=33&&shader!=null&&size.width>0&&size.height>0){
                shader.setFloatUniform("extent",size.width,size.height);shader.setFloatUniform("corner",radius.toPx())
                val blur=android.graphics.RenderEffect.createBlurEffect(7.dp.toPx(),7.dp.toPx(),android.graphics.Shader.TileMode.CLAMP)
                renderEffect=android.graphics.RenderEffect.createChainEffect(android.graphics.RenderEffect.createRuntimeShaderEffect(shader,"scene"),blur).asComposeRenderEffect()
            }else if(Build.VERSION.SDK_INT>=31)renderEffect=BlurEffect(9.dp.toPx(),9.dp.toPx(),TileMode.Clamp)
        }){translate(scene.origin.x-origin.x,scene.origin.y-origin.y){drawLayer(scene.layer)}}
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(tones)))
        if(!solid)Canvas(Modifier.matchParentSize()){
            val r=radius.toPx();val edge=androidx.compose.ui.geometry.CornerRadius(r)
            val shine=if(dark&&!tinted&&!smoked).20f else .53f
            drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha=shine+.12f*pressure),Color.Transparent),Offset(size.width*lightX,-size.height*.8f),size.width*.72f))
            drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha=if(dark).48f else .90f),Color.White.copy(alpha=.06f),Color.White.copy(alpha=.50f))),cornerRadius=edge,style=Stroke(1.dp.toPx()))
            inset(2.dp.toPx()){
                drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha=.22f),Color.Transparent,Color.White.copy(alpha=.11f))),cornerRadius=androidx.compose.ui.geometry.CornerRadius((r-2.dp.toPx()).coerceAtLeast(0f)),style=Stroke(.7.dp.toPx()))
            }
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent,Color.White.copy(alpha=.78f),Color.Transparent)),Offset(r*.75f,1.5.dp.toPx()),Offset(size.width-r*.75f,1.5.dp.toPx()),1.1.dp.toPx(),StrokeCap.Round)
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent,Color.White.copy(alpha=.32f),Color.Transparent)),Offset(r,size.height-3.dp.toPx()),Offset(size.width-r,size.height-3.dp.toPx()),1.dp.toPx(),StrokeCap.Round)
            drawLine(Color.Black.copy(alpha=if(smoked).13f else .055f),Offset(r,size.height-.6.dp.toPx()),Offset(size.width-r,size.height-.6.dp.toPx()),.9.dp.toPx())
        }
        content()
    }
}

@Composable fun GlassAction(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,smoked:Boolean=false,onClick:()->Unit){
    val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState()
    var width by remember{mutableIntStateOf(1)};var contact by remember{mutableStateOf(Offset(.24f,.12f))}
    LaunchedEffect(source){source.interactions.collect{if(it is PressInteraction.Press)contact=Offset((it.pressPosition.x/width).coerceIn(0f,1f),.3f)}}
    val reduced=LocalReducedMotion.current
    GlassPlate(modifier.fillMaxWidth().heightIn(min=if(LocalEasyMode.current)64.dp else 52.dp).onSizeChanged{width=it.width.coerceAtLeast(1)}
        .semantics(mergeDescendants=true){if(!enabled)disabled()}
        .clickable(interactionSource=source,indication=null,enabled=enabled,role=Role.Button,onClick=onClick),tinted=enabled&&!smoked,smoked=smoked&&enabled,pressed=pressed,contact=contact,radius=21.dp){
        Text(text,Modifier.align(Alignment.Center).padding(horizontal=12.dp,vertical=12.dp),color=when{!enabled->TraceColors.Muted;smoked->Color(0xFFFAFAFC);else->Color(0xFF341A14)},
            style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold,maxLines=2)
    }
}

/** 32dp visual inside a 48dp hit region. Smaller appearance does not reduce motor accessibility. */
@Composable fun GlassIcon(icon:ImageVector,label:String,modifier:Modifier=Modifier,onClick:()->Unit){
    val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState()
    Box(modifier.size(48.dp).semantics{contentDescription=label}.clickable(interactionSource=source,indication=null,role=Role.Button,onClick=onClick),contentAlignment=Alignment.Center){
        GlassPlate(Modifier.size(if(LocalEasyMode.current)40.dp else 32.dp),radius=12.dp,pressed=pressed){
            Icon(icon,null,Modifier.size(if(LocalEasyMode.current)23.dp else 18.dp).align(Alignment.Center),tint=TraceColors.Ink)
        }
    }
}
@Composable fun GlassAmbient(layer:GraphicsLayer,modifier:Modifier=Modifier){
    val paper=TraceColors.Paper;val coral=TraceColors.Coral;val dark=paper.luminance()<.25f
    Canvas(modifier.drawWithContent{layer.record{this@drawWithContent.drawContent()};drawLayer(layer)}){
        drawRect(paper)
        drawRect(Brush.radialGradient(listOf(coral.copy(alpha=if(dark).035f else .085f),Color.Transparent),Offset(size.width*.97f,-24.dp.toPx()),310.dp.toPx()))
    }
}

@Composable fun CalibratedDock(tabs:List<Triple<String,String,ImageVector>>,route:String,backdrop:GraphicsLayer,contentOrigin:Offset,
    modifier:Modifier=Modifier,reduceTransparency:Boolean=LocalReducedTransparency.current,onSelect:(String)->Unit){
    val bounds=remember{mutableStateMapOf<String,Rect>()};var origin by remember{mutableStateOf(Offset.Zero)}
    val target=bounds[route];val reduce=LocalReducedMotion.current
    val x by animateFloatAsState(target?.left?:4f,if(reduce)snap()else spring(dampingRatio=.94f,stiffness=700f),label="dock-x")
    val y by animateFloatAsState(target?.top?:4f,if(reduce)snap()else tween(240),label="dock-y")
    val dark=TraceColors.Paper.luminance()<.25f;val density=LocalDensity.current
    CompositionLocalProvider(LocalReducedTransparency provides reduceTransparency){
        GlassPlate(modifier.fillMaxWidth().widthIn(max=560.dp).onGloballyPositioned{origin=it.positionInRoot()}.testTag("glass_dock"),scene=GlassScene(backdrop,contentOrigin),radius=24.dp){
            if(target!=null)Box(Modifier.offset{IntOffset(x.roundToInt(),y.roundToInt())}.requiredSize(with(density){target.width.toDp()},with(density){target.height.toDp()}).testTag("glass_lens")
                .clip(RoundedCornerShape(19.dp)).background(Brush.verticalGradient(listOf(Color.White.copy(alpha=if(dark).15f else .66f),Color.White.copy(alpha=if(dark).045f else .12f))))
                .border(.8.dp,Color.White.copy(alpha=if(dark).24f else .89f),RoundedCornerShape(19.dp)))
            Row(Modifier.fillMaxWidth().padding(4.dp)){
                tabs.forEach{(id,label,icon)->
                    Column(Modifier.weight(1f).heightIn(min=48.dp).onGloballyPositioned{c->val p=c.positionInRoot()-origin;val r=Rect(p.x,p.y,p.x+c.size.width,p.y+c.size.height);if(bounds[id]!=r)bounds[id]=r}
                        .clip(RoundedCornerShape(19.dp)).clickable(role=Role.Tab){if(id!=route)onSelect(id)}.testTag("nav_$id").semantics{selected=id==route;contentDescription=label}
                        .padding(vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)){
                        Icon(icon,null,Modifier.size(19.dp),tint=if(id==route)TraceColors.Ink else TraceColors.Muted)
                        Text(label,style=MaterialTheme.typography.labelSmall.copy(fontSize=if(LocalEasyMode.current)12.sp else 10.5.sp,lineHeight=14.sp),fontWeight=if(id==route)FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
