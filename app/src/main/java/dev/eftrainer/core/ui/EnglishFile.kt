package dev.eftrainer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.data.content.SectionKind

@Composable
fun Bubble(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

val bubbleText =
    TextStyle(
        fontFamily = Golos,
        fontSize = 12.sp,
        lineHeight = 14.4.sp,
        fontWeight = FontWeight.Medium,
        color = EF.BubbleInk,
        textAlign = TextAlign.Center,
    )

val SectionKind.color: Color
    get() =
        when (this) {
            SectionKind.GRAMMAR -> EF.G
            SectionKind.VOCABULARY -> EF.V
            SectionKind.PRONUNCIATION -> EF.P
            SectionKind.SKILLS, SectionKind.PRACTICAL_ENGLISH, SectionKind.REVIEW -> EF.Title
        }

private val SectionKind.letter: String
    get() =
        when (this) {
            SectionKind.GRAMMAR -> "G"
            SectionKind.VOCABULARY -> "V"
            SectionKind.PRONUNCIATION -> "P"
            SectionKind.SKILLS -> "LS"
            SectionKind.PRACTICAL_ENGLISH -> "PE"
            SectionKind.REVIEW -> "R"
        }

@Composable
fun SectionTag(
    kind: SectionKind,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(22.dp)
            .widthIn(min = 22.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(kind.color)
            .padding(horizontal = if (kind.letter.length > 1) 3.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            kind.letter,
            style =
                TextStyle(
                    fontFamily = Golos,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EF.TagInk,
                ),
        )
    }
}

@Composable
fun KindLabel(
    text: String,
    kind: SectionKind,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(),
        style =
            TextStyle(
                fontFamily = Golos,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .06.em,
                color = kind.color,
            ),
        modifier = modifier,
    )
}

@Composable
fun KindHeader(
    kind: SectionKind,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTag(kind)
        KindLabel(text, kind)
    }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    alt: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (alt) EF.Panel2 else EF.Panel)
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        content = content,
    )
}

@Composable
fun PanelHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(),
        style =
            TextStyle(
                fontFamily = Golos,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .06.em,
                color = EF.Title,
            ),
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
fun NotePanel(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .border(1.5.dp, EF.Title, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(EfIcons.Magnifier, null, Modifier.size(16.dp), tint = EF.Title)
                Text(
                    title,
                    style =
                        TextStyle(
                            fontFamily = Golos,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EF.Title,
                        ),
                )
            }
        }
        content()
    }
}

object EfIcons {
    val Magnifier = stroke("M4 11a7 7 0 1 0 14 0a7 7 0 1 0 -14 0M20 20l-4 -4", 2.2f)
    val Gear =
        stroke(
            "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0" +
                "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06" +
                "a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09" +
                "A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83" +
                "l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09" +
                "A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83" +
                "l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09" +
                "a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83" +
                "l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09" +
                "a1.65 1.65 0 0 0-1.51 1z",
            1.8f,
        )

    private fun stroke(
        d: String,
        width: Float,
    ) = ImageVector
        .Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            pathData = addPathNodes(d),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
}
