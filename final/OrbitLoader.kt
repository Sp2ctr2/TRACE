package app.saeon.trace.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbitContextLoading(cancel: () -> Unit) {
    val reduced = LocalReducedMotion.current
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(240); stage = 1
        delay(340); stage = 2
        delay(350); stage = 3
    }
    val transition = rememberInfiniteTransition(label = "trace_orbit")
    val angle by transition.animateFloat(
        initialValue = -90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(tween(880, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit_angle"
    )
    val breath by transition.animateFloat(
        initialValue = .96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orbit_breath"
    )
    val coral = TraceColors.Coral
    val divider = TraceColors.Divider
    val muted = TraceColors.Muted

    TaskPage(tag = "trace_evaluating", footer = {
        SecondaryButton("송금 취소", onClick = cancel)
    }) {
        val compact = LocalTaskHeight.current < 520.dp
        Spacer(Modifier.height(if (compact) 12.dp else 42.dp))
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(if (compact) 118.dp else 148.dp).testTag("trace_orbit"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * .35f
                    drawCircle(divider.copy(alpha = .72f), radius, c, style = Stroke(1.4.dp.toPx()))
                    drawCircle(divider.copy(alpha = .28f), radius * 1.23f, c, style = Stroke(1.dp.toPx()))

                    val baseAngle = if (reduced) -35f else angle
                    repeat(7) { i ->
                        val a = Math.toRadians((baseAngle - i * 8f).toDouble())
                        val p = Offset(
                            c.x + cos(a).toFloat() * radius,
                            c.y + sin(a).toFloat() * radius
                        )
                        drawCircle(
                            coral.copy(alpha = (1f - i / 8f) * .72f),
                            (3.8f - i * .34f).coerceAtLeast(1.4f).dp.toPx(),
                            p
                        )
                    }
                    val a = Math.toRadians(baseAngle.toDouble())
                    val dot = Offset(c.x + cos(a).toFloat() * radius, c.y + sin(a).toFloat() * radius)
                    drawCircle(coral, 5.2.dp.toPx(), dot)
                }
                Box(Modifier.graphicsLayer {
                    val s = if (reduced) 1f else breath
                    scaleX = s; scaleY = s
                }) {
                    AppIcon(BankIcons.Trace, description = "TRACE", tint = coral, size = if (compact) 45 else 54)
                }
            }

            Space(if (compact) 16 else 24)
            Text(
                "송금 앞의 맥락을\n확인하고 있어요",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite }
            )
            Space(if (compact) 13 else 20)

            val labels = listOf("요청의 목적", "최근 위험 신호", "수취인과 거래")
            labels.forEachIndexed { index, label ->
                val done = stage > index
                Row(
                    Modifier.widthIn(max = 248.dp).fillMaxWidth().heightIn(min = 34.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (done) TraceColors.Ink else muted
                    )
                    AppIcon(
                        if (done) BankIcons.Check else BankIcons.More,
                        size = 17,
                        tint = if (done) TraceColors.CoralText else divider
                    )
                }
            }
            Space(if (compact) 10 else 16)
            Caption("아직 돈은 이동하지 않았어요.")
        }
    }
}
