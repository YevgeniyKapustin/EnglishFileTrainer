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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.eftrainer.R
import dev.eftrainer.core.ui.AssetImage
import dev.eftrainer.data.content.Picture

private val PictureShape = RoundedCornerShape(12.dp)

@Composable
internal fun TalkPictures(
    pictures: List<Picture>,
    modifier: Modifier = Modifier,
) {
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    val height = if (pictures.size == 1) 240.dp else 150.dp
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pictures.forEach { picture ->
            AssetImage(
                picture.image,
                stringResource(R.string.talk_picture, picture.label),
                Modifier
                    .height(height)
                    .clip(PictureShape)
                    .clickable { opened = picture.image },
            )
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
