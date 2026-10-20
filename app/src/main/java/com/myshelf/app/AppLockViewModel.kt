package com.myshelf.app

import android.app.Application
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

enum class BiometricAction { ENABLE, UNLOCK }

class AppLockViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("app_lock", 0)
    var enabled by mutableStateOf(prefs.getBoolean("enabled", false))
        private set
    private val session = LockSession(enabled)
    var locked by mutableStateOf(session.locked)
        private set
    var biometricEnabled by mutableStateOf(enabled && prefs.getBoolean("biometric", false))
        private set
    var biometricAvailable by mutableStateOf(false)
        private set
    var biometricAction by mutableStateOf<BiometricAction?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var retrySeconds by mutableIntStateOf(0)
        private set
    private var cooldown: Job? = null
    private var foreground = true

    init { refreshBiometrics(); updateCooldown() }

    fun refreshBiometrics() {
        biometricAvailable = BiometricManager.from(getApplication())
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
    fun clearMessage() { message = null }
    fun disableBiometrics() { biometricEnabled = false; prefs.edit().putBoolean("biometric", false).apply() }
    fun beginBiometric(action: BiometricAction): Boolean {
        refreshBiometrics()
        if (!enabled || !biometricAvailable || busy || biometricAction != null || (action == BiometricAction.UNLOCK && !biometricEnabled)) return false
        biometricAction = action
        message = null
        return true
    }
    fun finishBiometric(success: Boolean, error: String? = null) {
        val action = biometricAction
        biometricAction = null
        if (!success) { message = error; return }
        if (action == BiometricAction.ENABLE && enabled && !locked) {
            biometricEnabled = true
            prefs.edit().putBoolean("biometric", true).apply()
        } else if (action == BiometricAction.UNLOCK && enabled && biometricEnabled) unlock()
    }
    fun onBackground() {
        foreground = false
        biometricAction = null
        session.background()
        locked = session.locked
    }
    fun onForeground() {
        foreground = true
        locked = session.locked
        refreshBiometrics()
        updateCooldown()
    }

    fun enable(pin: String, completed: () -> Unit) {
        if (enabled || busy || !PinCode.valid(pin)) return
        busy = true; message = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                    val verifier = PinCode.digest(pin, salt, key(create = true))
                    check(prefs.edit().putString("salt", encode(salt)).putString("verifier", encode(verifier))
                        .putBoolean("enabled", true).putBoolean("biometric", false)
                        .putInt("failures", 0).putLong("retryAt", 0).commit())
                }
                enabled = true; biometricEnabled = false; session.enable()
                if (!foreground) session.background()
                locked = session.locked
                completed()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "Couldn't set up app lock. Try again." }
            finally { busy = false }
        }
    }

    fun verify(pin: String, disable: Boolean = false, completed: () -> Unit = {}) {
        if (!enabled || busy || !PinCode.valid(pin)) return
        if (prefs.getLong("retryAt", 0) > System.currentTimeMillis()) { updateCooldown(); return }
        busy = true; message = null
        viewModelScope.launch {
            try {
                val correct = withContext(Dispatchers.IO) {
                    PinCode.matches(pin, decode(requireNotNull(prefs.getString("salt", null))), key(false),
                        decode(requireNotNull(prefs.getString("verifier", null))))
                }
                if (correct) {
                    if (disable) {
                        withContext(Dispatchers.IO) { check(prefs.edit().clear().commit()) }
                        enabled = false; biometricEnabled = false
                        session.disable(); locked = false
                    } else unlock()
                    prefs.edit().putInt("failures", 0).putLong("retryAt", 0).apply()
                    retrySeconds = 0
                    completed()
                } else {
                    val attempts = prefs.getInt("failures", 0) + 1
                    if (attempts >= 5) {
                        // Commit the cooldown so restarting the process does not reset attempts.
                        withContext(Dispatchers.IO) {
                            check(prefs.edit().putInt("failures", 0).putLong("retryAt", System.currentTimeMillis() + 30_000).commit())
                        }
                        updateCooldown()
                    } else {
                        withContext(Dispatchers.IO) { check(prefs.edit().putInt("failures", attempts).commit()) }
                        message = "Incorrect PIN."
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "Couldn't verify PIN. Try again." }
            finally { busy = false }
        }
    }

    private fun unlock() {
        if (!foreground) return
        session.unlock(); locked = false; message = null
        prefs.edit().putInt("failures", 0).putLong("retryAt", 0).apply()
        retrySeconds = 0
    }
    private fun updateCooldown() {
        cooldown?.cancel()
        cooldown = viewModelScope.launch {
            do {
                retrySeconds = ((prefs.getLong("retryAt", 0) - System.currentTimeMillis() + 999) / 1000).toInt().coerceIn(0, 30)
                if (retrySeconds > 0) delay(1000)
            } while (retrySeconds > 0)
        }
    }
    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("myshelf_pin", null) as? SecretKey)?.let { return it }
        check(create) { "PIN key unavailable." }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder("myshelf_pin", KeyProperties.PURPOSE_SIGN).build())
            generateKey()
        }
    }
    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)
}
