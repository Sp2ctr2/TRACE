package app.saeon.trace.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import app.saeon.trace.ui.design.BankIcons

private val presentationIcon = ImageVector.Builder("Presentation", 24.dp, 24.dp, 24f, 24f)
    .addPath(PathParser().parsePathString("M8 4L20 12L8 20Z").toNodes(), fill = SolidColor(Color.Black))
    .build()

/** App-only presentation destination; this vector is not shipped in the bank UI library. */
val BankIcons.Play: ImageVector get() = presentationIcon
