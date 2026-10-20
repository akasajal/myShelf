package com.myshelf.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.myshelf.app.core.model.Accent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfColorsTest {
    @Test fun materialAccentRolesAndRaisedSurfacesChangeWithEveryAccent() {
        listOf(false, true).forEach { dark ->
            val palettes = Accent.entries.map { shelfColorScheme(it, dark) }
            val roles: List<(androidx.compose.material3.ColorScheme) -> Color> = listOf(
                { it.primary }, { it.secondary }, { it.tertiary }, { it.inversePrimary },
                { it.primaryContainer }, { it.secondaryContainer }, { it.tertiaryContainer },
                { it.surfaceTint }, { it.surfaceContainerLow }, { it.surfaceContainer },
                { it.surfaceContainerHigh }, { it.surfaceContainerHighest }
            )
            roles.forEachIndexed { index, role ->
                assertEquals("Role $index retains a fixed color (dark=$dark)", Accent.entries.size,
                    palettes.map(role).distinct().size)
            }
        }
    }

    @Test fun accentTextAndFilledControlsRemainReadableInBothThemes() {
        Accent.entries.forEach { accent ->
            listOf(false, true).forEach { dark ->
                val colors = shelfColorScheme(accent, dark)
                listOf(colors.primary to colors.surface, colors.onPrimary to colors.primary,
                    colors.onPrimaryContainer to colors.primaryContainer).forEach { (text, background) ->
                    val a = text.luminance(); val b = background.luminance()
                    val contrast = (maxOf(a, b) + .05f) / (minOf(a, b) + .05f)
                    assertTrue("$accent dark=$dark contrast=$contrast", contrast >= 4.5f)
                }
            }
        }
    }
}
