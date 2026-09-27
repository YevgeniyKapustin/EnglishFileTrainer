package dev.eftrainer.core.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

fun imageAsset(name: String): String = "image/$name.webp"

@Composable
fun AssetImage(
    name: String,
    description: String?,
    modifier: Modifier = Modifier,
    keepRatio: Boolean = true,
) {
    val context = LocalContext.current
    val bitmap: ImageBitmap? =
        remember(name) {
            try {
                context.assets.open(imageAsset(name)).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            } catch (_: IOException) {
                null
            }
        }
    bitmap ?: return
    Image(
        bitmap,
        description,
        if (keepRatio) {
            modifier.aspectRatio(bitmap.width.toFloat() / bitmap.height, matchHeightConstraintsFirst = true)
        } else {
            modifier
        },
        contentScale = ContentScale.Crop,
    )
}
