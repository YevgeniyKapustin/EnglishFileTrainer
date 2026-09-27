package dev.eftrainer.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.eftrainer.R

/**
 * Тёмная ли сейчас тема. Это snapshot-состояние (как сигнал/стор во фронте):
 * всё, что прочитало цвет из [C] во время отрисовки, перерисуется при смене темы.
 * Выставляет [AppTheme]; выбор System/Light/Dark хранит ThemeSettings.
 */
object Palette {
    var dark by mutableStateOf(false)
}

private fun pick(light: Long, dark: Long) = Color(if (Palette.dark) dark else light)

/** Палитра из mockups/style.css (:root и html.dark). */
object C {
    val Paper get() = pick(0xFFF6F4EE, 0xFF1A1917)
    val Ink get() = pick(0xFF1C1B19, 0xFFECE9E2)
    val Muted get() = pick(0xFF6E6A61, 0xFF9A958B)
    val Line get() = pick(0xFFE3DFD5, 0xFF2E2C28)
    val Accent get() = pick(0xFF2F4BA0, 0xFF8FA6E8)
    val OnAccent get() = pick(0xFFFFFFFF, 0xFF14161F)
    val Ok get() = pick(0xFF1F6B43, 0xFF7CC89A)
    val OkBg get() = pick(0xFFE3F0E7, 0xFF1E3327)
    val Bad get() = pick(0xFFA3360F, 0xFFF09A76)
    val BadBg get() = pick(0xFFFBE8E0, 0xFF3A2119)
    val Disabled get() = pick(0xFFCFCABF, 0xFF3A3833)
    val Off get() = pick(0xFFA7A297, 0xFF5F5B53)
    val Chevron get() = pick(0xFFB5B0A5, 0xFF5F5B53)
    val FieldBorder get() = pick(0xFFC9C4B8, 0xFF45423C)
    val Field get() = pick(0xFFFFFFFF, 0xFF24231F)
}

/** Цвета учебника English File (--ef-* в mockups/style.css). */
object EF {
    val Teal = Color(0xFF12A3BF)
    val Sky = Color(0xFF7FCBE0)
    val Navy = Color(0xFF1D4E9E)
    val Lime = Color(0xFFA9D46F)
    val BubbleInk = Color(0xFF10384A)
    val File2 = Color(0xFFE8762B)
    val File3 = Color(0xFF4FA64A)
    /** Бирюзовый заголовок/подписи: на тёмном фоне — светлее. */
    val Title get() = if (Palette.dark) Sky else Teal
    val G get() = pick(0xFF1D4E9E, 0xFF6F95E0)
    val V get() = pick(0xFF3E9A3A, 0xFF79C06F)
    val P get() = pick(0xFFD02A78, 0xFFF07AAE)
    /** Текст на цветной плашке G/V/P. */
    val TagInk get() = pick(0xFFFFFFFF, 0xFF14161F)
    val Panel get() = pick(0xFFDDF0F6, 0xFF16323A)
    val Panel2 get() = pick(0xFFE6F2D6, 0xFF262420)
}

/** Шрифты вариативные: без явной оси wght Android рисует всё обычным начертанием. */
private fun weighted(res: Int, weight: FontWeight) =
    Font(res, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** Интерфейс — Golos Text; английский материал книги — Literata (как в макетах). */
val Golos = FontFamily(
    weighted(R.font.golos_text, FontWeight.Normal),
    weighted(R.font.golos_text, FontWeight.Medium),
    weighted(R.font.golos_text, FontWeight.SemiBold),
    weighted(R.font.golos_text, FontWeight.Bold),
)
val Literata = FontFamily(
    weighted(R.font.literata, FontWeight.Normal),
    weighted(R.font.literata, FontWeight.SemiBold),
)

/** Стили текста из макетов. Геттеры — чтобы цвет брался из текущей темы. */
object T {
    val label get() = TextStyle(fontFamily = Golos, fontSize = 13.sp, color = C.Muted)
    val title get() = TextStyle(fontFamily = Golos, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = C.Ink)
    val titleEn get() = TextStyle(fontFamily = Literata, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
    val body get() = TextStyle(fontFamily = Golos, fontSize = 16.sp, lineHeight = 24.sp, color = C.Ink)
    val row get() = TextStyle(fontFamily = Golos, fontSize = 17.sp, color = C.Ink)
    val rowEn get() = TextStyle(fontFamily = Literata, fontSize = 17.sp, color = C.Ink)
    val rowSub get() = TextStyle(fontFamily = Golos, fontSize = 13.sp, color = C.Muted)
    val question get() = TextStyle(fontFamily = Literata, fontSize = 22.sp, lineHeight = 36.sp, color = C.Ink)
    val field get() = TextStyle(fontFamily = Literata, fontSize = 20.sp, lineHeight = 28.sp, color = C.Ink)
    val hint get() = TextStyle(fontFamily = Golos, fontSize = 17.sp, color = C.Muted)
}

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    Palette.dark = dark
    val base = if (dark) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = base.copy(
            primary = C.Accent,
            onPrimary = C.OnAccent,
            background = C.Paper,
            surface = C.Paper,
            surfaceContainerHigh = C.Field,
            onBackground = C.Ink,
            onSurface = C.Ink,
            onSurfaceVariant = C.Muted,
        ),
        typography = Typography().run {
            copy(bodyLarge = bodyLarge.copy(fontFamily = Golos), bodyMedium = bodyMedium.copy(fontFamily = Golos))
        },
        content = content,
    )
}
