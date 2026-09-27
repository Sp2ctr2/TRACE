package app.saeon.trace.ui.design

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*

/** No fabricated progress. Hide short waits; placeholders exist only while work is pending. */
@Composable fun DeferredSkeleton(pending:Boolean,modifier:Modifier=Modifier,rows:Int=3,content:@Composable ()->Unit){
    var visible by remember{mutableStateOf(false)}
    LaunchedEffect(pending){if(pending){delay(160);visible=true}else visible=false}
    if(!pending)content() else if(visible)SkeletonRows(modifier,rows)
}
@Composable fun SkeletonRows(modifier:Modifier=Modifier,rows:Int=3){
    val reduced=LocalReducedMotion.current
    val loop=rememberInfiniteTransition(label="loading-placeholder")
    val opacity by loop.animateFloat(.40f,.73f,infiniteRepeatable(tween(900,easing=LinearEasing),RepeatMode.Reverse),label="loading-softness")
    val color=TraceColors.Divider.copy(alpha=if(reduced).8f else opacity)
    Column(modifier.fillMaxWidth().testTag("loading_skeleton").clearAndSetSemantics{contentDescription="불러오는 중";liveRegion=LiveRegionMode.Polite},verticalArrangement=Arrangement.spacedBy(16.dp)){
        repeat(rows){i->Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Box(Modifier.size(32.dp).background(color,RoundedCornerShape(11.dp)))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Box(Modifier.fillMaxWidth(if(i%2==0).56f else .7f).height(11.dp).background(color,RoundedCornerShape(6.dp)))
                Box(Modifier.fillMaxWidth(.35f).height(8.dp).background(color,RoundedCornerShape(5.dp)))
            }
            Box(Modifier.width(58.dp).height(12.dp).background(color,RoundedCornerShape(7.dp)))
        }}
    }
}

/** Both shapes are the original TRACE paths. The detached point docks into its canonical position. */
@Composable fun TraceLaunchMark(progress:Float,modifier:Modifier=Modifier){
    val main=remember{PathParser().parsePathString("M6 9h28v7H23.5v17h-7V16H6z").toPath()}
    val block=remember{PathParser().parsePathString("M6 23h7v10H6z").toPath()}
    val coral=TraceColors.Coral;val dark=TraceColors.Paper.luminance()<.25f
    Canvas(modifier.clearAndSetSemantics{}){
        val s=size.minDimension/40f
        val reveal=TraceMotion.Enter.transform(((progress-.04f)/.42f).coerceIn(0f,1f))
        val dock=TraceMotion.Enter.transform(((progress-.16f)/.54f).coerceIn(0f,1f))
        val drift=1f-dock
        scale(s,s,pivot=Offset.Zero){
            clipRect(right=40f*reveal){drawPath(main,coral)}
            translate(24f*drift,(-15f*drift)+sin(drift*PI.toFloat())*-4f){
                drawPath(block,coral.copy(alpha=((progress-.08f)/.15f).coerceIn(0f,1f)))
            }
        }
    }
}

/** Launch once per cold Activity state; configuration changes and biometric returns do not replay it.
 * The short signature hands off to real loading placeholders if storage is still busy. */
@Composable fun StudioLaunch(ready:Boolean,content:@Composable ()->Unit){
    var finished by rememberSaveable{mutableStateOf(false)}
    val reduced=LocalReducedMotion.current
    val progress=remember{Animatable(if(finished||reduced)1f else 0f)}
    LaunchedEffect(Unit){if(!finished){if(!reduced)progress.animateTo(1f,tween(820,easing=LinearEasing));finished=true}}
    Box(Modifier.fillMaxSize()){
        Box(Modifier.fillMaxSize().then(if(!finished)Modifier.clearAndSetSemantics{}else Modifier)){content()}
        if(!finished){
            val t=progress.value
            val fade=if(t>.82f)(1f-(t-.82f)/.18f).coerceIn(0f,1f)else 1f
            Box(Modifier.fillMaxSize().graphicsLayer{alpha=fade}.background(TraceColors.Paper)
                .testTag("launch_splash").pointerInput(Unit){awaitPointerEventScope{while(true){awaitPointerEvent().changes.forEach{it.consume()}}}}
                .semantics{contentDescription="새온은행 시작";liveRegion=LiveRegionMode.Polite},contentAlignment=Alignment.Center){
                val dark=TraceColors.Paper.luminance()<.25f;val coral=TraceColors.Coral
                Canvas(Modifier.matchParentSize()){
                    drawRect(Brush.radialGradient(listOf(coral.copy(alpha=if(dark).035f else .085f),Color.Transparent),Offset(size.width*.95f,0f),320.dp.toPx()))
                }
                Column(Modifier.graphicsLayer{scaleX=.985f+.015f*TraceMotion.Enter.transform((t/.5f).coerceIn(0f,1f));scaleY=scaleX},horizontalAlignment=Alignment.CenterHorizontally){
                    GlassPlate(Modifier.size(116.dp),radius=32.dp){
                        TraceLaunchMark(t,Modifier.size(80.dp).align(Alignment.Center))
                    }
                    Space(24)
                    Text("새온은행",Modifier.graphicsLayer{alpha=((t-.28f)/.25f).coerceIn(0f,1f)},style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
                }
            }
        }
    }
}
