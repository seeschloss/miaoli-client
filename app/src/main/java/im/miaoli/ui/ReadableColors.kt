package im.miaoli.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import im.miaoli.ui.theme.MiaoliDarkPrimary
import im.miaoli.ui.theme.MiaoliLightPrimary
import kotlin.math.max
import kotlin.math.min

/** Returns black or white, whichever contrasts more with [background]. */
internal fun readableTextColor(background: Color): Color {
    val blackContrast = contrastRatio(background, Color.Black)
    val whiteContrast = contrastRatio(background, Color.White)
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}

/** Returns the app link blue that contrasts more with [background]. */
internal fun readableLinkColor(background: Color): Color {
    val lightContrast = contrastRatio(background, MiaoliLightPrimary)
    val darkContrast = contrastRatio(background, MiaoliDarkPrimary)
    return if (lightContrast >= darkContrast) MiaoliLightPrimary else MiaoliDarkPrimary
}

private fun contrastRatio(first: Color, second: Color): Float {
    val firstLuminance = first.luminance()
    val secondLuminance = second.luminance()
    val lighter = max(firstLuminance, secondLuminance)
    val darker = min(firstLuminance, secondLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}
