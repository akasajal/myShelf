package com.myshelf.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myshelf.app.features.security.AppLockViewModel
import com.myshelf.app.features.security.BiometricAction
import com.myshelf.app.features.security.UnlockScreen
import com.myshelf.app.navigation.ShelfApp
import com.myshelf.app.state.ShelfViewModel
import com.myshelf.app.ui.theme.ShelfTheme

class MainActivity : FragmentActivity() {
    private val appLock: AppLockViewModel by viewModels()
    private lateinit var biometricPrompt: BiometricPrompt
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { appLock.finishBiometric(true) }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val cancelled = errorCode in listOf(BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                appLock.finishBiometric(false, if (cancelled) null else "Biometric unavailable. Use your PIN.")
            }
        })
        enableEdgeToEdge()
        setContent {
            val vm: ShelfViewModel = viewModel()
            ShelfTheme(vm.themeMode, vm.accent) {
                SideEffect {
                    if (appLock.enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                val shelfState = rememberSaveableStateHolder()
                if (appLock.locked) UnlockScreen(appLock, { requestBiometric(BiometricAction.UNLOCK) }, { moveTaskToBack(true) })
                else shelfState.SaveableStateProvider("shelf") {
                    ShelfApp(vm, appLock) { requestBiometric(BiometricAction.ENABLE) }
                }
            }
        }
    }

    private fun requestBiometric(action: BiometricAction) {
        if (!appLock.beginBiometric(action)) return
        biometricPrompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (action == BiometricAction.ENABLE) "Enable biometric unlock" else "Unlock My Shelf")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(if (action == BiometricAction.UNLOCK) "Use PIN" else "Cancel").build())
    }
    override fun onStart() {
        super.onStart()
        appLock.onForeground()
    }
    override fun onStop() {
        if (!isChangingConfigurations) {
            appLock.onBackground()
            biometricPrompt.cancelAuthentication()
        }
        super.onStop()
    }
}
