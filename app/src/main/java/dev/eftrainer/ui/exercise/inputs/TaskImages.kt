package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.eftrainer.core.ui.AssetImage

@Composable
fun TaskImages(
    images: List<String>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
    ) {
        for (image in images) {
            AssetImage(
                image,
                null,
                Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}
