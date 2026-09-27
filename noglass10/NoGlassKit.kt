package app.saeon.trace.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

/** Opaque bank surfaces. No capture layer, blur, refraction, shader or specular simulation. */
@Composable fun BankSurface(modifier:Modifier=Modifier,radius:Dp=18.dp,content:@Composable BoxScope.()->Unit){
    Box(modifier.clip(RoundedCornerShape(radius)).background(TraceColors.Surface),content=content)
}

@Composable fun BankAction(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,smoked:Boolean=false,onClick:()->Unit){
    val source=remember{MutableInteractionSource()}
    val down by source.collectIsPressedAsState()
    val reduced=LocalReducedMotion.current
    val scale by animateFloatAsState(if(down&&!reduced).987f else 1f,if(reduced)snap()else tween(if(down)80 else 210),label="action-pressure")
    val dark=TraceColors.Paper.luminance()<.25f
    val colors=when{
        !enabled->listOf(TraceColors.Divider,TraceColors.Divider)
        smoked&&dark->listOf(Color(0xFF3D3D42),Color(0xFF2D2D31))
        smoked->listOf(Color(0xFF494C46),Color(0xFF373A34))
        dark->listOf(Color(0xFFD2AA99),Color(0xFFBC8C7D))
        else->listOf(Color(0xFFF5A188),Color(0xFFED8065))
    }
    Box(modifier.fillMaxWidth().heightIn(min=if(LocalEasyMode.current)60.dp else 52.dp)
        .graphicsLayer{scaleX=scale;scaleY=scale}
        .clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(colors))
        .clickable(interactionSource=source,indication=null,enabled=enabled,role=Role.Button,onClick=onClick)
        .semantics(mergeDescendants=true){if(!enabled)disabled()},contentAlignment=Alignment.Center){
        Text(text,Modifier.padding(horizontal=12.dp,vertical=12.dp),style=MaterialTheme.typography.labelLarge,
            fontWeight=FontWeight.SemiBold,color=when{!enabled->TraceColors.Muted;smoked->Color(0xFFFAFBF8);else->Color(0xFF3A211A)})
    }
}

/** Small symbol with an unchanged 48dp interaction target. */
@Composable fun BankIconButton(icon:ImageVector,label:String,modifier:Modifier=Modifier,onClick:()->Unit){
    val press=remember{MutableInteractionSource()}
    IconButton(onClick=onClick,interactionSource=press,modifier=modifier.size(48.dp).tracePress(press).semantics{contentDescription=label}){
        Icon(icon,null,Modifier.size(if(LocalEasyMode.current)23.dp else 18.dp),tint=TraceColors.Ink)
    }
}

/** The stronger upper-right coral field spans system bars and every destination. */
@Composable fun BankAmbient(modifier:Modifier=Modifier){
    val paper=TraceColors.Paper;val coral=TraceColors.Coral
    val dark=paper.luminance()<.25f
    Canvas(modifier){
        drawRect(paper)
        val radius=(size.width*1.12f).coerceIn(430.dp.toPx(),1000.dp.toPx())
        drawRect(Brush.radialGradient(
            0f to coral.copy(alpha=if(dark).15f else .29f),
            .24f to coral.copy(alpha=if(dark).095f else .18f),
            .55f to coral.copy(alpha=if(dark).025f else .055f),
            .88f to Color.Transparent,
            center=Offset(size.width*1.04f,-size.height*.025f),radius=radius))
        if(!dark)drawRect(Brush.radialGradient(listOf(Color(0xFFFFB596).copy(alpha=.12f),Color.Transparent),Offset(size.width*.78f,0f),radius*.52f))
    }
}

/** Solid dock; selection follows measured target rectangles, not percent offsets. */
@Composable fun BankDock(tabs:List<Triple<String,String,ImageVector>>,route:String,modifier:Modifier=Modifier,onSelect:(String)->Unit){
    val bounds=remember{mutableStateMapOf<String,Rect>()}
    var origin by remember{mutableStateOf(Offset.Zero)}
    val target=bounds[route]
    val reduced=LocalReducedMotion.current
    val x by animateFloatAsState(target?.left?:4f,if(reduced)snap()else spring(dampingRatio=1f,stiffness=700f),label="navigation-selection")
    val y by animateFloatAsState(target?.top?:4f,if(reduced)snap()else tween(180),label="navigation-baseline")
    val density=LocalDensity.current
    val dark=TraceColors.Paper.luminance()<.25f
    Box(modifier.widthIn(max=600.dp).fillMaxWidth().onGloballyPositioned{origin=it.positionInRoot()}
        .testTag("bank_dock").clip(RoundedCornerShape(20.dp)).background(TraceColors.Surface)
        .border(1.dp,TraceColors.Divider,RoundedCornerShape(20.dp))){
        if(target!=null)Box(Modifier.offset{IntOffset(x.roundToInt(),y.roundToInt())}
            .requiredSize(with(density){target.width.toDp()},with(density){target.height.toDp()})
            .testTag("bank_selection").clip(RoundedCornerShape(15.dp))
            .background(if(dark)Color(0xFF333337)else Color(0xFFE8EBE4))){
            Box(Modifier.align(Alignment.TopCenter).padding(top=4.dp).size(17.dp,2.dp).background(TraceColors.Coral,RoundedCornerShape(2.dp)))
        }
        Row(Modifier.fillMaxWidth().padding(4.dp)){
            tabs.forEach{(id,label,icon)->
                Column(Modifier.weight(1f).heightIn(min=48.dp).onGloballyPositioned{c->
                    val p=c.positionInRoot()-origin;val r=Rect(p.x,p.y,p.x+c.size.width,p.y+c.size.height)
                    if(bounds[id]!=r)bounds[id]=r
                }.clip(RoundedCornerShape(15.dp)).clickable(role=Role.Tab){if(id!=route)onSelect(id)}
                    .testTag("nav_$id").semantics{selected=id==route;contentDescription=label}
                    .padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)){
                    Icon(icon,null,Modifier.size(18.dp),tint=if(id==route)TraceColors.Ink else TraceColors.Muted)
                    Text(label,style=MaterialTheme.typography.labelSmall.copy(fontSize=if(LocalEasyMode.current)12.sp else 10.5.sp,lineHeight=14.sp),fontWeight=if(id==route)FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}
