package dev.eftrainer.ui.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Talk
import dev.eftrainer.data.content.needsAi
import dev.eftrainer.data.content.number
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.KindHeader
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.BottomBar
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.core.ui.PrimaryButton
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.label

@Composable
fun SectionScreen(
    sectionId: String,
    onBack: () -> Unit,
    onRules: (startTasks: Boolean) -> Unit,
    onStart: () -> Unit,
) {
    val container = appContainer()
    val vm = viewModel(key = sectionId) { SectionViewModel(container, sectionId) }
    val ui by vm.state.collectAsStateWithLifecycle()
    val state = ui ?: return
    val hasRules = state.section.reference.isNotEmpty()

    Column(Modifier.fillMaxSize()) {
        BackLink(state.chapterId, onBack)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KindHeader(state.section.kind, state.section.kind.label())
                Text(state.section.title, style = T.titleEn)
            }

            if (hasRules) {
                Column(Modifier.padding(top = 20.dp)) {
                    HorizontalDivider(color = C.Line)
                    ListRow(onClick = { onRules(!state.firstPassDone) }, divider = true) {
                        Text(stringResource(R.string.rules), style = T.row, modifier = Modifier.weight(1f))
                        Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
                    }
                }
            }

            // Задания сгруппированы по книгам и пронумерованы как в них: SB 2a, Grammar Bank b, WB 2a.
            state.tasks.forEachIndexed { i, row ->
                val task = row.task
                val last = state.tasks.getOrNull(i + 1)?.task?.origin != task.origin
                if (i == 0 || state.tasks[i - 1].task.origin != task.origin) {
                    Text(
                        when (task.origin) {
                            Origin.SB -> stringResource(R.string.book_sb)
                            Origin.SB_GRAMMAR_BANK -> stringResource(R.string.book_gb, state.chapterId)
                            Origin.WB -> stringResource(R.string.book_wb)
                            Origin.GENERATED -> stringResource(R.string.extra_practice)   // заготовки: 0 / N, пока книга не освоена
                        },
                        style = T.label,
                        modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
                    )
                }
                // Задание только с ИИ (разговор, письменный ответ) без ИИ недоступно — строка приглушена,
                // но видно, что это такое и как включить.
                val off = task.needsAi && !state.aiOn
                ListRow(onClick = null, divider = !last) {
                    Text(
                        task.number,
                        style = T.row.copy(color = if (off) C.Off else EF.Title, fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(30.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(task.instruction, style = T.rowEn.copy(fontSize = 16.sp, lineHeight = 22.sp, color = if (off) C.Off else T.rowEn.color))
                        if (task.needsAi) Text(
                            stringResource(
                                when {
                                    task.exercises.all { it is Talk } -> if (off) R.string.talk_needs_key else R.string.talk_with_ai
                                    else -> if (off) R.string.write_needs_key else R.string.write_with_ai
                                },
                            ),
                            style = T.rowSub.copy(color = if (off) C.Muted else EF.Title),
                        )
                    }
                    if (!off) Text(stringResource(R.string.n_of_m, row.mastered, row.total), style = T.label.copy(fontSize = 15.sp))
                }
            }
        }

        if (!state.firstPassDone) {
            BottomBar {
                PrimaryButton(
                    stringResource(if (state.started) R.string.continue_section else R.string.start_section),
                    onClick = { if (hasRules && !state.started) onRules(true) else onStart() },
                )
            }
        }
    }
}
