package com.myshelf.app.features.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy

@Composable
fun PinDialog(lock: AppLockViewModel, disable: Boolean, onDismiss: () -> Unit) {
    // PINs remain in memory only; never save them to instance state.
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) }
    var mismatch by remember { mutableStateOf(false) }
    val back = {
        if (!lock.busy) {
            if (firstPin != null) {
                firstPin = null; pin = ""; mismatch = false; lock.clearMessage()
            } else onDismiss()
        }
    }
    LaunchedEffect(lock.message, lock.retrySeconds) {
        if (lock.message != null || lock.retrySeconds > 0) pin = ""
    }
    Dialog(onDismissRequest = back, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
        dismissOnClickOutside = false, securePolicy = SecureFlagPolicy.SecureOn
    )) {
        BackHandler(onBack = back)
        PinPadScreen(
            title = if (disable) "Enter PIN to Disable" else if (firstPin == null) "Choose Your PIN" else "Confirm Your PIN",
            pin = pin, lock = lock, error = if (mismatch) "PINs don't match. Try again." else null,
            onBack = back,
            onDigit = { digit ->
                if (pin.length < 4 && !lock.busy && lock.retrySeconds == 0) {
                    mismatch = false; lock.clearMessage()
                    pin += digit
                    if (pin.length == 4) {
                        when {
                            disable -> lock.verify(pin, disable = true, completed = onDismiss)
                            firstPin == null -> { firstPin = pin; pin = "" }
                            firstPin != pin -> { mismatch = true; pin = "" }
                            else -> lock.enable(pin, onDismiss)
                        }
                    }
                }
            },
            onDelete = { pin = pin.dropLast(1); mismatch = false; lock.clearMessage() }
        )
    }
}

@Composable
fun UnlockScreen(lock: AppLockViewModel, onBiometric: () -> Unit, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    var pin by remember { mutableStateOf("") }
    LaunchedEffect(lock.message, lock.retrySeconds) {
        if (lock.message != null || lock.retrySeconds > 0) pin = ""
    }
    PinPadScreen(
        title = "Enter PIN to Unlock", pin = pin, lock = lock,
        onDigit = { digit ->
            if (pin.length < 4 && !lock.busy && lock.retrySeconds == 0 && lock.biometricAction == null) {
                lock.clearMessage()
                pin += digit
                if (pin.length == 4) lock.verify(pin)
            }
        },
        onDelete = { pin = pin.dropLast(1); lock.clearMessage() },
        onBiometric = if (lock.biometricEnabled && lock.biometricAvailable) onBiometric else null
    )
}

@Composable
private fun PinPadScreen(
    title: String, pin: String, lock: AppLockViewModel,
    onDigit: (String) -> Unit, onDelete: () -> Unit,
    error: String? = null, onBack: (() -> Unit)? = null, onBiometric: (() -> Unit)? = null
) {
    val background = MaterialTheme.colorScheme.surface
    val keyBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val accent = MaterialTheme.colorScheme.primary
    val idleDot = MaterialTheme.colorScheme.outlineVariant
    val enabled = !lock.busy && lock.retrySeconds == 0 && lock.biometricAction == null
    Surface(Modifier.fillMaxSize(), color = background) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)) {
            // Scale keys on short displays; allow scrolling when needed.
            val compact = maxHeight < 600.dp
            val availableHeight = maxHeight
            val keySize = minOf(64.dp, (maxWidth - 48.dp) / 3,
                ((maxHeight - 230.dp) / 4).coerceAtLeast(40.dp))
            val gap = if (compact) 8.dp else 12.dp
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().heightIn(min = availableHeight).padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(if (compact) 32.dp else 44.dp), tint = accent)
                    Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(if (compact) 18.dp else 30.dp))
                    Row(Modifier.clearAndSetSemantics {
                        contentDescription = "${pin.length} of 4 digits entered"
                        liveRegion = LiveRegionMode.Polite
                    }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(4) { index ->
                            Box(Modifier.size(18.dp).background(if (index < pin.length) accent else idleDot, CircleShape))
                        }
                    }
                    Box(Modifier.height(if (compact) 32.dp else 46.dp), contentAlignment = Alignment.Center) {
                        LockFeedback(lock, error)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { digits ->
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                digits.forEach { digit ->
                                    PinKey(digit, keySize, enabled, keyBackground) { onDigit(digit) }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.size(keySize))
                            PinKey("0", keySize, enabled, keyBackground) { onDigit("0") }
                            IconButton(onClick = onDelete, enabled = enabled && pin.isNotEmpty(), modifier = Modifier.size(keySize)) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, "Delete digit", Modifier.size(26.dp))
                            }
                        }
                    }
                    // Reserve this space during setup too, so all PIN stages align.
                    Box(Modifier.height(if (compact) 54.dp else 86.dp), contentAlignment = Alignment.Center) {
                        if (onBiometric != null) TextButton(onClick = onBiometric,
                            enabled = !lock.busy && lock.biometricAction == null,
                            colors = ButtonDefaults.textButtonColors(contentColor = accent)) {
                            Icon(Icons.Outlined.Fingerprint, null, Modifier.size(26.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Use Biometrics", fontSize = 14.sp)
                        }
                    }
                }
            }
            if (onBack != null) IconButton(onClick = onBack, enabled = !lock.busy,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        }
    }
}

@Composable
private fun PinKey(digit: String, size: androidx.compose.ui.unit.Dp, enabled: Boolean, background: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.size(size), shape = CircleShape,
        color = background, contentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
        Box(contentAlignment = Alignment.Center) {
            Text(digit, fontSize = 28.sp, fontWeight = FontWeight.Normal)
        }
    }
}

@Composable
private fun LockFeedback(lock: AppLockViewModel, error: String? = null) {
    if (lock.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    else {
        val text = if (lock.retrySeconds > 0) "Try again in ${lock.retrySeconds}s." else error ?: lock.message
        text?.let {
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center, modifier = Modifier.clearAndSetSemantics {
                    contentDescription = it; liveRegion = LiveRegionMode.Polite
                })
        }
    }
}
