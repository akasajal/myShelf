package com.myshelf.app.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myshelf.app.BuildConfig
import com.myshelf.app.core.model.Accent
import com.myshelf.app.core.model.CardDetails
import com.myshelf.app.core.model.ThemeMode
import com.myshelf.app.features.security.AppLockViewModel
import com.myshelf.app.features.security.PinDialog
import com.myshelf.app.state.ShelfViewModel
import com.myshelf.app.ui.theme.swatch
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: ShelfViewModel, lock: AppLockViewModel, onEnableBiometric: () -> Unit, modifier: Modifier = Modifier) {
    var pinDialog by remember { mutableStateOf<Boolean?>(null) }
    pinDialog?.let { disable ->
        PinDialog(lock, disable) { pinDialog = null; lock.clearMessage() }
    }
    val light = MaterialTheme.colorScheme.surface.luminance() > .5f
    val background = if (light) Color(0xFFF8F7FA) else MaterialTheme.colorScheme.surface
    val controlBackground = if (light) Color.White else MaterialTheme.colorScheme.surfaceContainer
    val selectedBackground = if (light) Color(0xFFE7DFF3) else Color(0xFF413850)
    val outline = if (light) Color(0xFFD9D7DD) else MaterialTheme.colorScheme.outlineVariant
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.exportShelf(uri)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.previewImport(uri)
    }
    val backup = vm.pendingImport
    if (backup != null) key(backup) { ImportPreviewDialog(vm, backup) }
    Column(modifier.fillMaxSize().background(background).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 20.dp, bottom = 56.dp))
        SectionLabel("Appearance")
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Theme", modifier = Modifier.weight(.36f), fontSize = 16.sp)
            Row(Modifier.weight(.64f).selectableGroup().clip(RoundedCornerShape(28.dp))
                .border(1.dp, outline, RoundedCornerShape(28.dp))) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                val selected = vm.themeMode == mode
                Row(Modifier.weight(1f).background(if (selected) selectedBackground else controlBackground)
                    .selectable(selected, onClick = { vm.setTheme(mode) }, role = Role.RadioButton)
                    .heightIn(min = 52.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(12.dp))
                    Text(mode.label, Modifier.padding(start = if (selected) 6.dp else 0.dp), fontSize = 11.sp, maxLines = 1)
                }
                if (index < ThemeMode.entries.lastIndex) VerticalDivider(Modifier.height(52.dp), color = outline)
            }
            }
        }
        Spacer(Modifier.height(30.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Accent Color", Modifier.weight(.36f), fontSize = 16.sp)
            Row(Modifier.weight(.64f).selectableGroup()) {
            Accent.entries.forEach { accent ->
                val selected = accent == vm.accent
                BoxWithConstraints(Modifier.weight(1f).height(48.dp).clip(CircleShape)
                    .selectable(selected, onClick = { vm.updateAccent(accent) }, role = Role.RadioButton)
                    .semantics { contentDescription = "${accent.label} accent" }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(minOf(40.dp, maxWidth - 2.dp))
                        .border(1.dp, if (selected) outline else Color.Transparent, CircleShape)
                        .padding(if (selected) 3.dp else 1.dp).background(accent.swatch, CircleShape))
                }
            }
                }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Cards per row", Modifier.weight(.36f), fontSize = 16.sp)
            Row(Modifier.weight(.64f).selectableGroup().clip(RoundedCornerShape(28.dp))
                .border(1.dp, outline, RoundedCornerShape(28.dp))) {
                (2..4).forEach { count ->
                    val selected = vm.cardsPerRow == count
                    Row(Modifier.weight(1f)
                        .background(if (selected) selectedBackground else controlBackground)
                        .selectable(selected, onClick = { vm.updateCardsPerRow(count) }, role = Role.RadioButton)
                        .semantics { contentDescription = "$count cards per row" }
                        .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(12.dp))
                        Text(count.toString(), Modifier.padding(start = if (selected) 6.dp else 0.dp), fontSize = 13.sp)
                    }
                    if (count < 4) VerticalDivider(Modifier.height(48.dp), color = outline)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Card details", Modifier.weight(.36f), fontSize = 16.sp)
            Row(Modifier.weight(.64f).selectableGroup().clip(RoundedCornerShape(28.dp))
                .border(1.dp, outline, RoundedCornerShape(28.dp))) {
                CardDetails.entries.forEachIndexed { index, option ->
                    val selected = vm.cardDetails == option
                    Box(Modifier.weight(1f).background(if (selected) selectedBackground else controlBackground)
                        .selectable(selected, onClick = { vm.updateCardDetails(option) }, role = Role.RadioButton)
                        .heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                        Text(option.label, fontSize = 10.sp, maxLines = 1)
                    }
                    if (index == 0) VerticalDivider(Modifier.height(48.dp), color = outline)
                }
            }
        }
        Spacer(Modifier.height(46.dp))
        SectionLabel("Backups")
        Spacer(Modifier.height(24.dp))
        val enabled = !vm.transferBusy && vm.loadError == null && backup == null
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { importer.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
            enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = controlBackground, contentColor = MaterialTheme.colorScheme.onSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, outline)) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Import")
        }
        OutlinedButton(onClick = { exporter.launch("my-shelf-${LocalDate.now()}.zip") },
            enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = controlBackground, contentColor = MaterialTheme.colorScheme.onSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, outline)) {
            Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Export")
        }
        }
        Spacer(Modifier.height(16.dp))
        if (vm.transferBusy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Processing…", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        vm.transferMessage?.let { message ->
            Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        vm.loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(30.dp))
        SectionLabel("Privacy")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrivacyToggle("App Lock", Icons.Outlined.Lock, if (lock.enabled) "Protected" else "Off",
                checked = lock.enabled, enabled = !lock.busy,
                accent = vm.accent.swatch, modifier = Modifier.weight(1f)) {
                lock.clearMessage(); pinDialog = lock.enabled
            }
            PrivacyToggle("Biometric", Icons.Outlined.Fingerprint,
                when {
                    lock.biometricEnabled -> "On"
                    !lock.enabled -> "PIN first"
                    !lock.biometricAvailable -> "Unavailable"
                    else -> "Off"
                }, checked = lock.biometricEnabled,
                enabled = lock.enabled && !lock.busy && (lock.biometricAvailable || lock.biometricEnabled),
                accent = vm.accent.swatch, modifier = Modifier.weight(1f)) { value ->
                if (value) onEnableBiometric() else lock.disableBiometrics()
            }
        }
        if (pinDialog == null) lock.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(32.dp))
        SectionLabel("About")
        Spacer(Modifier.height(22.dp))
        Text("My Shelf", fontSize = 16.sp)
        Text("v${BuildConfig.VERSION_NAME} · Your personal media library.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PrivacyToggle(title: String, icon: ImageVector, state: String, checked: Boolean, enabled: Boolean, accent: Color,
    modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .45f)
    Surface(modifier.fillMaxHeight().clip(RoundedCornerShape(20.dp))
        .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        shape = RoundedCornerShape(20.dp),
        color = if (checked) accent.copy(alpha = .1f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (checked) accent.copy(alpha = .35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(30.dp).background(accent.copy(alpha = if (enabled) .14f else .06f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(19.dp), tint = if (enabled) accent else foreground)
                }
                Text(title, style = MaterialTheme.typography.labelLarge, color = foreground, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(state, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = if (checked) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .6f))
                Switch(checked = checked, onCheckedChange = null, enabled = enabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.White,
                        checkedBorderColor = Color.Transparent))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val light = MaterialTheme.colorScheme.surface.luminance() > .5f
    Text(text.uppercase(), color = if (light) Color(0xFFBD7297) else MaterialTheme.colorScheme.primary, fontSize = 11.sp, letterSpacing = 1.5.sp)
}
