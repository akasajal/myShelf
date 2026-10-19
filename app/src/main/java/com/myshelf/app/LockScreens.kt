package com.myshelf.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PinDialog(lock: AppLockViewModel, disable: Boolean, onDismiss: () -> Unit) {
    // PIN entry and confirmation are deliberately never saved to instance state.
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) }
    var mismatch by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!lock.busy) onDismiss() },
        title = { Text(if (disable) "Disable app lock" else if (firstPin == null) "Enter a 4-digit PIN" else "Confirm PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                key(firstPin != null) { PinField(pin, {
                    pin = it; mismatch = false; lock.clearMessage()
                }, enabled = !lock.busy && lock.retrySeconds == 0) }
                if (mismatch) Text("PINs don't match.", color = MaterialTheme.colorScheme.error)
                LockFeedback(lock)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    disable -> lock.verify(pin, disable = true, completed = onDismiss)
                    firstPin == null -> { firstPin = pin; pin = "" }
                    firstPin != pin -> { mismatch = true; pin = "" }
                    else -> lock.enable(pin, onDismiss)
                }
            }, enabled = PinCode.valid(pin) && !lock.busy && lock.retrySeconds == 0) {
                Text(if (disable) "Disable" else if (firstPin == null) "Next" else "Enable")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !lock.busy) { Text("Cancel") } })
}

@Composable
fun UnlockScreen(lock: AppLockViewModel, onBiometric: () -> Unit, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    var pin by remember { mutableStateOf("") }
    // Clear an attempted PIN after a failed verification.
    LaunchedEffect(lock.message, lock.retrySeconds) {
        if (lock.message != null || lock.retrySeconds > 0) pin = ""
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.Lock, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(20.dp))
            Text("My Shelf", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            Box(Modifier.widthIn(max = 240.dp)) {
                PinField(pin, { pin = it; lock.clearMessage() }, !lock.busy && lock.retrySeconds == 0)
            }
            Spacer(Modifier.height(12.dp))
            LockFeedback(lock)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { lock.verify(pin) }, enabled = PinCode.valid(pin) && !lock.busy && lock.retrySeconds == 0) { Text("Unlock") }
            if (lock.biometricEnabled && lock.biometricAvailable) {
                TextButton(onClick = onBiometric, enabled = !lock.busy && lock.biometricAction == null) {
                    Icon(Icons.Outlined.Fingerprint, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use biometric")
                }
            }
        }
    }
}

@Composable
private fun PinField(pin: String, onChange: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(value = pin, onValueChange = { value ->
        if (value.length <= 4 && value.all { it in '0'..'9' }) onChange(value)
    }, label = { Text("PIN") }, enabled = enabled, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun LockFeedback(lock: AppLockViewModel) {
    if (lock.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    if (lock.retrySeconds > 0) Text("Try again in ${lock.retrySeconds}s.", color = MaterialTheme.colorScheme.error)
    else lock.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
