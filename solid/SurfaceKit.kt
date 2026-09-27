package app.saeon.trace.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Immutable
data class AdaptiveSpec(
    val width:Dp,
    val height:Dp,
    val useRail:Boolean,
    val twoPane:Boolean,
    val compactHeight:Boolean,
    val contentMax:Dp,
    val horizontalPadding:Dp
)
val LocalAdaptiveSpec=staticCompositionLocalOf {
    AdaptiveSpec(390.dp,844.dp,false,false,false,600.dp,20.dp)
}
fun adaptiveSpec(width:Dp,height:Dp):AdaptiveSpec {
    val compactHeight=height<480.dp
    val useRail=width>=600.dp || compactHeight
    val twoPane=width>=720.dp && height>=360.dp
    val max=when {
        width>=1200.dp->1040.dp
        width>=840.dp->920.dp
        width>=600.dp->760.dp
        else->600.dp
    }
    val pad=when {
        width>=1200.dp->36.dp
        width>=840.dp->30.dp
        width>=600.dp->24.dp
        else->20.dp
    }
    return AdaptiveSpec(width,height,useRail,twoPane,compactHeight,max,pad)
}

enum class ActionTone { CORAL, INK, NEUTRAL }

@Composable fun GradientAction(
    text:String,
    modifier:Modifier=Modifier,
    enabled:Boolean=true,
    tone:ActionTone=ActionTone.CORAL,
    onClick:()->Unit
){
    val source=remember{MutableInteractionSource()}
    val pressed by source.collectIsPressedAsState()
    val reduced=LocalReducedMotion.current
    val press by animateFloatAsState(if(pressed)1f else 0f,if(reduced)snap() else tween(if(pressed)85 else 220,easing=TraceMotion.Enter),label="action_press")
    val dark=TraceColors.Paper.luminance()<.25f
    val shape=RoundedCornerShape(if(LocalEasyMode.current)18.dp else 16.dp)
    val colors=when {
        !enabled->listOf(TraceColors.Divider,TraceColors.Divider)
        tone==ActionTone.INK && dark->listOf(Color(0xFF4A4A4E),Color(0xFF29292D))
        tone==ActionTone.INK->listOf(Color(0xFF343633),Color(0xFF20211F))
        tone==ActionTone.NEUTRAL && dark->listOf(Color(0xFF2B2B30),Color(0xFF242428))
        tone==ActionTone.NEUTRAL->listOf(Color(0xFFFAFAF8),Color(0xFFF0F1EE))
        dark->listOf(Color(0xFFD99A86),Color(0xFFC98370),Color(0xFFB96B5D))
        else->listOf(Color(0xFFFFAE98),Color(0xFFF47B60),Color(0xFFEA5239))
    }
    val content=when {
        !enabled->TraceColors.Muted
        tone==ActionTone.INK->Color(0xFFF8F8F6)
        tone==ActionTone.NEUTRAL->TraceColors.Ink
        else->if(dark)Color(0xFF211311) else Color(0xFF2D1511)
    }
    Box(
        modifier.fillMaxWidth().heightIn(min=if(LocalEasyMode.current)64.dp else 52.dp)
            .graphicsLayer{scaleX=1f-.012f*press;scaleY=scaleX}
            .shadow(if(enabled)5.dp else 0.dp,shape,ambientColor=Color.Black.copy(alpha=.035f),spotColor=Color.Black.copy(alpha=.08f))
            .clip(shape)
            .background(Brush.linearGradient(colors,start=Offset.Zero,end=Offset.Infinite))
            .border(1.dp,if(dark)Color.White.copy(alpha=.09f) else Color.White.copy(alpha=.34f),shape)
            .clickable(interactionSource=source,indication=null,enabled=enabled,role=Role.Button,onClick=onClick)
            .semantics{role=Role.Button},
        contentAlignment=Alignment.Center
    ){
        Canvas(Modifier.matchParentSize()){
            drawRoundRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha=if(dark).12f else .30f),Color.Transparent)),
                cornerRadius=CornerRadius(16.dp.toPx())
            )
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent,Color.White.copy(alpha=if(dark).20f else .45f),Color.Transparent)),
                Offset(size.width*.18f,1.dp.toPx()),Offset(size.width*.82f,1.dp.toPx()),1.dp.toPx()
            )
        }
        Text(text,color=content,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)
    }
}

@Composable fun PlainIconAction(
    icon:ImageVector,
    label:String,
    modifier:Modifier=Modifier,
    onClick:()->Unit
){
    val source=remember{MutableInteractionSource()}
    val pressed by source.collectIsPressedAsState()
    val alpha by animateFloatAsState(if(pressed).08f else 0f,tween(120),label="icon_press")
    Box(
        modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
            .background(TraceColors.Ink.copy(alpha=alpha))
            .clickable(interactionSource=source,indication=null,role=Role.Button,onClick=onClick)
            .semantics{this.role=Role.Button},
        contentAlignment=Alignment.Center
    ){
        AppIcon(icon,description=label,size=19,tint=TraceColors.Ink)
    }
}

@Composable fun AccountSelector(label:String,modifier:Modifier=Modifier,onClick:()->Unit){
    Row(
        modifier.heightIn(min=40.dp).clip(RoundedCornerShape(13.dp))
            .background(TraceColors.Surface.copy(alpha=.72f))
            .border(.8.dp,TraceColors.Divider.copy(alpha=.75f),RoundedCornerShape(13.dp))
            .clickable(role=Role.Button,onClick=onClick)
            .padding(horizontal=11.dp,vertical=7.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(6.dp)
    ){
        Text(label,style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.Medium)
        AppIcon(BankIcons.Chevron,size=11,tint=TraceColors.Muted)
    }
}

@Composable fun SolidAmbient(modifier:Modifier=Modifier){
    val dark=TraceColors.Paper.luminance()<.25f
    Canvas(modifier){
        drawRect(TraceColors.Paper)
        drawRect(
            Brush.radialGradient(
                listOf(TraceColors.Coral.copy(alpha=if(dark).045f else .13f),Color.Transparent),
                center=Offset(size.width*.98f,-24.dp.toPx()),
                radius=(size.minDimension*.72f).coerceAtLeast(320.dp.toPx())
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf((if(dark)Color.White else Color(0xFF738078)).copy(alpha=if(dark).018f else .035f),Color.Transparent),
                center=Offset(-48.dp.toPx(),size.height*.92f),
                radius=size.minDimension*.62f
            )
        )
    }
}

@Composable fun SolidBottomNavigation(
    tabs:List<Triple<String,String,ImageVector>>,
    route:String,
    modifier:Modifier=Modifier,
    onSelect:(String)->Unit
){
    val dark=TraceColors.Paper.luminance()<.25f
    Surface(
        modifier=modifier.fillMaxWidth().widthIn(max=560.dp).testTag("root_navigation"),
        color=if(dark)Color(0xFF202024) else Color(0xFFFAFAF8),
        shape=RoundedCornerShape(22.dp),
        border=BorderStroke(.8.dp,if(dark)Color.White.copy(alpha=.08f) else TraceColors.Divider.copy(alpha=.85f)),
        shadowElevation=7.dp,
        tonalElevation=0.dp
    ){
        Row(Modifier.fillMaxWidth().padding(4.dp)){
            tabs.forEach{(id,label,icon)->
                val selected=id==route
                Column(
                    Modifier.weight(1f).height(48.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if(selected)TraceColors.Ink.copy(alpha=if(dark).13f else .055f) else Color.Transparent)
                        .clickable(role=Role.Tab){if(!selected)onSelect(id)}
                        .testTag("nav_$id").semantics{this.selected=selected},
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.Center
                ){
                    AppIcon(icon,size=18,tint=if(selected)TraceColors.Ink else TraceColors.Muted)
                    Spacer(Modifier.height(2.dp))
                    Text(label,style=MaterialTheme.typography.labelSmall.copy(fontSize=10.sp,lineHeight=12.sp),fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal,color=if(selected)TraceColors.Ink else TraceColors.Muted)
                }
            }
        }
    }
}

@Composable fun SolidNavigationRail(
    tabs:List<Triple<String,String,ImageVector>>,
    route:String,
    modifier:Modifier=Modifier,
    onSelect:(String)->Unit
){
    val dark=TraceColors.Paper.luminance()<.25f
    Column(
        modifier.width(78.dp).fillMaxHeight().testTag("root_navigation_rail")
            .background(if(dark)Color(0xFF1D1D21) else Color(0xFFF8F9F6))
            .border(BorderStroke(.7.dp,if(dark)Color.White.copy(alpha=.06f) else TraceColors.Divider.copy(alpha=.72f)),RoundedCornerShape(0.dp))
            .padding(top=16.dp,bottom=18.dp),
        horizontalAlignment=Alignment.CenterHorizontally
    ){
        AppIcon(BankIcons.Trace,size=28,tint=TraceColors.Coral)
        Spacer(Modifier.height(24.dp))
        tabs.forEach{(id,label,icon)->
            val selected=id==route
            Box(
                Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=3.dp)
                    .height(54.dp).clip(RoundedCornerShape(16.dp))
                    .background(if(selected)TraceColors.Ink.copy(alpha=if(dark).14f else .055f) else Color.Transparent)
                    .clickable(role=Role.Tab){if(!selected)onSelect(id)}
                    .testTag("nav_$id").semantics{this.selected=selected},
                contentAlignment=Alignment.Center
            ){
                Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)){
                    AppIcon(icon,size=19,tint=if(selected)TraceColors.Ink else TraceColors.Muted)
                    Text(label,style=MaterialTheme.typography.labelSmall.copy(fontSize=10.sp),fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal,color=if(selected)TraceColors.Ink else TraceColors.Muted)
                }
                if(selected)Box(Modifier.align(Alignment.CenterEnd).width(3.dp).height(22.dp).background(TraceColors.Coral,RoundedCornerShape(3.dp)))
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
