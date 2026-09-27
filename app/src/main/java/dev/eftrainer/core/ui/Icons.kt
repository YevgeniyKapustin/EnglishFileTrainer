package dev.eftrainer.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object Icons {
    val ChevronRight =
        icon {
            moveTo(9f, 6f)
            lineTo(15f, 12f)
            lineTo(9f, 18f)
        }
    val ChevronLeft =
        icon {
            moveTo(15f, 6f)
            lineTo(9f, 12f)
            lineTo(15f, 18f)
        }
    val Close =
        icon {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    val Check =
        icon {
            moveTo(5f, 12f)
            lineTo(10f, 17f)
            lineTo(20f, 7f)
        }
    val Lock =
        icon {
            moveTo(7f, 11f)
            lineTo(17f, 11f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            lineTo(19f, 19f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
            lineTo(7f, 21f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
            lineTo(5f, 13f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            close()
            moveTo(8f, 11f)
            lineTo(8f, 7f)
            arcToRelative(4f, 4f, 0f, false, true, 8f, 0f)
            lineTo(16f, 11f)
        }

    val Bell = svg("M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9 M13.73 21a2 2 0 0 1-3.46 0")

    val Gear =
        svg(
            "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z " +
                "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06" +
                "a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09" +
                "A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83" +
                "l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09" +
                "A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83" +
                "l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09" +
                "a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83" +
                "l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09" +
                "a1.65 1.65 0 0 0-1.51 1z",
            stroke = 1.8f,
        )
    val Sun =
        svg(
            "M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0zM12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7" +
                "l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4",
        )
    val Moon = svg("M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")

    val HalfCircle =
        ImageVector
            .Builder(
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).addPath(
                addPathNodes("M4 12a8 8 0 1 0 16 0a8 8 0 1 0 -16 0z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
            ).addPath(
                addPathNodes("M12 4a8 8 0 0 1 0 16z"),
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineJoin = StrokeJoin.Round,
            ).build()
    val Flame =
        ImageVector
            .Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                addPathNodes("M12 2c1 4 5 6 5 11a5 5 0 0 1-10 0c0-2 1-3.5 2-4.5 0 2 1 3 2 3 0-3-1-6 1-9.5z"),
                fill = SolidColor(Color.Black),
            ).build()
    val Key = svg("M4 15a4 4 0 1 0 8 0a4 4 0 1 0 -8 0zM10.8 12.2l8.2-8.2M16 7l3 3M14 9l2 2")

    private fun svg(
        d: String,
        stroke: Float = 2f,
    ) = ImageVector
        .Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(
            addPathNodes(d),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

    private fun icon(block: PathBuilder.() -> Unit) =
        ImageVector
            .Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            ).build()
}
