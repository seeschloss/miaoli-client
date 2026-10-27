package im.miaoli.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val MiaoliLightPrimary = Color(0xFF1976D2)
internal val MiaoliDarkPrimary = Color(0xFF90CAF9)

/** Accent colours for this device's posts and replies to them. */
internal data class MiaoliPostColors(
    val ownContainer: Color,
    val ownContent: Color,
    val replyContainer: Color,
    val replyContent: Color,
)

private val MiaoliLightPostColors = MiaoliPostColors(
    ownContainer = Color(0xFFD4F2DB),
    ownContent = Color(0xFF12351D),
    replyContainer = Color(0xFFFFE8B8),
    replyContent = Color(0xFF4A3200),
)

private val MiaoliDarkPostColors = MiaoliPostColors(
    ownContainer = Color(0xFF1B3D28),
    ownContent = Color(0xFFD4F2DB),
    replyContainer = Color(0xFF4A3512),
    replyContent = Color(0xFFFFE8B8),
)

internal val LocalMiaoliPostColors = staticCompositionLocalOf {
    MiaoliLightPostColors
}

private val MiaoliLightColors = lightColorScheme(
    primary = MiaoliLightPrimary,
    onPrimary = Color.White,
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF444444),
    outlineVariant = Color(0xFFC4C4C4),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val MiaoliDarkColors = darkColorScheme(
    primary = MiaoliDarkPrimary,
    onPrimary = Color(0xFF003258),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFC4C4C4),
    outlineVariant = Color(0xFF4A4A4A),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun MiaoliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMiaoliPostColors provides if (darkTheme) {
            MiaoliDarkPostColors
        } else {
            MiaoliLightPostColors
        },
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) MiaoliDarkColors else MiaoliLightColors,
            content = content,
        )
    }
}
