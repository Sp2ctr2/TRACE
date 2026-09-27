package app.saeon.trace.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** One launch transition per Activity state, never restarted on returning from authentication. */
@Composable fun StudioLaunch(ready:Boolean,content:@Composable ()->Unit) {
    var finished by rememberSaveable{mutableStateOf(false)}
    val reduced=LocalReducedMotion.current
    val mark=remember{Animatable(if(reduced)1f else 0f)}
    val opacity=remember{Animatable(1f)}
    LaunchedEffect(Unit){if(!reduced)mark.animateTo(1f,tween(520,easing=TraceMotion.Enter))}
    LaunchedEffect(ready,reduced){
        if(ready&&!finished){
            if(!reduced){delay((560-(mark.value*520).toLong()).coerceAtLeast(80));opacity.animateTo(0f,tween(200))}
            finished=true
        }
    }
    Box(Modifier.fillMaxSize()){
        content()
        if(!finished)Box(Modifier.fillMaxSize().graphicsLayer{alpha=opacity.value}.background(TraceColors.Paper).testTag("launch_splash")
            .semantics{contentDescription="새온은행을 준비하고 있어요";liveRegion=LiveRegionMode.Polite},contentAlignment=Alignment.Center){
            Column(horizontalAlignment=Alignment.CenterHorizontally){
                WeaveMark(mark.value,Modifier.size(88.dp))
                Space(24)
                Text("새온은행",style=MaterialTheme.typography.titleLarge,modifier=Modifier.graphicsLayer{alpha=mark.value})
                Space(7)
                Caption("Protected by TRACE",Modifier.graphicsLayer{alpha=mark.value})
            }
        }
    }
}
