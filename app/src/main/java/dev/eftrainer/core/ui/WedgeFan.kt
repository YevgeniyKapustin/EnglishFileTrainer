package dev.eftrainer.core.ui

import kotlin.random.Random

enum class WedgeShade { LIGHT, BRIGHT, SOFT, DEEP, DARKEST }

data class WedgeFan(
    val shieldTop: Float,
    val shieldBottom: Float,
    val shieldBulge: Float,
    val hubX: Float,
    val hubDrop: Float,
    val leftTilt: Float,
    val rayWeights: List<Float>,
    val rayShades: List<WedgeShade?>,
    val accentTop: Float,
    val accentWidth: Float,
    val flapLength: Float,
)

private class Span(
    val from: Float,
    val to: Float,
) {
    fun pick(random: Random) = from + (to - from) * random.nextFloat()
}

private val ShieldTop = Span(250f, 295f)
private val ShieldBottom = Span(120f, 160f)
private val ShieldBulge = Span(.6f, .85f)
private val HubX = Span(20f, 70f)
private val HubDrop = Span(4f, 24f)
private val LeftTilt = Span(0f, 8f)
private val AccentTop = Span(.35f, .55f)
private val AccentWidth = Span(7f, 12f)
private val FlapLength = Span(40f, 90f)
private val FreeRayWeight = Span(.55f, 1.45f)
private val DeepRayWeight = Span(.7f, 1.1f)
private val DarkestRayWeight = Span(.3f, .5f)
private const val FEWEST_FREE_RAYS = 4
private const val MOST_FREE_RAYS = 6
private val FirstRayShades = listOf(WedgeShade.BRIGHT, WedgeShade.SOFT, null)
private val FreeRayShades = listOf(WedgeShade.LIGHT, WedgeShade.BRIGHT, WedgeShade.SOFT, null)

fun wedgeFan(chapterId: String?): WedgeFan {
    val random = Random(chapterId?.hashCode() ?: 0)
    val shieldTop = ShieldTop.pick(random)
    val shieldBottom = ShieldBottom.pick(random)
    val shieldBulge = ShieldBulge.pick(random)
    val hubX = HubX.pick(random)
    val hubDrop = HubDrop.pick(random)
    val leftTilt = LeftTilt.pick(random)
    val accentTop = AccentTop.pick(random)
    val accentWidth = AccentWidth.pick(random)
    val flapLength = FlapLength.pick(random)
    val freeRays = random.nextInt(FEWEST_FREE_RAYS, MOST_FREE_RAYS + 1)
    val weights =
        List(freeRays) { FreeRayWeight.pick(random) } + DeepRayWeight.pick(random) + DarkestRayWeight.pick(random)
    val shades =
        List(freeRays) { (if (it == 0) FirstRayShades else FreeRayShades).random(random) } +
            WedgeShade.DEEP + WedgeShade.DARKEST
    return WedgeFan(
        shieldTop = shieldTop,
        shieldBottom = shieldBottom,
        shieldBulge = shieldBulge,
        hubX = hubX,
        hubDrop = hubDrop,
        leftTilt = leftTilt,
        rayWeights = weights,
        rayShades = shades,
        accentTop = accentTop,
        accentWidth = accentWidth,
        flapLength = flapLength,
    )
}
