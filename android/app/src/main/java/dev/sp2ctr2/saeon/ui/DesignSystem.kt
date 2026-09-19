package dev.sp2ctr2.saeon.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import dev.sp2ctr2.saeon.data.AppOptions
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TraceColors {
    val Paper=Color(0xFFF5F4F0)
    val Surface=Color(0xFFFBFAF7)
    val Ink=Color(0xFF20211F)
    val Muted=Color(0xFF686A63)
    val Faint=Color(0xFF7D7E77)
    val Line=Color(0xFFDDDDD5)
    val Coral=Color(0xFFEF4A32)
    val Action=Color(0xFFD93B25)
    val Soft=Color(0xFFFAE7DF)
    val White=Color.White
}
val LocalEasy=staticCompositionLocalOf { false }
val LocalReducedMotion=staticCompositionLocalOf { false }
@Composable fun TraceTheme(options: AppOptions, content: @Composable ()->Unit) {
    val base=TextStyle(fontFamily=FontFamily.SansSerif, color=TraceColors.Ink, platformStyle=PlatformTextStyle(includeFontPadding=false), lineBreak=LineBreak.Paragraph)
    MaterialTheme(
        colorScheme=lightColorScheme(primary=TraceColors.Action,onPrimary=Color.White,secondary=TraceColors.Ink,background=TraceColors.Paper,surface=TraceColors.Surface,onSurface=TraceColors.Ink,onSurfaceVariant=TraceColors.Muted,outline=TraceColors.Line,surfaceVariant=TraceColors.Paper,error=TraceColors.Action),
        typography=Typography(
            bodyLarge=base.copy(fontSize=16.sp,lineHeight=25.sp),bodyMedium=base.copy(fontSize=15.sp,lineHeight=23.sp),
            bodySmall=base.copy(fontSize=13.sp,lineHeight=20.sp),titleLarge=base.copy(fontSize=23.sp,lineHeight=31.sp,fontWeight=FontWeight.SemiBold),
            titleMedium=base.copy(fontSize=17.sp,lineHeight=25.sp,fontWeight=FontWeight.SemiBold),
            labelLarge=base.copy(fontSize=16.sp,lineHeight=23.sp,fontWeight=FontWeight.SemiBold)
        ),
        shapes=Shapes(small=RoundedCornerShape(8.dp),medium=RoundedCornerShape(12.dp),large=RoundedCornerShape(20.dp))
    ) { CompositionLocalProvider(LocalEasy provides options.easy,LocalReducedMotion provides options.reducedMotion) { content() } }
}
fun money(value: Long): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(value)
fun dateLabel(at: Long, pattern: String="MM.dd HH:mm"): String = DateTimeFormatter.ofPattern(pattern, Locale.KOREA).withZone(ZoneId.of("Asia/Seoul")).format(Instant.ofEpochMilli(at))
@Composable fun Gap(value: Int=16) { Spacer(Modifier.height(value.dp)) }
@Composable fun Copy(text: String, modifier: Modifier=Modifier, size: Int=15, color: Color=TraceColors.Muted, weight: FontWeight=FontWeight.Normal, align: TextAlign=TextAlign.Start) {
    val actual=if(LocalEasy.current && size in 14..18) 19 else size
    Text(text,modifier,color=color,fontSize=actual.sp,lineHeight=(actual*1.55).sp,fontWeight=weight,textAlign=align)
}
@Composable fun Title(text: String, size: Int=29, modifier: Modifier=Modifier) {
    Text(text,modifier.semantics { heading() },fontSize=(if(LocalEasy.current) maxOf(size,30) else size).sp,lineHeight=(size*1.3).sp,fontWeight=FontWeight.Bold,letterSpacing=(-0.5).sp,color=TraceColors.Ink,style=MaterialTheme.typography.titleLarge.copy(lineBreak=LineBreak.Heading))
}
@Composable fun Eyebrow(text: String, coral: Boolean=false) { Copy(text,size=12,color=if(coral) TraceColors.Action else TraceColors.Muted,weight=FontWeight.Medium) }
@Composable fun Amount(value: Long, tag: String="", size: Int=39, hidden: Boolean=false) {
    val scaled=if(LocalDensity.current.fontScale>1.5f) minOf(size,25) else size
    val text=if(hidden) "잔액 숨김" else money(value)
    Text(buildAnnotatedString {
        withStyle(SpanStyle(fontWeight=FontWeight.Bold,fontSize=scaled.sp,letterSpacing=(-1).sp)){ append(text) }
        if(!hidden) withStyle(SpanStyle(fontWeight=FontWeight.Medium,fontSize=(scaled*0.52).sp)){ append("원") }
    },Modifier.testTag(tag).semantics { contentDescription=if(hidden) "잔액 숨김" else "${money(value)}원" },color=TraceColors.Ink,lineHeight=(scaled*1.25).sp)
}
@Composable fun TraceMark(modifier: Modifier=Modifier.size(23.dp), color: Color=TraceColors.Coral) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val u=size.width/24
        drawRect(color,Offset(3*u,3*u),Size(19*u,4*u))
        drawRect(color,Offset(12*u,6*u),Size(4*u,15*u))
        drawRect(color,Offset(3*u,11*u),Size(5*u,5*u))
    }
}
private val iconPaths=mapOf(
    "back" to "M15 5L8 12L15 19", "chevron" to "M9 6L15 12L9 18", "close" to "M6 6L18 18M18 6L6 18",
    "home" to "M3 10L12 3L21 10V21H15V14H9V21H3Z",
    "assets" to "M4 8H20V20H4ZM7 8V4H17V8M4 12H20M10 15H14",
    "transfer" to "M3 7H20M15 2L20 7L15 12M21 17H4M9 12L4 17L9 22",
    "shield" to "M12 3L20 6V12C20 17 16 20 12 22C8 20 4 17 4 12V6Z",
    "menu" to "M4 6H20M4 12H20M4 18H20",
    "bell" to "M6 16V10C6 2 18 2 18 10V16L20 18H4ZM10 21H14",
    "search" to "M10.5 3A7.5 7.5 0 1 0 10.5 18A7.5 7.5 0 1 0 10.5 3M16 16L22 22",
    "receipt" to "M6 3H18V22L15 20L12 22L9 20L6 22ZM9 8H15M9 12H15M9 16H13",
    "bank" to "M3 8L12 3L21 8ZM5 11V18M10 11V18M15 11V18M20 11V18M3 21H22",
    "check" to "M5 12L10 17L20 6", "pause" to "M8 4V20M16 4V20",
    "lock" to "M5 10H19V21H5ZM8 10V7C8 1 16 1 16 7V10M12 14V17",
    "message" to "M3 4H21V17H10L4 21V17H3ZM7 8H17M7 12H14",
    "link" to "M10 7L12 5C18 -1 25 6 19 12L16 15M14 17L12 19C6 25 -1 18 5 12L8 9M8 16L16 8",
    "arrow" to "M4 12H20M14 6L20 12L14 18", "plus" to "M12 4V20M4 12H20",
    "delete" to "M9 5H21V19H9L2 12ZM12 9L18 15M18 9L12 15",
    "edit" to "M4 16L16 4L20 8L8 20H4ZM14 6L18 10",
    "profile" to "M12 3A4 4 0 1 0 12 11A4 4 0 1 0 12 3M4 21C4 12 20 12 20 21",
    "clock" to "M12 2A10 10 0 1 0 12 22A10 10 0 1 0 12 2M12 6V12L16 15",
    "eye" to "M2 12C7 3 17 3 22 12C17 21 7 21 2 12ZM12 9A3 3 0 1 0 12 15A3 3 0 1 0 12 9",
    "phone" to "M5 3H9L11 8L8 10C9 13 11 15 14 16L16 13L21 15V20C10 25 -1 12 5 3Z",
    "info" to "M12 2A10 10 0 1 0 12 22A10 10 0 1 0 12 2M12 10V17M12 6V6.5",
    "card" to "M3 5H21V20H3ZM3 10H21M6 16H10",
    "filter" to "M4 6H20M7 12H17M10 18H14"
)
private val vectorCache=mutableMapOf<String,ImageVector>()
@Composable fun Glyph(name: String, color: Color=TraceColors.Ink, size: Int=22, description: String?=null) {
    val vector=remember(name) { vectorCache.getOrPut(name) {
        ImageVector.Builder(name,24.dp,24.dp,24f,24f).addPath(
            pathData=PathParser().parsePathString(iconPaths[name] ?: iconPaths.getValue("info")).toNodes(),
            fill=null,stroke=SolidColor(Color.Black),strokeLineWidth=1.75f,strokeLineCap=StrokeCap.Round,strokeLineJoin=StrokeJoin.Round
        ).build()
    } }
    Icon(vector,description,Modifier.size(size.dp),tint=color)
}
@Composable fun GlyphButton(name: String, label: String, tag: String="", onClick: ()->Unit) {
    IconButton(onClick,Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).testTag(tag)) { Glyph(name,description=label) }
}
@Composable fun Primary(label: String, tag: String="", enabled: Boolean=true, neutral: Boolean=false, onClick: ()->Unit) {
    Button(onClick,Modifier.fillMaxWidth().heightIn(min=56.dp).testTag(tag),enabled=enabled,shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(horizontal=18.dp,vertical=16.dp),colors=ButtonDefaults.buttonColors(containerColor=if(neutral) TraceColors.Ink else TraceColors.Action,contentColor=TraceColors.White,disabledContainerColor=TraceColors.Line,disabledContentColor=TraceColors.Muted),elevation=null) {
        Text(label,fontSize=(if(LocalEasy.current) 19 else 16).sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
    }
}
@Composable fun Secondary(label: String, tag: String="", onClick: ()->Unit) {
    TextButton(onClick,Modifier.fillMaxWidth().heightIn(min=48.dp).testTag(tag),colors=ButtonDefaults.textButtonColors(contentColor=TraceColors.Muted)) { Text(label,fontSize=15.sp,textAlign=TextAlign.Center) }
}
@Composable fun Rule() { HorizontalDivider(thickness=1.dp,color=TraceColors.Line) }
@Composable fun SectionHeading(title: String, action: String?=null, onAction: ()->Unit={}) {
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(title,Modifier.weight(1f).semantics { heading() },fontSize=18.sp,fontWeight=FontWeight.SemiBold)
        if(action!=null) TextButton(onAction,colors=ButtonDefaults.textButtonColors(contentColor=TraceColors.Muted)) { Text(action,fontSize=13.sp) }
    }
}
@Composable fun DetailRow(label: String, value: String, accent: Boolean=false) {
    Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
        Copy(label,Modifier.weight(1f),size=14)
        Copy(value,Modifier.weight(1.7f),size=14,color=if(accent) TraceColors.Action else TraceColors.Ink,weight=FontWeight.Medium,align=TextAlign.End)
    }
}
@Composable fun ListRow(title: String, subtitle: String?=null, value: String?=null, icon: String?=null, tag: String="", accent: Boolean=false, onClick: (() -> Unit)?=null) {
    val large=LocalDensity.current.fontScale>1.3f
    Row(Modifier.fillMaxWidth().heightIn(min=72.dp).then(if(onClick!=null) Modifier.clickable(role=Role.Button,onClick=onClick) else Modifier).testTag(tag).padding(vertical=15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        if(icon!=null) Glyph(icon,color=if(accent) TraceColors.Action else TraceColors.Muted)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Copy(title,size=16,color=TraceColors.Ink,weight=FontWeight.Medium)
            if(subtitle!=null) Copy(subtitle,size=12)
            if(large && value!=null) Copy(value,size=16,color=if(accent) TraceColors.Action else TraceColors.Ink,weight=FontWeight.SemiBold)
        }
        if(!large && value!=null) Copy(value,size=16,color=if(accent) TraceColors.Action else TraceColors.Ink,weight=FontWeight.SemiBold)
        if(onClick!=null) Glyph("chevron",TraceColors.Faint,16)
    }
}
@Composable fun QuietNotice(text: String, icon: String="info", accent: Boolean=false) {
    Row(Modifier.fillMaxWidth().background(if(accent) TraceColors.Soft else TraceColors.Paper,RoundedCornerShape(12.dp)).padding(16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Top) {
        Glyph(icon,if(accent) TraceColors.Action else TraceColors.Muted,18)
        Copy(text,size=13,color=if(accent) TraceColors.Action else TraceColors.Muted)
    }
}
@Composable fun BankBar(title: String, onBack: (() -> Unit)?=null, brand: Boolean=false, actions: @Composable RowScope.()->Unit={}) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).padding(horizontal=if(onBack!=null) 10.dp else 24.dp),verticalAlignment=Alignment.CenterVertically) {
        if(onBack!=null) GlyphButton("back","뒤로","back",onBack)
        if(brand) { TraceMark(Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)) }
        Text(title,Modifier.weight(1f),fontSize=if(brand) 22.sp else 17.sp,fontWeight=FontWeight.SemiBold)
        actions()
    }
}
@Composable fun Screen(title: String, tag: String="", onBack: (() -> Unit)?=null, brand: Boolean=false, actions: @Composable RowScope.()->Unit={}, footer: (@Composable ColumnScope.()->Unit)?=null, content: @Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().background(TraceColors.Surface).testTag(tag)) {
        BankBar(title,onBack,brand,actions)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start=24.dp,end=24.dp,top=20.dp,bottom=24.dp),content=content)
        if(footer!=null) Column(Modifier.fillMaxWidth().background(TraceColors.Surface).padding(horizontal=24.dp,vertical=12.dp),content=footer)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TraceSheet(title: String, onDismiss: ()->Unit, tag: String="", content: @Composable ColumnScope.()->Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=TraceColors.Surface,scrimColor=TraceColors.Ink.copy(alpha=.32f),shape=RoundedCornerShape(topStart=26.dp,topEnd=26.dp),dragHandle={ Box(Modifier.padding(top=10.dp,bottom=8.dp).size(32.dp,3.dp).background(TraceColors.Line,RoundedCornerShape(2.dp))) }) {
        Column(Modifier.fillMaxWidth().testTag(tag).verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=24.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) { Title(title,24,Modifier.weight(1f)); GlyphButton("close","닫기",onClick=onDismiss) }
            Gap(12); content()
        }
    }
}
@Composable fun LabeledField(value: String, onChange: (String)->Unit, label: String, tag: String="", singleLine: Boolean=true, error: Boolean=false) {
    OutlinedTextField(value,onChange,Modifier.fillMaxWidth().testTag(tag),label={ Text(label) },singleLine=singleLine,minLines=if(singleLine) 1 else 5,maxLines=if(singleLine) 1 else 10,isError=error,shape=RoundedCornerShape(12.dp),textStyle=MaterialTheme.typography.bodyLarge,colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=TraceColors.Action,unfocusedBorderColor=TraceColors.Line,focusedLabelColor=TraceColors.Action,unfocusedContainerColor=TraceColors.Surface,focusedContainerColor=TraceColors.Surface))
}
