package fr.free.nrw.commons.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import fr.free.nrw.commons.R

@Composable
fun CommonsTheme(content: @Composable () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val background = if (isDarkTheme)
        colorResource(R.color.main_background_dark)
    else
        colorResource(R.color.main_background_light)

    val colorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = colorResource(R.color.primaryColor),
            background = background,
            onBackground = Color.White,
            surfaceVariant = Color(0xFF1E1E1E),
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.primaryColor),
            background = background,
            onBackground = Color.Black,
            surfaceVariant = Color.White,
            onSurface = Color.Black
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}