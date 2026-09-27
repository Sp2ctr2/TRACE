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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlin.math.*

/** Sample a separate scene, never recursively sample the control's own layer. */
data class GlassScene(val layer:GraphicsLayer,val origin:Offset)
val LocalGlassScene=staticCompositionLocalOf<GlassScene?>{null}

private const val EDGE_SHADER="""
uniform shader scene;
uniform float2 extent;
uniform float corner;
half4 main(float2 p) {
    float2 c=extent*.5;
    float2 v=p-c;
    float2 q=abs(v)-(c-float2(corner));
    float d=length(max(q,float2(0)))+min(max(q.x,q.y),0.0)-corner;
    float edge=1.0-smoothstep(0.0,12.0,-d);
    float2 n=normalize(v/max(c,float2(1))+float2(.0001));
    float2 uv=clamp(p-n*edge*5.5,float2(1),extent-float2(1));
    return scene.eval(uv);
}
"""

/** Android approximation: live source sampling, bounded blur and edge lensing (API 33+).
 * No Apple private/system material is used. Older APIs retain the same legible material.
 */
@Composable fun GlassPlate(modifier:Modifier=Modifier,tinted:Boolean=false,radius:Dp=24.dp,
    scene:GlassScene?=LocalGlassScene.current,pressed:Boolean=false,content:@Composable BoxScope.()->Unit) {
    var origin by remember{mutableStateOf(Offset.Zero)}
    val solid=LocalReducedTransparency.current
    val dark=TraceColors.Paper.luminance()<.25f
    val shader=remember{if(Build.VERSION.SDK_INT>=33)RuntimeShader(EDGE_SHADER)else null}
    val palette=LocalTracePalette.current
    val shape=RoundedCornerShape(radius)
    val press by animateFloatAsState(if(pressed)1f else 0f,if(LocalReducedMotion.current)snap()else tween(150),label="glass-press")
    Box(modifier.onGloballyPositioned{val next=it.positionInRoot();if(next!=origin)origin=next}
        .graphicsLayer{scaleX=1f-.012f*press;scaleY=scaleX}
        .shadow(if(tinted)10.dp else 7.dp,shape,ambientColor=Color.Black.copy(alpha=.055f),spotColor=Color.Black.copy(alpha=.09f))
        .clip(shape)) {
        if(scene!=null&&!solid)Canvas(Modifier.matchParentSize().graphicsLayer{
            if(Build.VERSION.SDK_INT>=33&&shader!=null&&size.width>0&&size.height>0){
                shader.setFloatUniform("extent",size.width,size.height)
                shader.setFloatUniform("corner",radius.toPx())
                val blur=android.graphics.RenderEffect.createBlurEffect(12.dp.toPx(),12.dp.toPx(),android.graphics.Shader.TileMode.CLAMP)
                renderEffect=android.graphics.RenderEffect.createChainEffect(android.graphics.RenderEffect.createRuntimeShaderEffect(shader,"scene"),blur).asComposeRenderEffect()
            }else if(Build.VERSION.SDK_INT>=31)renderEffect=BlurEffect(12.dp.toPx(),12.dp.toPx(),TileMode.Clamp)
        }){translate(scene.origin.x-origin.x,scene.origin.y-origin.y){drawLayer(scene.layer)}}
        val colors=when{
            solid&&tinted->listOf(Color(0xFFF49A80),Color(0xFFF49A80))
            solid->listOf(palette.Surface,palette.Surface)
            tinted&&dark->listOf(Color(0xFFFFAE92).copy(alpha=.92f),Color(0xFFF37259).copy(alpha=.85f),palette.Coral.copy(alpha=.83f))
            tinted->listOf(Color(0xFFFFB59C).copy(alpha=.82f),Color(0xFFF77F62).copy(alpha=.85f),palette.Coral.copy(alpha=.86f))
            dark->listOf(Color(0xFF383D3A).copy(alpha=.78f),Color(0xFF242926).copy(alpha=.73f))
            else->listOf(Color.White.copy(alpha=.55f),Color(0xFFF8FAF7).copy(alpha=.36f))
        }
        Box(Modifier.matchParentSize().background(Brush.linearGradient(colors)))
        if(!solid)Canvas(Modifier.matchParentSize()){
            drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha=if(tinted).34f else .38f),Color.Transparent),center=Offset(size.width*(.18f+.62f*press),-size.height*.2f),radius=size.width*.63f))
            drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha=.86f),Color.White.copy(alpha=.06f),Color.White.copy(alpha=.42f))),cornerRadius=androidx.compose.ui.geometry.CornerRadius(radius.toPx()),style=Stroke(1.dp.toPx()))
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent,Color.White.copy(alpha=.78f),Color.Transparent)),Offset(radius.toPx()*.8f,1.dp.toPx()),Offset(size.width-radius.toPx()*.8f,1.dp.toPx()),1.dp.toPx(),StrokeCap.Round)
            drawLine(Color.Black.copy(alpha=if(tinted).09f else .045f),Offset(radius.toPx(),size.height-1.dp.toPx()),Offset(size.width-radius.toPx(),size.height-1.dp.toPx()),1.dp.toPx())
        }
        content()
    }
}

@Composable fun GlassAction(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,onClick:()->Unit){
    val interaction=remember{MutableInteractionSource()}
    val pressed by interaction.collectIsPressedAsState()
    GlassPlate(modifier.fillMaxWidth().heightIn(min=if(LocalEasyMode.current)64.dp else 54.dp)
        .semantics(mergeDescendants=true){if(!enabled)disabled()}
        .clickable(interactionSource=interaction,indication=null,enabled=enabled,role=Role.Button,onClick=onClick),tinted=enabled,pressed=pressed){
        Text(text,Modifier.align(Alignment.Center).padding(horizontal=16.dp,vertical=12.dp),
            color=if(enabled)Color(0xFF391911)else TraceColors.Muted,
            style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
    }
}

@Composable fun GlassIcon(icon:ImageVector,label:String,modifier:Modifier=Modifier,onClick:()->Unit){
    val interaction=remember{MutableInteractionSource()};val pressed by interaction.collectIsPressedAsState()
    GlassPlate(modifier.size(48.dp).semantics{contentDescription=label}.clickable(interactionSource=interaction,indication=null,role=Role.Button,onClick=onClick),radius=17.dp,pressed=pressed){
        Icon(icon,null,Modifier.size(21.dp).align(Alignment.Center),tint=TraceColors.Ink)
    }
}

/** Quiet scene color, recorded separately for CTA samples. The content layer stays solid. */
@Composable fun GlassAmbient(layer:GraphicsLayer,modifier:Modifier=Modifier){
    val paper=TraceColors.Paper;val coral=TraceColors.Coral;val dark=paper.luminance()<.25f
    Canvas(modifier.drawWithContent{layer.record{this@drawWithContent.drawContent()};drawLayer(layer)}){
        drawRect(paper)
        drawRect(Brush.radialGradient(listOf(coral.copy(alpha=if(dark).12f else .085f),Color.Transparent),Offset(size.width*.97f,-24.dp.toPx()),radius=310.dp.toPx()))
    }
}

/** Lens bounds come from measured tab rectangles, including padding and actual font metrics. */
@Composable fun CalibratedDock(tabs:List<Triple<String,String,ImageVector>>,route:String,backdrop:GraphicsLayer,contentOrigin:Offset,
    modifier:Modifier=Modifier,reduceTransparency:Boolean=LocalReducedTransparency.current,onSelect:(String)->Unit){
    val bounds=remember{mutableStateMapOf<String,Rect>()}
    var dockOrigin by remember{mutableStateOf(Offset.Zero)}
    val selected=bounds[route]
    val reduce=LocalReducedMotion.current
    val targetX=selected?.left?:6f
    val targetY=selected?.top?:6f
    val x by animateFloatAsState(targetX,if(reduce)snap()else spring(dampingRatio=1f,stiffness=650f),label="dock-lens-x")
    val y by animateFloatAsState(targetY,if(reduce)snap()else tween(220),label="dock-lens-y")
    val width=selected?.width?:0f;val height=selected?.height?:0f
    val dark=TraceColors.Paper.luminance()<.25f
    CompositionLocalProvider(LocalReducedTransparency provides reduceTransparency){
        GlassPlate(modifier.fillMaxWidth().widthIn(max=560.dp).onGloballyPositioned{dockOrigin=it.positionInRoot()}.testTag("glass_dock"),scene=GlassScene(backdrop,contentOrigin),radius=28.dp){
            if(width>0&&height>0)Box(Modifier.offset{IntOffset(x.roundToInt(),y.roundToInt())}.requiredSize(with(androidx.compose.ui.platform.LocalDensity.current){width.toDp()},with(androidx.compose.ui.platform.LocalDensity.current){height.toDp()})
                .testTag("glass_lens").clip(RoundedCornerShape(21.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha=if(dark).16f else .64f),Color.White.copy(alpha=if(dark).07f else .2f))))
                .border(.8.dp,Color.White.copy(alpha=if(dark).22f else .88f),RoundedCornerShape(21.dp)))
            Row(Modifier.fillMaxWidth().padding(6.dp)){
                tabs.forEach{(id,label,icon)->
                    Column(Modifier.weight(1f).heightIn(min=56.dp).onGloballyPositioned{c->val p=c.positionInRoot()-dockOrigin;val r=Rect(p.x,p.y,p.x+c.size.width,p.y+c.size.height);if(bounds[id]!=r)bounds[id]=r}
                        .clip(RoundedCornerShape(21.dp)).clickable(role=Role.Tab){onSelect(id)}.testTag("nav_$id").semantics{this.selected=id==route;contentDescription=label}
                        .padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Icon(icon,null,Modifier.size(21.dp),tint=if(id==route)TraceColors.Ink else TraceColors.Muted)
                        Text(label,style=MaterialTheme.typography.labelSmall.copy(fontSize=if(LocalEasyMode.current)12.sp else 11.sp),fontWeight=if(id==route)FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
