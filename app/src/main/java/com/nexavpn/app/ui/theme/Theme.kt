package com.nexavpn.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexaBlue = Color(0xFF2962FF)
private val NexaBlueDark = Color(0xFF1565C0)
private val NexaGreen = Color(0xFF00C853)
private val NexaBg = Color(0xFFF6F7FB)
private val NexaBgDark = Color(0xFF101318)

private val LightColors = lightColorScheme(
    primary = NexaBlue,
    secondary = NexaBlueDark,
    tertiary = NexaGreen,
    background = NexaBg,
)

private val DarkColors = darkColorScheme(
    primary = NexaBlue,
    secondary = NexaBlueDark,
    tertiary = NexaGreen,
    background = NexaBgDark,
)

@Composable
fun NexaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
