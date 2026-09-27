package dev.eftrainer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.Golos

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = C.Accent,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) color else C.Disabled)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style =
                TextStyle(
                    fontFamily = Golos,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.OnAccent,
                ),
        )
    }
}

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp)) { content() }
}

@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier.height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.Line)) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(3.dp))
                .background(C.Accent),
        )
    }
}

@Composable
fun ProgressBar(
    fraction: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.height(6.dp).clip(RoundedCornerShape(3.dp)).background(C.Line).drawBehind {
            val filled = size.width * fraction().coerceIn(0f, 1f)
            if (filled > 0f) {
                drawRoundRect(C.Accent, size = Size(filled, size.height), cornerRadius = CornerRadius(size.height / 2))
            }
        },
    )
}

@Composable
fun BackLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = C.Muted,
    closesRight: Boolean = false,
) {
    Row(
        modifier.height(56.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .heightIn(
                    min = 44.dp,
                ).clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!closesRight) Icon(Icons.ChevronLeft, null, Modifier.size(18.dp), tint = tint)
            Text(text, style = TextStyle(fontFamily = Golos, fontSize = 15.sp, color = tint))
            if (closesRight) Icon(Icons.ChevronRight, null, Modifier.size(18.dp), tint = tint)
        }
    }
}

@Composable
fun IconButton24(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = C.Ink,
    size: Int = 22,
) {
    Box(
        modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, Modifier.size(size.dp), tint = tint) }
}

@Composable
fun ListRow(
    onClick: (() -> Unit)?,
    divider: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .heightIn(min = 64.dp)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
        if (divider) HorizontalDivider(color = C.Line)
    }
}
