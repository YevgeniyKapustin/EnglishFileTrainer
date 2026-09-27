package dev.eftrainer.ui.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T
import dev.eftrainer.core.ui.BackLink
import dev.eftrainer.core.ui.Icons
import dev.eftrainer.core.ui.KindHeader
import dev.eftrainer.core.ui.ListRow
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.Order
import dev.eftrainer.data.content.Origin
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.data.content.Sort
import dev.eftrainer.data.content.Speak
import dev.eftrainer.data.content.Stress
import dev.eftrainer.data.content.Talk
import dev.eftrainer.data.content.Task
import dev.eftrainer.data.content.Write
import dev.eftrainer.data.content.needsAi
import dev.eftrainer.data.content.number
import dev.eftrainer.ui.appContainer
import dev.eftrainer.ui.label

@Composable
fun SectionScreen(
    sectionId: String,
    onBack: () -> Unit,
    onRules: () -> Unit,
    onTask: (taskId: String) -> Unit,
    modifier: Modifier = Modifier,
    vm: SectionViewModel = sectionViewModel(sectionId),
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val state = ui ?: return

    Column(modifier.fillMaxSize()) {
        BackLink(state.chapterId, onBack)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            SectionTitle(state)
            if (state.section.reference.isNotEmpty()) RulesRow(onClick = onRules)
            SectionTasks(state, onTask)
        }
    }
}

@Composable
private fun SectionTasks(
    state: SectionUi,
    onTask: (taskId: String) -> Unit,
) {
    Column {
        state.tasks.forEachIndexed { i, row ->
            if (state.tasks.startsOriginGroup(i)) OriginHeader(row.task.origin, state.chapterId)
            TaskItem(row, aiOn = state.aiOn, divider = !state.tasks.endsOriginGroup(i), onTask = onTask)
        }
    }
}

@Composable
private fun sectionViewModel(sectionId: String): SectionViewModel =
    appContainer().let { container -> viewModel(key = sectionId) { SectionViewModel(container, sectionId) } }

private fun Exercise.kindLabel(): Int =
    when (this) {
        is Blank -> R.string.kind_blank
        is Rewrite -> R.string.kind_rewrite
        is Speak -> R.string.kind_speak
        is Talk -> R.string.kind_talk
        is Write -> R.string.kind_write
        is Order -> R.string.kind_order
        is Stress -> R.string.kind_stress
        is Sort -> R.string.kind_sort
    }

private fun Task.kindLabel(): Int =
    exercises
        .groupingBy { it.kindLabel() }
        .eachCount()
        .maxBy { it.value }
        .key

@Composable
private fun SectionTitle(state: SectionUi) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KindHeader(state.section.kind, state.section.kind.label())
        Text(state.section.title, style = T.titleEn)
    }
}

@Composable
private fun RulesRow(onClick: () -> Unit) {
    Column(Modifier.padding(top = 20.dp)) {
        HorizontalDivider(color = C.Line)
        ListRow(onClick = onClick, divider = true) {
            Text(stringResource(R.string.rules), style = T.row, modifier = Modifier.weight(1f))
            Icon(Icons.ChevronRight, null, Modifier.size(20.dp), tint = C.Chevron)
        }
    }
}

@Composable
private fun OriginHeader(
    origin: Origin,
    chapterId: String,
) {
    Text(
        when (origin) {
            Origin.SB -> stringResource(R.string.book_sb)
            Origin.SB_GRAMMAR_BANK -> stringResource(R.string.ef_grammar_bank, chapterId)
            Origin.WB -> stringResource(R.string.book_wb)
            Origin.GENERATED -> stringResource(R.string.extra_practice)
        },
        style = T.label,
        modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
    )
}

@Composable
private fun TaskItem(
    row: TaskRow,
    aiOn: Boolean,
    divider: Boolean,
    onTask: (taskId: String) -> Unit,
) {
    val task = row.task
    val off = task.needsAi && !aiOn
    ListRow(onClick = { onTask(task.id) }.takeUnless { off }, divider = divider) {
        Text(
            task.number,
            style = T.row.copy(color = if (off) C.Off else EF.Title, fontWeight = FontWeight.Bold),
            softWrap = false,
            modifier = Modifier.widthIn(min = 30.dp),
        )
        TaskText(task, off, Modifier.weight(1f))
        if (!off) {
            Text(
                stringResource(R.string.n_of_m, row.mastered, row.total),
                style = T.label.copy(fontSize = 15.sp),
            )
        }
    }
}

@Composable
private fun TaskText(
    task: Task,
    off: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            task.instruction,
            style =
                T.rowEn.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = if (off) C.Off else T.rowEn.color,
                ),
        )
        val kind = stringResource(task.kindLabel())
        Text(
            if (off) stringResource(R.string.kind_needs_key, kind) else kind,
            style = T.rowSub.copy(color = if (off) C.Muted else EF.Title),
        )
    }
}
