package com.myshelf.app.ui.startup

import android.app.Activity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.myshelf.app.R
import com.myshelf.app.core.model.ThemeMode

/** Install before super.onCreate; the platform keeps the splash until the first frame. */
fun Activity.installShelfSplashScreen() {
    when (getSharedPreferences("shelf", 0).getString("theme", ThemeMode.SYSTEM.name)) {
        ThemeMode.LIGHT.name -> setTheme(R.style.Theme_MyShelf_Starting_Light)
        ThemeMode.DARK.name -> setTheme(R.style.Theme_MyShelf_Starting_Dark)
    }
    installSplashScreen().setOnExitAnimationListener { splash ->
        splash.view.animate().alpha(0f).setDuration(120L).withEndAction { splash.remove() }.start()
    }
}
