package com.propslab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Scheme = darkColorScheme(
    primary = Palette.Chalk, onPrimary = Palette.Ink,
    secondary = Palette.Over, tertiary = Palette.Under,
    background = Palette.Ink, onBackground = Palette.Chalk,
    surface = Palette.Court, onSurface = Palette.Chalk,
    surfaceVariant = Palette.Raised, onSurfaceVariant = Palette.Dust,
    outline = Palette.Hairline, outlineVariant = Palette.Hairline,
    error = Palette.Loss,
)

@Composable
fun PropsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = PropsTypography, content = content)
}
