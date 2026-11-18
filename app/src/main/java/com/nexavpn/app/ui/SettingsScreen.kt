package com.nexavpn.app.ui

import android.content.Intent
import android.net.VpnService
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexavpn.app.data.AppSettings
import com.nexavpn.app.data.SettingsRepository
import com.nexavpn.app.vpn.KillSwitch
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenSplitTunnel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by SettingsRepository.settings(context).collectAsState(initial = AppSettings())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                SettingsToggle(
                    title = "Kill switch",
                    subtitle = "Reconnect automatically and keep traffic protected if the tunnel drops unexpectedly",
                    checked = settings.killSwitch,
                    onChange = { scope.launch { SettingsRepository.setKillSwitch(context, it) } },
                )
            }
            item {
                SettingsToggle(
                    title = "Auto-connect",
                    subtitle = "Connect to the last server when the app starts",
                    checked = settings.autoConnect,
                    onChange = { scope.launch { SettingsRepository.setAutoConnect(context, it) } },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Split tunneling") },
                    supportingContent = { Text("Choose which apps bypass the VPN") },
                    modifier = Modifier.clickableRow(onClick = onOpenSplitTunnel),
                )
            }
            item {
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Always-on VPN (system)") },
                    supportingContent = {
                        Text("Open Android's VPN settings to enable always-on + \"block connections without VPN\" for the strongest kill switch")
                    },
                    modifier = Modifier.clickableRow(onClick = {
                        context.startActivity(Intent("android.net.vpn.SETTINGS"))
                    }),
                )
            }
            item {
                HorizontalDivider()
                val ver = remember { BuildInfo.version }
                ListItem(
                    headlineContent = { Text("Version") },
                    supportingContent = { Text(ver) },
                )
            }
        }
    }

    // Activate kill-switch observer when the setting flips on.
    LaunchedEffect(settings.killSwitch) {
        if (settings.killSwitch) KillSwitch.observe(context)
    }
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, fontSize = 12.sp) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(
        clickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            onClick = onClick,
        )
    )

/** Injected at build time in a later release step; placeholder for now. */
object BuildInfo {
    var version: String = "dev"
}
