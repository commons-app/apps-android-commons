package fr.free.nrw.commons.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColorScheme(
    background = DarkBackground,
    onSurface = DarkOnSurface,
    primary = DarkPrimary
)
private val LightColorPalette = lightColorScheme(
    background = LightBackground,
    onSurface = LightOnSurface,
    primary = LightPrimary
)

@Composable
fun CommonsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}