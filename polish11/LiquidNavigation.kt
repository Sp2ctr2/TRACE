package app.saeon.trace.ui.design

import android.os.Build
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
import androidx.compose.ui.geometry.CornerRadius
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

/** A sampled navigation material. No bank content or action buttons use this effect.
 * The unmodified content layer is recorded before the dock; recursive self-sampling is impossible.
 * This is an Android interpretation of a liquid material, not an Apple framework.
 */
@Composable fun LiquidNavigation(tabs:List<Triple<String,String,ImageVector>>,route:String,
    source:GraphicsLayer,sourceOrigin:Offset,modifier:Modifier=Modifier,
    reducedTransparency:Boolean=false,onSelect:(String)->Unit){
    val measured=remember{mutableStateMapOf<String,Rect>()}
    var origin by remember{mutableStateOf(Offset.Zero)}
    val target=measured[route]
    val density=LocalDensity.current
    val reduced=LocalReducedMotion.current
    val dark=TraceColors.Paper.luminance()<.25f
    val x by animateFloatAsState(target?.left?:4f,if(reduced)snap()else spring(dampingRatio=1f,stiffness=520f),label="navigation-lens-x")
    val y by animateFloatAsState(target?.top?:4f,if(reduced)snap()else spring(dampingRatio=1f,stiffness=520f),label="navigation-lens-y")
    val w=target?.width?:0f;val h=target?.height?:0f
    val outline=RoundedCornerShape(27.dp)
    Box(modifier.widthIn(max=600.dp).fillMaxWidth().onGloballyPositioned{val p=it.positionInRoot();if(p!=origin)origin=p}
        .testTag("bank_dock").shadow(6.dp,outline,ambientColor=Color.Black.copy(alpha=.04f),spotColor=Color.Black.copy(alpha=.07f)).clip(outline)){
        if(!reducedTransparency)Canvas(Modifier.matchParentSize().graphicsLayer{
            if(Build.VERSION.SDK_INT>=31)renderEffect=BlurEffect(12.dp.toPx(),12.dp.toPx(),TileMode.Clamp)
        }){translate(sourceOrigin.x-origin.x,sourceOrigin.y-origin.y){drawLayer(source)}}
        Box(Modifier.matchParentSize().background(if(reducedTransparency)TraceColors.Surface else if(dark)Color(0xFF222226).copy(alpha=.77f)else Color(0xFFF9FAF7).copy(alpha=.64f)))
        Canvas(Modifier.matchParentSize()){
            drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha=if(dark).20f else .88f),Color.White.copy(alpha=.08f),TraceColors.Divider.copy(alpha=.75f))),cornerRadius=CornerRadius(27.dp.toPx()),style=Stroke(1.dp.toPx()))
        }
        if(target!=null&&w>0&&h>0){
            val specular=if(w>0)((x-target.left)/w).coerceIn(-1f,1f)else 0f
            Box(Modifier.offset{IntOffset(x.roundToInt(),y.roundToInt())}.requiredSize(with(density){w.toDp()},with(density){h.toDp()})
                .testTag("bank_selection").clip(RoundedCornerShape(23.dp))){
                if(!reducedTransparency)Canvas(Modifier.matchParentSize()){
                    // A subtle magnified second sample under the lens gives moving background parallax.
                    val lensOrigin=origin+Offset(x,y)
                    scale(1.045f,1.045f){translate(sourceOrigin.x-lensOrigin.x,sourceOrigin.y-lensOrigin.y){drawLayer(source)}}
                }
                Box(Modifier.matchParentSize().background(if(reducedTransparency)TraceColors.Divider else if(dark)Color(0xFF55555D).copy(alpha=.65f)else Color.White.copy(alpha=.68f)))
                if(!reducedTransparency)Canvas(Modifier.matchParentSize()){
                    val radius=23.dp.toPx()
                    drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha=if(dark).52f else .98f),Color.White.copy(alpha=.10f),Color.White.copy(alpha=if(dark).29f else .79f))),cornerRadius=CornerRadius(radius),style=Stroke(1.dp.toPx()))
                    drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha=if(dark).22f else .53f),Color.Transparent),Offset(size.width*(.25f+specular*.2f),-size.height*.55f),size.width*.8f))
                    drawLine(Brush.horizontalGradient(listOf(Color.Transparent,Color.White.copy(alpha=.85f),Color.Transparent)),Offset(radius*.55f,2.dp.toPx()),Offset(size.width-radius*.55f,2.dp.toPx()),1.dp.toPx(),StrokeCap.Round)
                    drawLine(Brush.horizontalGradient(listOf(Color.Transparent,Color.White.copy(alpha=.45f),Color.Transparent)),Offset(radius*.65f,size.height-2.dp.toPx()),Offset(size.width-radius*.65f,size.height-2.dp.toPx()),.75.dp.toPx(),StrokeCap.Round)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(4.dp)){
            tabs.forEach{(id,label,icon)->
                val interaction=remember{MutableInteractionSource()}
                val down by interaction.collectIsPressedAsState()
                val iconScale by animateFloatAsState(if(down&&!reduced).91f else 1f,if(reduced)snap()else tween(if(down)75 else 210),label="navigation-icon")
                Column(Modifier.weight(1f).heightIn(min=48.dp).onGloballyPositioned{c->
                    val p=c.positionInRoot()-origin;val b=Rect(p.x,p.y,p.x+c.size.width,p.y+c.size.height);if(measured[id]!=b)measured[id]=b
                }.clip(RoundedCornerShape(23.dp)).clickable(interactionSource=interaction,indication=null,role=Role.Tab){if(id!=route)onSelect(id)}
                    .testTag("nav_$id").semantics{selected=id==route;contentDescription=label}
                    .padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)){
                    Icon(icon,null,Modifier.size(19.dp).graphicsLayer{scaleX=iconScale;scaleY=iconScale},tint=if(id==route)TraceColors.Ink else TraceColors.Muted)
                    Text(label,style=MaterialTheme.typography.labelSmall.copy(fontSize=if(LocalEasyMode.current)12.sp else 10.5.sp,lineHeight=14.sp),fontWeight=if(id==route)FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}
