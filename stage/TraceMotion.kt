package app.saeon.trace.ui.design

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay

object TraceMotion {
    const val Touch=90; const val Micro=170; const val Local=230
    const val Spatial=320; const val Decision=460; const val Gate=1290
    val Enter=CubicBezierEasing(.22f,1f,.36f,1f)
    val Exit=CubicBezierEasing(.4f,0f,1f,1f)
    val Brake=CubicBezierEasing(.16f,1f,.3f,1f)
}
val LocalReducedTransparency=staticCompositionLocalOf{false}
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalMotionShared=staticCompositionLocalOf<SharedTransitionScope?>{null}
val LocalMotionVisibility=staticCompositionLocalOf<AnimatedVisibilityScope?>{null}

@Composable fun Modifier.tracePress(source:MutableInteractionSource):Modifier {
    val pressed by source.collectIsPressedAsState()
    val reduced=LocalReducedMotion.current
    val scale by animateFloatAsState(if(pressed&&!reduced).982f else 1f,
        animationSpec=if(pressed)tween(TraceMotion.Touch)else spring(dampingRatio=1f,stiffness=650f),label="touch_pressure")
    return graphicsLayer{scaleX=scale;scaleY=scale}
}
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable fun Modifier.traceShared(key:String):Modifier {
    val shared=LocalMotionShared.current;val visibility=LocalMotionVisibility.current
    if(shared==null||visibility==null||LocalReducedMotion.current)return this
    return with(shared){this@traceShared.sharedBounds(rememberSharedContentState(key),visibility,
        boundsTransform={_,_->tween(TraceMotion.Spatial,easing=TraceMotion.Enter)},
        enter=fadeIn(tween(TraceMotion.Micro)),exit=fadeOut(tween(TraceMotion.Micro)),
        resizeMode=SharedTransitionScope.ResizeMode.ScaleToBounds())}
}
@Composable fun RecipientTitle(id:String,name:String,modifier:Modifier=Modifier) {
    Text("${name}님에게",modifier.traceShared("payee/$id"),style=MaterialTheme.typography.titleMedium)
}
@Composable fun AnimatedDigits(value:Long,modifier:Modifier=Modifier,fontSize:TextUnit=36.sp,color:Color=TraceColors.Ink,suffix:String="원") {
    val text=won(value)
    val reduced=LocalReducedMotion.current
    Row(modifier.clearAndSetSemantics{contentDescription="$text$suffix"},verticalAlignment=Alignment.CenterVertically) {
        text.forEachIndexed { index,char ->
            key(text.length-index) {
                AnimatedContent(char,modifier=Modifier.clipToBounds(),transitionSpec={
                    if(reduced)EnterTransition.None togetherWith ExitTransition.None
                    else (fadeIn(tween(TraceMotion.Micro))+slideInVertically(tween(TraceMotion.Micro)){it/5}) togetherWith
                        (fadeOut(tween(110))+slideOutVertically(tween(TraceMotion.Micro)){-it/5})
                },label="digit_${text.length-index}") { c ->
                    Text(c.toString(),fontSize=fontSize,fontWeight=FontWeight.SemiBold,color=color,
                        style=MaterialTheme.typography.displaySmall.copy(fontFeatureSettings="tnum",lineHeight=(fontSize.value*1.22).sp,letterSpacing=(-.6).sp))
                }
            }
        }
        Text(suffix,Modifier.padding(start=4.dp),fontSize=(fontSize.value*.56f).sp,fontWeight=FontWeight.Medium,color=color)
    }
}
@Composable fun MotionMoney(value:Long,modifier:Modifier=Modifier,sharedId:String?=null) {
    BoxWithConstraints(modifier) {
        val scale=LocalDensity.current.fontScale
        val font=(maxWidth.value/(scale*(won(value).length*.62f+1.7f))).coerceIn(18f,36f).sp
        AnimatedDigits(value,Modifier.then(if(sharedId!=null)Modifier.traceShared("amount/$sharedId/$value")else Modifier),font)
    }
}

/** The original website's two paths. Only their transforms animate; geometry is untouched. */
@Composable fun WeaveMark(progress:Float,modifier:Modifier=Modifier) {
    val paths=remember{listOf("M6 9h28v7H23.5v17h-7V16H6z","M6 23h7v10H6z").map{PathParser().parsePathString(it).toPath()}}
    val coral=TraceColors.Coral
    Canvas(modifier.clearAndSetSemantics{}) {
        scale(size.width/40f,size.height/40f,pivot=Offset.Zero) {
            translate(top=(1-progress.coerceIn(0f,1f))*-3f){drawPath(paths[0],coral,alpha=progress.coerceIn(0f,1f))}
            translate(left=(1-progress.coerceIn(0f,1f))*-5f,top=(1-progress.coerceIn(0f,1f))*4f){drawPath(paths[1],coral,alpha=progress.coerceIn(0f,1f))}
        }
    }
}
@Composable fun ContextWeave(cancel:()->Unit) {
    val reduced=LocalReducedMotion.current
    val elapsed=remember{Animatable(0f)}
    LaunchedEffect(reduced){if(reduced)elapsed.snapTo(1290f)else elapsed.animateTo(1290f,tween(TraceMotion.Gate,easing=LinearEasing))}
    val t=elapsed.value
    val coral=TraceColors.Coral;val line=TraceColors.Divider
    TaskPage(tag="trace_evaluating",footer={SecondaryButton("송금 취소",onClick=cancel)}) {
        Spacer(Modifier.height((LocalTaskHeight.current.value*.18f).coerceIn(12f,100f).dp))
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.widthIn(max=310.dp).fillMaxWidth().height(168.dp),contentAlignment=Alignment.Center) {
                Canvas(Modifier.matchParentSize()) {
                    val center=Offset(size.width*.5f,size.height*.5f)
                    val starts=listOf(Offset(18.dp.toPx(),size.height*.2f),Offset(size.width-18.dp.toPx(),size.height*.24f),Offset(size.width*.24f,size.height*.9f))
                    val ends=listOf(Offset(center.x-32.dp.toPx(),center.y-4.dp.toPx()),Offset(center.x+32.dp.toPx(),center.y),Offset(center.x-5.dp.toPx(),center.y+36.dp.toPx()))
                    starts.indices.forEach { i ->
                        val start=starts[i];val end=ends[i]
                        val path=Path().apply {moveTo(start.x,start.y);cubicTo(start.x,center.y,end.x,start.y,end.x,end.y)}
                        drawPath(path,line.copy(alpha=.6f),style=Stroke(1.dp.toPx()))
                        val f=((t-listOf(240,580,930)[i])/260f).coerceIn(0f,1f)
                        val m=PathMeasure().apply{setPath(path,false)};val segment=Path();m.getSegment(0f,m.length*TraceMotion.Enter.transform(f),segment)
                        drawPath(segment,coral.copy(alpha=.8f),style=Stroke(1.7.dp.toPx(),cap=StrokeCap.Round))
                        drawCircle(if(f>=1f)coral else line,3.dp.toPx(),start)
                    }
                }
                WeaveMark(if(reduced)1f else (t/200f).coerceIn(0f,1f),Modifier.size(68.dp).traceShared("trace-mark"))
            }
            TaskGap(14)
            Text("송금 앞의 맥락을\n연결하고 있어요",style=MaterialTheme.typography.headlineSmall,textAlign=TextAlign.Center,
                modifier=Modifier.semantics{heading();liveRegion=LiveRegionMode.Polite})
            TaskGap(20)
            listOf("요청의 목적","최근 위험 신호","수취인과 거래").forEachIndexed { index,label ->
                val done=t>=listOf(240,580,930)[index]
                Row(Modifier.widthIn(max=240.dp).fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=if(done)TraceColors.Ink else TraceColors.Muted)
                    Icon(if(done)BankIcons.Check else BankIcons.More,null,Modifier.size(17.dp),tint=if(done)TraceColors.CoralText else line)
                }
            }
            TaskGap(14);Caption("아직 돈은 이동하지 않았어요.")
        }
    }
}
@Immutable data class ThreadNode(val time:String,val title:String)
@Composable fun ContextThread(nodes:List<ThreadNode>,key:String,compact:Boolean=false) {
    val reduced=LocalReducedMotion.current
    val reveal=remember(key){Animatable(0f)}
    LaunchedEffect(key,reduced){if(reduced)reveal.snapTo(1f)else reveal.animateTo(1f,tween(TraceMotion.Decision,easing=TraceMotion.Brake))}
    val coral=TraceColors.CoralText;val line=TraceColors.Divider
    Column(Modifier.fillMaxWidth().testTag("context_thread")) {
        nodes.forEachIndexed{index,node->
            Row(Modifier.fillMaxWidth().heightIn(min=if(compact)34.dp else 42.dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(node.time,Modifier.width(41.dp),style=MaterialTheme.typography.bodySmall,color=TraceColors.Muted)
                Canvas(Modifier.width(9.dp).height(if(compact)34.dp else 42.dp)) {
                    val x=size.width/2;val y=size.height/2
                    if(index>0)drawLine(line,Offset(x,0f),Offset(x,y),1.dp.toPx())
                    if(index<nodes.lastIndex)drawLine(line,Offset(x,y),Offset(x,y+(size.height-y)*reveal.value),1.dp.toPx())
                    drawCircle(if(index==nodes.lastIndex)coral else line,2.7.dp.toPx(),Offset(x,y))
                }
                Text(node.title,Modifier.weight(1f).graphicsLayer{alpha=.55f+.45f*reveal.value},style=MaterialTheme.typography.bodyMedium,
                    fontWeight=if(index==nodes.lastIndex)FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}
