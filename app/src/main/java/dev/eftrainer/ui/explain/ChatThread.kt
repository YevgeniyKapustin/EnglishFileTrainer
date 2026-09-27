package dev.eftrainer.ui.explain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.domain.ai.AiChat
import dev.eftrainer.ui.exercise.aiErrorRes

@Composable
fun ChatThread(
    chat: AiChat,
    canAsk: Boolean,
    onAsk: (String) -> Unit,
    onRetry: () -> Unit,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val small = T.label.copy(fontSize = 15.sp, lineHeight = 21.sp)
    val scroll = rememberScrollState()
    LaunchedEffect(chat.lines.size, chat.loading) { scroll.animateScrollTo(scroll.maxValue) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            chat.lines.forEach { line ->
                if (line.student) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            line.text,
                            style = T.body.copy(fontSize = 15.sp, lineHeight = 21.sp),
                            modifier =
                                Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp))
                                    .background(C.Field)
                                    .border(1.dp, C.Line, RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    Text(line.text, style = T.body.copy(fontSize = 15.sp, lineHeight = 22.sp))
                }
            }
            when {
                chat.loading -> {
                    Text(
                        stringResource(R.string.thinking),
                        style = small,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }

                chat.error != null -> {
                    Text(
                        stringResource(aiErrorRes(chat.error)),
                        style = small.copy(color = C.Bad),
                        modifier = Modifier.clickable(onClick = onRetry).padding(vertical = 4.dp),
                    )
                }
            }
        }
        val settled = !chat.loading && chat.error == null
        if (canAsk && chat.answered && settled) FollowUpField(onAsk)
    }
}

@Composable
private fun FollowUpField(onAsk: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val send = {
        if (text.isNotBlank()) {
            onAsk(text.trim())
            text = ""
        }
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(
                shape,
            ).background(C.Field)
            .border(1.dp, C.FieldBorder, shape)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = T.body.copy(fontSize = 15.sp),
            cursorBrush = SolidColor(C.Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        stringResource(R.string.ask_follow_up),
                        style = T.body.copy(fontSize = 15.sp, color = C.Muted),
                    )
                }
                inner()
            },
        )
        SendButton(enabled = text.isNotBlank(), onClick = send)
    }
}

@Composable
fun SendButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) C.Accent else C.Disabled)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(SendIcon, stringResource(R.string.send), Modifier.size(20.dp), tint = C.OnAccent)
    }
}

private val SendIcon: ImageVector =
    ImageVector
        .Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            addPathNodes("M12 19V5M6 11l6-6l6 6"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

@Composable
fun LinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Text(
        text,
        style =
            T.label.copy(
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = if (enabled) C.Accent else C.Off,
                fontWeight = FontWeight.SemiBold,
            ),
        modifier = modifier.clickable(enabled = enabled, onClick = onClick).padding(vertical = 4.dp),
    )
}
