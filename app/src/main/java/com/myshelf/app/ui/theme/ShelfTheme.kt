package com.myshelf.app.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
    val activity = LocalContext.current as? ComponentActivity
    SideEffect {
        val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.DKGRAY)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    val colors = shelfColorScheme(accent, dark)
    MaterialTheme(colorScheme = colors, content = content)
}
