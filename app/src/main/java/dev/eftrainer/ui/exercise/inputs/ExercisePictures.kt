package dev.eftrainer.ui.exercise.inputs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.eftrainer.core.ui.AssetImage

private val PictureShape = RoundedCornerShape(12.dp)

@Composable
fun ExercisePictures(
    images: List<String>,
    modifier: Modifier = Modifier,
) {
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    val single = images.singleOrNull()
    if (single != null) {
        AssetImage(single, null, modifier.fillMaxWidth().clip(PictureShape))
    } else {
        Row(
            modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            images.forEach { image ->
                AssetImage(
                    image,
                    null,
                    Modifier
                        .height(150.dp)
                        .clip(PictureShape)
                        .clickable { opened = image },
                )
            }
        }
    }
    opened?.let { name ->
        Dialog(
            onDismissRequest = { opened = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AssetImage(
                name,
                null,
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(PictureShape)
                    .clickable { opened = null },
            )
        }
    }
}
