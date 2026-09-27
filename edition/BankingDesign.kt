package app.saeon.trace.ui.design

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.*

/** Design primitives carry no bank state, demo scenario or payment authorization. */
@Composable fun BankAmount(value:Long,modifier:Modifier=Modifier,size:Int=36,center:Boolean=false) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val scale=LocalDensity.current.fontScale
        val digits=won(value)
        val fit=(maxWidth.value/(scale*(digits.length*.63f+1.2f))).coerceIn(18f,size.toFloat())
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontSize=fit.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.8).sp)){append(digits)}
            withStyle(SpanStyle(fontSize=(fit*.52f).sp,fontWeight=FontWeight.Medium,letterSpacing=0.sp)){append(" 원")}
        },Modifier.fillMaxWidth().semantics{contentDescription="${digits}원"},
            style=MaterialTheme.typography.displaySmall.copy(lineHeight=(fit*1.24f).sp,fontFeatureSettings="tnum"),
            textAlign=if(center)TextAlign.Center else TextAlign.Start,color=TraceColors.Ink)
    }
}
@Composable fun BankAction(text:String,modifier:Modifier=Modifier,enabled:Boolean=true,trace:Boolean=false,onClick:()->Unit) {
    val press=remember{androidx.compose.foundation.interaction.MutableInteractionSource()}
    Button(onClick,modifier.tracePress(press).fillMaxWidth().heightIn(min=54.dp),enabled=enabled,
        interactionSource=press,shape=RoundedCornerShape(18.dp),contentPadding=PaddingValues(16.dp,14.dp),
        colors=ButtonDefaults.buttonColors(containerColor=if(trace)TraceColors.Coral else TraceColors.Ink,
            contentColor=if(trace)Color.White else TraceColors.Paper,
            disabledContainerColor=TraceColors.Divider,disabledContentColor=TraceColors.Muted)) {
        Text(text,style=MaterialTheme.typography.labelLarge.copy(fontSize=if(trace)19.sp else 17.sp,fontWeight=FontWeight.Bold),textAlign=TextAlign.Center)
    }
}
@Composable fun BankRow(title:String,subtitle:String?=null,value:String?=null,icon:androidx.compose.ui.graphics.vector.ImageVector?=null,
    modifier:Modifier=Modifier,onClick:()->Unit) {
    Row(modifier.fillMaxWidth().heightIn(min=if(subtitle==null)52.dp else 64.dp).clickable(role=Role.Button,onClick=onClick).padding(vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        if(icon!=null)Box(Modifier.size(36.dp).background(TraceColors.Surface,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){AppIcon(icon,size=19,tint=TraceColors.Muted)}
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Medium)
            if(!subtitle.isNullOrBlank())Text(subtitle,style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
        }
        if(value!=null)Text(value,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.End)
        AppIcon(BankIcons.Chevron,size=14,tint=TraceColors.Muted)
    }
}
@Composable fun QuietSurface(modifier:Modifier=Modifier,padding:Int=18,content:@Composable ColumnScope.()->Unit) {
    Column(modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(22.dp)).padding(padding.dp),content=content)
}
@Composable fun SectionHeading(title:String,action:String?=null,onAction:(()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().heightIn(min=44.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
        if(action!=null&&onAction!=null)QuietButton(action,onClick=onAction)
    }
}

/** Exactly one orbit. No incoming lines, mesh, glow or scanner. */
@Composable fun TraceOrbit(modifier:Modifier=Modifier) {
    val reduced=LocalReducedMotion.current
    val angle:Float=if(reduced) -35f else {
        val loop=rememberInfiniteTransition(label="trace_orbit")
        val rotation by loop.animateFloat(-90f,270f,infiniteRepeatable(tween(900,easing=LinearEasing),RepeatMode.Restart),label="orbit_angle")
        rotation
    }
    val coral=TraceColors.Coral;val line=TraceColors.Divider
    Box(modifier.size(112.dp).testTag("trace_orbit"),contentAlignment=Alignment.Center) {
        Canvas(Modifier.matchParentSize().clearAndSetSemantics{}) {
            val r=size.minDimension*.43f;val c=center
            drawCircle(line.copy(alpha=.7f),r,c,style=Stroke(1.25.dp.toPx()))
            if(!reduced)repeat(7){i->
                drawArc(coral.copy(alpha=(.55f-i*.065f).coerceAtLeast(.08f)),angle-8f*(i+1),7.5f,false,
                    topLeft=Offset(c.x-r,c.y-r),size=androidx.compose.ui.geometry.Size(r*2,r*2),style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
            }
            val rad=Math.toRadians(angle.toDouble());val p=Offset(c.x+cos(rad).toFloat()*r,c.y+sin(rad).toFloat()*r)
            drawCircle(coral,3.4.dp.toPx(),p)
        }
        Icon(BankIcons.Trace,"TRACE",Modifier.size(51.dp),tint=coral)
    }
}
@Composable fun BankingGate(cancel:()->Unit) {
    var checked by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit){delay(240);checked=1;delay(340);checked=2;delay(350);checked=3}
    TaskPage(tag="trace_evaluating",footer={SecondaryButton("송금 취소",Modifier.testTag("gate_cancel"),onClick=cancel)}) {
        val dense=LocalTaskHeight.current<510.dp
        Space(if(dense)16 else 58)
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
            TraceOrbit();Space(if(dense)22 else 34)
            Text("송금 앞의 맥락을\n확인하고 있어요",style=MaterialTheme.typography.headlineSmall.copy(letterSpacing=(-.55).sp),
                textAlign=TextAlign.Center,modifier=Modifier.semantics{heading();liveRegion=LiveRegionMode.Polite})
            Space(if(dense)18 else 28)
            listOf("요청의 목적","최근 위험 신호","수취인과 거래").forEachIndexed{index,label->
                Row(Modifier.widthIn(max=248.dp).fillMaxWidth().heightIn(min=34.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=if(checked>index)TraceColors.Ink else TraceColors.Muted)
                    AppIcon(if(checked>index)BankIcons.Check else BankIcons.More,size=16,tint=if(checked>index)TraceColors.CoralText else TraceColors.Divider)
                }
            }
            Space(16);Caption("아직 돈은 이동하지 않았어요.")
        }
    }
}

/** First render only. The host decides readiness; animation cannot alter it. */
@Composable fun BankingLaunch(modifier:Modifier=Modifier) {
    val reduced=LocalReducedMotion.current
    val settle=remember{Animatable(if(reduced)1f else 0f)}
    LaunchedEffect(reduced){if(reduced)settle.snapTo(1f)else settle.animateTo(1f,tween(420,easing=TraceMotion.Enter))}
    Box(modifier.fillMaxSize().background(TraceColors.Paper).testTag("bank_launch"),contentAlignment=Alignment.Center) {
        Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.graphicsLayer{alpha=.4f+.6f*settle.value;translationY=(1-settle.value)*12f}) {
            Icon(BankIcons.Trace,null,Modifier.size(72.dp),tint=TraceColors.Coral)
            Space(20);Text("새온은행",style=MaterialTheme.typography.titleLarge.copy(letterSpacing=(-.6).sp))
            Space(6);Caption("보내기 전, 한 번 더.")
        }
    }
}
