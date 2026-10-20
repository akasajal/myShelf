package com.myshelf.app.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.myshelf.app.core.model.Accent
import com.myshelf.app.core.model.ThemeMode

@Composable
fun ShelfTheme(mode: ThemeMode, accent: Accent, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val base = if (dark) Color(0xFF181B1B) else Color(0xFFF7F5EF)
    val activity = LocalContext.current as? ComponentActivity
    SideEffect {
        val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.DKGRAY)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    val primary = if (dark) lerp(accent.swatch, Color.White, .36f) else accent.light
    val container = lerp(base, accent.swatch, if (dark) .25f else .18f)
    val colors = if (dark) darkColorScheme(
        primary = primary, onPrimary = Color(0xFF18201B), primaryContainer = container,
        onPrimaryContainer = Color(0xFFF1F0E9), secondary = primary, secondaryContainer = container,
        onSecondaryContainer = Color(0xFFF1F0E9), background = base, surface = base,
        onSurface = Color(0xFFE7E8E1), onBackground = Color(0xFFE7E8E1),
        surfaceVariant = Color(0xFF303632), onSurfaceVariant = Color(0xFFC2C9C3)
    ) else lightColorScheme(
        primary = primary, onPrimary = Color.White, primaryContainer = container,
        onPrimaryContainer = Color(0xFF232D29), secondary = primary, secondaryContainer = container,
        onSecondaryContainer = Color(0xFF232D29), background = base, surface = base,
        onSurface = Color(0xFF232D29), onBackground = Color(0xFF232D29),
        surfaceVariant = Color(0xFFE9E8E0), onSurfaceVariant = Color(0xFF555E58)
    )
    MaterialTheme(colorScheme = colors, content = content)
}
