package com.myshelf.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.myshelf.app.core.model.Accent

/** Populate every accent and surface role so Material components share one palette. */
internal fun shelfColorScheme(accent: Accent, dark: Boolean): ColorScheme {
    val base = if (dark) Color(0xFF181B1B) else Color(0xFFF7F5EF)
    val ink = if (dark) Color(0xFFE7E8E1) else Color(0xFF232D29)
    val primary = if (dark) lerp(accent.swatch, Color.White, .36f) else accent.light
    val onPrimary = if (dark) Color(0xFF18201B) else Color.White
    fun tint(amount: Float) = lerp(base, accent.swatch, amount)
    val container = tint(if (dark) .25f else .18f)
    val defaults = if (dark) darkColorScheme() else lightColorScheme()
    return defaults.copy(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = container, onPrimaryContainer = ink,
        inversePrimary = if (dark) accent.light else lerp(accent.swatch, Color.White, .36f),
        secondary = primary, onSecondary = onPrimary,
        secondaryContainer = container, onSecondaryContainer = ink,
        tertiary = primary, onTertiary = onPrimary,
        tertiaryContainer = container, onTertiaryContainer = ink,
        background = base, onBackground = ink,
        surface = base, onSurface = ink, surfaceTint = primary,
        surfaceVariant = tint(if (dark) .16f else .09f),
        onSurfaceVariant = if (dark) Color(0xFFC2C9C3) else Color(0xFF555E58),
        surfaceDim = tint(if (dark) .02f else .14f),
        surfaceBright = tint(if (dark) .18f else .01f),
        surfaceContainerLowest = if (dark) lerp(base, Color.Black, .18f) else Color.White,
        surfaceContainerLow = tint(.04f), surfaceContainer = tint(.07f),
        surfaceContainerHigh = tint(.10f), surfaceContainerHighest = tint(.14f),
        outline = lerp(ink, base, .5f), outlineVariant = lerp(ink, base, .8f),
        inverseSurface = ink, inverseOnSurface = base
    )
}
