package fr.free.nrw.commons.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CommonsTheme(content: @Composable () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val colorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = PrimaryBlueNight,
            background = BackgroundDark,
            onBackground = TextWhite,
            surfaceVariant = CardBackgroundDark,
            onSurface = TextWhite
        )
    } else {
        lightColorScheme(
            primary = PrimaryBlue,
            background = BackgroundLight,
            onBackground = TextBlack,
            surfaceVariant = CardBackgroundLight,
            onSurface = TextBlack
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}