package app.saeon.trace.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable fun TraceLaunchMark(progress:Float,modifier:Modifier=Modifier){
    val main=remember{PathParser().parsePathString("M6 9h28v7H23.5v17h-7V16H6z").toPath()}
    val block=remember{PathParser().parsePathString("M6 23h7v10H6z").toPath()}
    val coral=TraceColors.Coral
    Canvas(modifier.clearAndSetSemantics{}){
        val unit=size.minDimension/40f
        val mainReveal=TraceMotion.Enter.transform(((progress-.04f)/.34f).coerceIn(0f,1f))
        val dock=TraceMotion.Enter.transform(((progress-.18f)/.48f).coerceIn(0f,1f))
        val trail=((progress-.12f)/.46f).coerceIn(0f,1f)
        scale(unit,unit,pivot=Offset.Zero){
            clipRect(right=40f*mainReveal){drawPath(main,coral)}
            val x=6f+24f*(1f-dock)
            val y=23f-15f*(1f-dock)-sin(dock*PI.toFloat())*6f
            translate(x-6f,y-23f){
                drawPath(block,coral.copy(alpha=((progress-.12f)/.10f).coerceIn(0f,1f)))
            }
            if(trail>0f&&dock<.98f){
                repeat(4){i->
                    val q=(dock-i*.055f).coerceIn(0f,1f)
                    val tx=9.5f+24f*(1f-q)
                    val ty=28f-15f*(1f-q)-sin(q*PI.toFloat())*6f
                    drawCircle(
                        coral.copy(alpha=(.18f-i*.035f)*(1f-dock)),
                        radius=(1.45f-i*.18f).coerceAtLeast(.7f),
                        center=Offset(tx,ty)
                    )
                }
            }
        }
    }
}

@Composable fun StudioLaunch(ready:Boolean,content:@Composable ()->Unit){
    var completed by rememberSaveable{mutableStateOf(false)}
    val reduced=LocalReducedMotion.current
    val progress=remember{Animatable(if(completed||reduced)1f else 0f)}
    LaunchedEffect(Unit){
        if(!completed){
            if(!reduced)progress.animateTo(1f,tween(980,easing=LinearEasing))
            completed=true
        }
    }
    Box(Modifier.fillMaxSize()){
        Box(Modifier.fillMaxSize().then(if(!completed)Modifier.clearAndSetSemantics{}else Modifier)){content()}
        if(!completed){
            val t=progress.value
            val fade=if(t>.84f)(1f-(t-.84f)/.16f).coerceIn(0f,1f)else 1f
            val settle=TraceMotion.Enter.transform((t/.72f).coerceIn(0f,1f))
            val paper=TraceColors.Paper
            val coral=TraceColors.Coral
            Box(
                Modifier.fillMaxSize().graphicsLayer{alpha=fade}.background(paper)
                    .testTag("launch_splash")
                    .pointerInput(Unit){awaitPointerEventScope{while(true){awaitPointerEvent().changes.forEach{it.consume()}}}}
                    .semantics{contentDescription="새온은행 시작";liveRegion=LiveRegionMode.Polite},
                contentAlignment=Alignment.Center
            ){
                Canvas(Modifier.matchParentSize()){
                    drawRect(paper)
                    drawRect(
                        Brush.radialGradient(
                            listOf(coral.copy(alpha=.13f),Color.Transparent),
                            center=Offset(size.width*.95f,-20.dp.toPx()),
                            radius=350.dp.toPx()
                        )
                    )
                    val wave=((t-.55f)/.28f).coerceIn(0f,1f)
                    if(wave>0f&&wave<1f){
                        drawCircle(
                            coral.copy(alpha=.10f*(1f-wave)),
                            radius=(54+80*wave).dp.toPx(),
                            center=Offset(size.width/2,size.height/2-30.dp.toPx()),
                            style=Stroke(1.dp.toPx())
                        )
                    }
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally){
                    Box(
                        Modifier.size(118.dp).graphicsLayer{scaleX=.94f+.06f*settle;scaleY=scaleX},
                        contentAlignment=Alignment.Center
                    ){
                        TraceLaunchMark(t,Modifier.size(84.dp))
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(
                        "새온은행",
                        Modifier.graphicsLayer{
                            val textT=((t-.55f)/.22f).coerceIn(0f,1f)
                            alpha=textT
                            translationY=(1f-textT)*7.dp.toPx()
                        },
                        style=MaterialTheme.typography.titleLarge,
                        fontWeight=FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "TRACE",
                        Modifier.graphicsLayer{alpha=((t-.66f)/.18f).coerceIn(0f,1f)},
                        style=MaterialTheme.typography.labelMedium,
                        color=TraceColors.Muted,
                        letterSpacing=1.4.sp
                    )
                }
            }
        }
    }
}
