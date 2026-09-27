package dev.eftrainer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.eftrainer.App
import dev.eftrainer.AppContainer
import dev.eftrainer.R
import dev.eftrainer.data.content.SectionKind

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as App).container

@Composable
fun SectionKind.label(): String = stringResource(
    when (this) {
        SectionKind.GRAMMAR -> R.string.kind_grammar
        SectionKind.VOCABULARY -> R.string.kind_vocabulary
        SectionKind.PRONUNCIATION -> R.string.kind_pronunciation
        SectionKind.SKILLS -> R.string.kind_skills
        SectionKind.PRACTICAL_ENGLISH -> R.string.kind_practical_english
    },
)

/** Доля 0..1 → целые проценты вниз: 89.9% не должно выглядеть как 90%. */
fun Double.toPercent(): Int = (this * 100).toInt()
