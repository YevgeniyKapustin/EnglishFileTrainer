package dev.eftrainer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.data.ai.Provider
import dev.eftrainer.data.ai.detectProvider

@Composable
internal fun AiKey(
    key: String?,
    checking: Boolean,
    error: AiException.Reason?,
    onEdit: () -> Unit,
    onSave: (String, () -> Unit) -> Unit,
    onRemove: () -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    val close = {
        open = false
        input = ""
    }

    AiKeyRow(key) {
        open = !open
        input = ""
        onEdit()
    }
    AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val typed = input.trim()
        val hit = detectProvider(typed)
        Column(Modifier.padding(top = 4.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KeyField(input) {
                input = it
                onEdit()
            }
            KeyHint(typed, hit)
            ProviderList(hit)
            Text(stringResource(R.string.ai_key_local), style = T.label)
            error?.let { KeyError(it) }
            PrimaryButton(
                stringResource(if (checking) R.string.ai_key_checking else R.string.save),
                onClick = { onSave(typed, close) },
                enabled = hit != null && !checking,
            )
            if (key != null) {
                RemoveKeyLink {
                    onRemove()
                    close()
                }
            }
        }
    }
}

@Composable
private fun AiKeyRow(
    key: String?,
    onClick: () -> Unit,
) {
    val saved = detectProvider(key)
    ListRow(onClick = onClick, divider = false) {
        Box(
            Modifier
                .size(
                    34.dp,
                ).clip(RoundedCornerShape(9.dp))
                .background(C.Field)
                .border(1.dp, C.Line, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (saved != null) ProviderLogo(saved, 20) else Icon(Icons.Key, null, Modifier.size(20.dp), tint = C.Ink)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ai_key), style = T.row)
            Text(
                key?.let { stringResource(R.string.ai_key_state, saved?.title ?: "API", it.takeLast(4)) }
                    ?: stringResource(R.string.ai_key_not_set),
                style = T.rowSub,
            )
        }
        Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
    }
}

@Composable
private fun KeyHint(
    typed: String,
    hit: Provider?,
) {
    val unknown = typed.isNotEmpty() && hit == null
    Text(
        when {
            unknown -> stringResource(R.string.ai_key_unknown)
            hit != null -> stringResource(R.string.ai_key_of, hit.title)
            else -> stringResource(R.string.ai_key_any)
        },
        style = T.label.copy(color = if (unknown) C.Bad else C.Muted),
    )
}

@Composable
private fun ProviderList(hit: Provider?) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Provider.entries.forEach { p ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ProviderLogo(p, 18)
                Text(
                    p.title,
                    style =
                        TextStyle(
                            fontFamily = Golos,
                            fontSize = 14.sp,
                            fontWeight = if (p == hit) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (p == hit) C.Ink else C.Muted,
                        ),
                )
            }
        }
    }
}

@Composable
private fun KeyError(reason: AiException.Reason) {
    Text(
        stringResource(
            when (reason) {
                AiException.Reason.NETWORK -> R.string.ai_key_offline
                AiException.Reason.SERVER -> R.string.ai_key_failed
                else -> R.string.ai_key_bad
            },
        ),
        style = T.label.copy(color = C.Bad),
    )
}

@Composable
private fun RemoveKeyLink(onClick: () -> Unit) {
    Text(
        stringResource(R.string.ai_key_remove),
        style = TextStyle(fontFamily = Golos, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.Bad),
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
    )
}

@Composable
private fun KeyField(
    value: String,
    onChange: (String) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val style = TextStyle(fontFamily = Golos, fontSize = 16.sp, color = C.Ink)
    BasicTextField(
        value,
        onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(C.Accent),
        interactionSource = interaction,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
            ),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.Field)
                    .border(1.5.dp, if (focused) C.Accent else C.FieldBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        stringResource(R.string.ai_key_placeholder),
                        style = style.copy(color = C.Muted),
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun ProviderLogo(
    p: Provider,
    size: Int,
) {
    val m = Modifier.size(size.dp)
    when (p) {
        Provider.OPENAI -> Icon(painterResource(R.drawable.ic_prov_openai), null, m, tint = C.Ink)
        Provider.OPENROUTER -> Icon(painterResource(R.drawable.ic_prov_openrouter), null, m, tint = C.Ink)
        Provider.DEEPSEEK -> Image(painterResource(R.drawable.ic_prov_deepseek), null, m)
        Provider.GEMINI -> Image(painterResource(R.drawable.ic_prov_gemini), null, m)
        Provider.CLAUDE -> Image(painterResource(R.drawable.ic_prov_claude), null, m)
    }
}
