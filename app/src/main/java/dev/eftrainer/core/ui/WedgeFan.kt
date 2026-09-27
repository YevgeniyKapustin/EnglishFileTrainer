package dev.eftrainer.core.ui

import kotlin.random.Random

data class WedgeFan(
    val pivotX: Float,
    val pivotY: Float,
    val edges: List<Float>,
    val cardRadius: Float,
    val deepRadius: Float,
    val mainRadius: Float,
    val flapRadius: Float,
)

val BookFan =
    WedgeFan(
        pivotX = 0f,
        pivotY = -.0556f,
        edges = listOf(7f, 18.4f, 36.8f, 46.5f, 67.6f, 77.5f, 84.4f),
        cardRadius = 1.284f,
        deepRadius = 1.433f,
        mainRadius = 1.743f,
        flapRadius = 1.835f,
    )

const val EDGE_JITTER_DEGREES = 2.2f
const val PIVOT_JITTER = .04f
const val RADIUS_JITTER = .02f

fun wedgeFan(chapterId: String?): WedgeFan {
    if (chapterId == null) return BookFan
    val random = Random(chapterId.hashCode())

    fun jitter() = random.nextFloat() * 2f - 1f

    fun Float.stretched() = this * (1f + RADIUS_JITTER * jitter())
    return WedgeFan(
        pivotX = BookFan.pivotX + PIVOT_JITTER * jitter(),
        pivotY = BookFan.pivotY + PIVOT_JITTER * jitter(),
        edges = BookFan.edges.map { it + EDGE_JITTER_DEGREES * jitter() },
        cardRadius = BookFan.cardRadius.stretched(),
        deepRadius = BookFan.deepRadius.stretched(),
        mainRadius = BookFan.mainRadius.stretched(),
        flapRadius = BookFan.flapRadius.stretched(),
    )
}
