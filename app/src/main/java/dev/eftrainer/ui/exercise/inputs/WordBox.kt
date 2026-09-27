package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.EF
import dev.eftrainer.core.theme.T

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordBox(
    words: List<String>,
    modifier: Modifier = Modifier,
    used: Set<String> = emptySet(),
) {
    val style = T.rowEn.copy(fontSize = 17.sp, lineHeight = 26.sp)
    FlowRow(
        modifier
            .fillMaxWidth()
            .border(1.5.dp, EF.Title, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        words.forEach { word ->
            val done = word in used
            Text(
                word,
                style =
                    if (done) {
                        style.copy(color = C.Off, textDecoration = TextDecoration.LineThrough)
                    } else {
                        style
                    },
            )
        }
    }
}
