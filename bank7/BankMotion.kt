package app.saeon.trace.ui.design

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable fun TraceOrbit(modifier:Modifier=Modifier,markSize:Int=48) {
    val reduced=LocalReducedMotion.current
    val angle=if(reduced)-35f else {
        val cycle=rememberInfiniteTransition(label="trace_orbit")
        val a by cycle.animateFloat(-90f,270f,infiniteRepeatable(tween(900,easing=LinearEasing)),label="orbit_angle")
        a
    }
    val line=TraceColors.Divider;val coral=TraceColors.Coral
    Box(modifier.testTag("trace_orbit").clearAndSetSemantics{},contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c=Offset(size.width/2,size.height/2);val radius=size.minDimension*.4f
            drawCircle(line.copy(alpha=.78f),radius,c,style=Stroke(1.4.dp.toPx()))
            if(!reduced)for(i in 0..7) {
                drawArc(coral.copy(alpha=.08f+i*.075f),angle-52+i*6,6f,false,
                    topLeft=Offset(c.x-radius,c.y-radius),size=androidx.compose.ui.geometry.Size(radius*2,radius*2),style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
            }
            val a=Math.toRadians(angle.toDouble());drawCircle(coral,4.dp.toPx(),Offset(c.x+cos(a).toFloat()*radius,c.y+sin(a).toFloat()*radius))
        }
        AppIcon(BankIcons.Trace,size=markSize,tint=coral)
    }
}

@Composable fun BankContextLoading(cancel:()->Unit) {
    var step by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit){delay(240);step=1;delay(340);step=2;delay(350);step=3}
    TaskPage(tag="trace_evaluating",footer={SecondaryButton("송금 취소",Modifier.testTag("trace_cancel"),onClick=cancel)}) {
        val compact=LocalTaskHeight.current<510.dp
        Spacer(Modifier.height(if(compact)18.dp else 58.dp))
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
            TraceOrbit(Modifier.size(if(compact)112.dp else 132.dp),if(compact)43 else 49)
            Space(if(compact)20 else 28)
            Text("송금 앞의 맥락을\n확인하고 있어요",style=MaterialTheme.typography.headlineSmall.copy(fontWeight=FontWeight.SemiBold,lineHeight=33.sp),textAlign=TextAlign.Center,
                modifier=Modifier.semantics{heading();liveRegion=LiveRegionMode.Polite})
            Space(if(compact)16 else 28)
            listOf("요청의 목적","최근 위험 신호","수취인과 거래").forEachIndexed{index,label->
                Row(Modifier.widthIn(max=246.dp).fillMaxWidth().height(34.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=if(step>index)TraceColors.Ink else TraceColors.Muted)
                    AppIcon(if(step>index)BankIcons.Check else BankIcons.More,size=17,tint=if(step>index)TraceColors.CoralText else TraceColors.Divider)
                }
            }
            Space(18);Caption("아직 돈은 이동하지 않았어요.")
        }
    }
}

/** One launch per Activity session. It never approves a transaction and never
 * covers a restored screen for an extra fixed delay after recreation. */
@Composable fun BankEntrance(ready:Boolean) {
    var elapsed by rememberSaveable{mutableStateOf(false)}
    var finished by rememberSaveable{mutableStateOf(false)}
    val reduced=LocalReducedMotion.current
    LaunchedEffect(Unit){if(!elapsed){delay(if(reduced)0 else 560);elapsed=true}}
    LaunchedEffect(ready,elapsed){if(ready&&elapsed)finished=true}
    AnimatedVisibility(!finished,enter=EnterTransition.None,exit=fadeOut(tween(if(reduced)0 else 200)),modifier=Modifier.fillMaxSize()) {
        val appear=remember{Animatable(if(reduced)1f else 0f)}
        LaunchedEffect(Unit){if(!reduced)appear.animateTo(1f,tween(380,easing=TraceMotion.Enter))}
        Box(Modifier.fillMaxSize().background(TraceColors.Paper).testTag("bank_entrance").semantics{paneTitle="새온은행 시작"},contentAlignment=Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.graphicsLayer{alpha=appear.value;translationY=(1-appear.value)*8}) {
                AppIcon(BankIcons.Trace,size=64,tint=TraceColors.Coral)
                Space(18);Text("새온은행",style=MaterialTheme.typography.headlineSmall.copy(fontWeight=FontWeight.SemiBold))
                Space(8);Caption("Protected by TRACE")
            }
        }
    }
}
