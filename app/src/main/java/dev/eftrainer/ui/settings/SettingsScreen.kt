package dev.eftrainer.ui.settings

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.AppContainer
import dev.eftrainer.R
import dev.eftrainer.core.speech.groupVoices
import dev.eftrainer.core.speech.rememberTextToSpeech
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.Golos
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.data.ai.Provider
import dev.eftrainer.data.ai.detectProvider
import dev.eftrainer.data.settings.SpeechRate
import dev.eftrainer.data.settings.ThemeMode
import dev.eftrainer.ui.exercise.inputs.SpeakerIcon
import dev.eftrainer.ui.appContainer
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val theme = container.theme.mode
    val key = container.ai.key

    var checking by mutableStateOf(false)
        private set
    /** Почему ключ не подошёл; null — ошибки нет. */
    var error by mutableStateOf<AiException.Reason?>(null)

    fun setTheme(mode: ThemeMode) = container.theme.set(mode)

    val voice = container.voice

    /** Проверяет ключ коротким запросом и только потом сохраняет. */
    fun save(value: String, onSaved: () -> Unit) {
        if (checking) return
        checking = true
        error = null
        viewModelScope.launch {
            try {
                container.aiClient.verify(value.trim())
                container.ai.setKey(value)
                onSaved()
            } catch (e: AiException) {
                error = e.reason
            } finally {
                checking = false
            }
        }
    }

    fun remove() = container.ai.setKey(null)
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onVoice: () -> Unit) {
    val container = appContainer()
    val vm = viewModel { SettingsViewModel(container) }
    val mode by vm.theme.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        BackLink(stringResource(R.string.back), onBack)
        Text(stringResource(R.string.settings), style = T.title, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            SectionLabel(stringResource(R.string.theme))
            val options = listOf(
                Triple(ThemeMode.SYSTEM, R.string.theme_system, EF.Teal to Icons.HalfCircle),
                Triple(ThemeMode.LIGHT, R.string.theme_light, Color(0xFFF2A93B) to Icons.Sun),
                Triple(ThemeMode.DARK, R.string.theme_dark, EF.Navy to Icons.Moon),
            )
            options.forEachIndexed { i, (option, title, tile) ->
                ListRow(onClick = { vm.setTheme(option) }, divider = i != options.lastIndex) {
                    Tile(tile.first, tile.second)
                    Text(stringResource(title), style = T.row, modifier = Modifier.weight(1f))
                    Icon(Icons.Check, null, Modifier.size(20.dp).alpha(if (option == mode) 1f else 0f), tint = EF.Teal)
                }
            }
            SectionLabel(stringResource(R.string.voice))
            Voice(vm, onVoice)
            SectionLabel(stringResource(R.string.ai_explanations))
            AiKey(vm)
        }
    }
}

/** Подпись секции капсом (.sec.kind). */
@Composable
internal fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = TextStyle(fontFamily = Golos, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em, color = EF.Title),
        modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
    )
}

/**
 * Озвучка реплик ИИ в разговоре: выключатель, а при включённом — громкость и темп.
 * Выключено — реплики сами не читаются, динамик у реплики работает.
 * Строка «Voice» (выбор голоса) — всегда, если в телефоне есть синтезатор.
 */
@Composable
private fun Voice(vm: SettingsViewModel, onVoice: () -> Unit) {
    val on by vm.voice.readAloud.collectAsStateWithLifecycle()
    val volume by vm.voice.volume.collectAsStateWithLifecycle()
    val rate by vm.voice.rate.collectAsStateWithLifecycle()
    val name by vm.voice.voiceName.collectAsStateWithLifecycle()
    val tts = rememberTextToSpeech()

    ListRow(onClick = { vm.voice.setReadAloud(!on) }, divider = on || tts.available) {
        Tile(EF.File2, SpeakerIcon)
        Text(stringResource(R.string.voice_read_aloud), style = T.row, modifier = Modifier.weight(1f))
        Switch(
            checked = on,
            onCheckedChange = vm.voice::setReadAloud,
            colors = SwitchDefaults.colors(
                checkedThumbColor = C.OnAccent,
                checkedTrackColor = C.Accent,
                uncheckedThumbColor = C.Off,
                uncheckedTrackColor = C.Field,
                uncheckedBorderColor = C.FieldBorder,
            ),
        )
    }
    if (on) {
    ListRow(onClick = null, divider = true) {
        Text(stringResource(R.string.voice_volume), style = T.row)
        Slider(
            value = volume,
            onValueChange = vm.voice::setVolume,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = C.Accent, activeTrackColor = C.Accent, inactiveTrackColor = C.Line),
        )
        Text("${(volume * 100).toInt()}%", style = T.rowSub, modifier = Modifier.widthIn(min = 40.dp))
    }
    ListRow(onClick = null, divider = tts.available) {
        Text(stringResource(R.string.voice_speed), style = T.row, modifier = Modifier.weight(1f))
        Segmented(
            listOf(SpeechRate.NORMAL to R.string.voice_speed_normal, SpeechRate.SLOW to R.string.voice_speed_slow),
            selected = rate,
            onSelect = vm.voice::setRate,
        )
    }
    }
    if (!tts.available) return
    val current = name?.let { n -> groupVoices(tts.voices).flatMap { it.voices }.firstOrNull { it.name == n } }
    ListRow(onClick = onVoice, divider = false) {
        Text(stringResource(R.string.voice), style = T.row, modifier = Modifier.weight(1f))
        Text(
            current?.let { stringResource(R.string.voice_current, it.accent, it.number) } ?: stringResource(R.string.voice_auto),
            style = T.rowSub,
        )
        Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
    }
}

/** Переключатель из двух-трёх вариантов в одну строку. */
@Composable
private fun <V> Segmented(options: List<Pair<V, Int>>, selected: V, onSelect: (V) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(C.Field).border(1.dp, C.FieldBorder, RoundedCornerShape(10.dp)).padding(3.dp),
    ) {
        options.forEach { (value, title) ->
            val on = value == selected
            Text(
                stringResource(title),
                style = TextStyle(
                    fontFamily = Golos,
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) C.OnAccent else C.Muted,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) C.Accent else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

/** Цветная плитка 34×34 с белой иконкой. */
@Composable
private fun Tile(color: Color, icon: ImageVector) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(color), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(18.dp), tint = Color.White)
    }
}

/**
 * Строка «AI key» и раскрывающийся под ней ввод ключа.
 * Поставщик узнаётся по виду ключа, перед сохранением ключ проверяется запросом.
 */
@Composable
private fun AiKey(vm: SettingsViewModel) {
    val key by vm.key.collectAsStateWithLifecycle()
    val saved = detectProvider(key)
    var open by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }

    ListRow(
        onClick = { open = !open; input = ""; vm.error = null },
        divider = false,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(C.Field).border(1.dp, C.Line, RoundedCornerShape(9.dp)),
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
    if (!open) return

    val typed = input.trim()
    val hit = detectProvider(typed)
    val unknown = typed.isNotEmpty() && hit == null
    Column(Modifier.padding(top = 4.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KeyField(input) { input = it; vm.error = null }
        Text(
            when {
                unknown -> stringResource(R.string.ai_key_unknown)
                hit != null -> stringResource(R.string.ai_key_of, hit.title)
                else -> stringResource(R.string.ai_key_any)
            },
            style = T.label.copy(color = if (unknown) C.Bad else C.Muted),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Provider.entries.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProviderLogo(p, 18)
                    Text(
                        p.title,
                        style = TextStyle(
                            fontFamily = Golos,
                            fontSize = 14.sp,
                            fontWeight = if (p == hit) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (p == hit) C.Ink else C.Muted,
                        ),
                    )
                }
            }
        }
        Text(stringResource(R.string.ai_key_local), style = T.label)
        vm.error?.let { reason ->
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
        PrimaryButton(
            stringResource(if (vm.checking) R.string.ai_key_checking else R.string.save),
            onClick = { vm.save(typed) { open = false; input = "" } },
            enabled = hit != null && !vm.checking,
        )
        if (key != null) {
            Text(
                stringResource(R.string.ai_key_remove),
                style = TextStyle(fontFamily = Golos, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.Bad),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { vm.remove(); open = false; input = "" }
                    .padding(vertical = 6.dp),
            )
        }
    }
}

/** Поле ключа (.field): без автозамены и заглавных — ключ вставляют как есть. */
@Composable
private fun KeyField(value: String, onChange: (String) -> Unit) {
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
        keyboardOptions = KeyboardOptions(
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
                if (value.isEmpty()) Text(stringResource(R.string.ai_key_placeholder), style = style.copy(color = C.Muted))
                inner()
            }
        },
    )
}

/** Логотип поставщика (LobeHub Icons). Одноцветные — OpenAI и OpenRouter — красим цветом текста. */
@Composable
private fun ProviderLogo(p: Provider, size: Int) {
    val m = Modifier.size(size.dp)
    when (p) {
        Provider.OPENAI -> Icon(painterResource(R.drawable.ic_prov_openai), null, m, tint = C.Ink)
        Provider.OPENROUTER -> Icon(painterResource(R.drawable.ic_prov_openrouter), null, m, tint = C.Ink)
        Provider.DEEPSEEK -> Image(painterResource(R.drawable.ic_prov_deepseek), null, m)
        Provider.GEMINI -> Image(painterResource(R.drawable.ic_prov_gemini), null, m)
        Provider.CLAUDE -> Image(painterResource(R.drawable.ic_prov_claude), null, m)
    }
}
