package com.nexavpn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexavpn.app.data.ApiClient
import com.nexavpn.app.data.LatencyProber
import com.nexavpn.app.data.VpnServer
import com.nexavpn.app.ui.components.ConnectButton
import com.nexavpn.app.ui.SettingsScreen
import com.nexavpn.app.ui.SplitTunnelScreen
import com.nexavpn.app.ui.theme.NexaTeal
import com.nexavpn.app.ui.theme.NexaTheme
import com.nexavpn.app.vpn.Consent
import com.nexavpn.app.vpn.KillSwitch
import com.nexavpn.app.vpn.TunnelState
import com.nexavpn.app.vpn.VpnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        pendingConsentCallback?.invoke(result.resultCode == RESULT_OK)
        pendingConsentCallback = null
    }

    private var pendingConsentCallback: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        KillSwitch.observe(this)
        setContent {
            NexaTheme {
                NexaVpnUi()
            }
        }
    }

    fun requestVpnConsent(onResult: (Boolean) -> Unit) {
        val intent = Consent.prepare(this)
        if (intent == null) {
            onResult(true)
        } else {
            pendingConsentCallback = onResult
            vpnPermissionLauncher.launch(intent)
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Shield),
    Servers("Servers", Icons.Outlined.Dns),
    Settings("Settings", Icons.Outlined.Settings),
}

@Composable
fun NexaVpnUi() {
    var tab by remember { mutableStateOf(Tab.Home) }
    var showSplitTunnel by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!showSplitTunnel) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Crossfade(targetState = Pair(tab, showSplitTunnel), label = "tabs") { state ->
            val (t, split) = state
            when {
                split -> SplitTunnelScreen(
                    modifier = Modifier.padding(padding),
                    onBack = { showSplitTunnel = false },
                )
                t == Tab.Home -> HomeScreen(Modifier.padding(padding))
                t == Tab.Servers -> ServersScreen(Modifier.padding(padding))
                t == Tab.Settings -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    onOpenSplitTunnel = { showSplitTunnel = true },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? MainActivity
    val vpnState by VpnManager.state.collectAsState()
    val api = remember { ApiClient() }

    var servers by remember { mutableStateOf<List<VpnServer>>(emptyList()) }
    var selected by remember { mutableStateOf<VpnServer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val remote = withContext(Dispatchers.IO) { api.fetchServers() }
        servers = remote?.servers ?: emptyList()
        if (selected == null) selected = servers.firstOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "NexaVPN",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(36.dp))

        Box(contentAlignment = Alignment.Center) {
            ConnectButton(
                state = vpnState,
                enabled = selected != null && vpnState != TunnelState.CONNECTING,
                onClick = {
                    val server = selected ?: return@ConnectButton
                    error = null
                    when (vpnState) {
                        TunnelState.DISCONNECTED -> activity?.requestVpnConsent { granted ->
                            if (granted) {
                                scope.launch {
                                    VpnManager.connect(context, server, api)
                                        .onFailure { error = it.message }
                                }
                            } else {
                                error = "VPN permission is required to connect"
                            }
                        }
                        else -> scope.launch {
                            VpnManager.disconnect(context).onFailure { error = it.message }
                        }
                    }
                },
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = when (vpnState) {
                TunnelState.DISCONNECTED -> "Not Protected"
                TunnelState.CONNECTING -> "Connecting…"
                TunnelState.CONNECTED -> "Protected"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = when (vpnState) {
                TunnelState.CONNECTED -> NexaTeal
                TunnelState.CONNECTING -> MaterialTheme.colorScheme.primary
                TunnelState.DISCONNECTED -> MaterialTheme.colorScheme.onBackground
            },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = selected?.let { "${it.city}, ${it.countryCode}" } ?: "No server selected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        error?.let { msg ->
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                    )
                    IconButton(onClick = { error = null }) {
                        Icon(Icons.Default.Close, "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Secure-core style strip at the bottom
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp),
        ) {
            Icon(
                Icons.Outlined.Shield,
                null,
                tint = if (vpnState == TunnelState.CONNECTED) NexaTeal
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (vpnState == TunnelState.CONNECTED)
                    "Traffic encrypted · WireGuard®"
                else "WireGuard® inside — fast and modern",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Servers
// ---------------------------------------------------------------------------

@Composable
fun ServersScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val vpnState by VpnManager.state.collectAsState()
    val api = remember { ApiClient() }

    var servers by remember { mutableStateOf<List<VpnServer>>(emptyList()) }
    var selected by remember { mutableStateOf<VpnServer?>(null) }
    var latencies by remember { mutableStateOf<Map<String, Int?>>(emptyMap()) }
    var isFallback by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        val remote = withContext(Dispatchers.IO) { api.fetchServers() }
        isFallback = remote == null
        servers = remote?.servers ?: demoServers()
        if (servers.none { it.id == selected?.id }) selected = servers.firstOrNull()
        loading = false
        latencies = withContext(Dispatchers.IO) { LatencyProber.probeAll(servers) }
    }

    LaunchedEffect(Unit) { load() }

    val filtered = remember(servers, query) {
        if (query.isBlank()) servers
        else servers.filter {
            it.country.contains(query, true) || it.city.contains(query, true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Servers", style = MaterialTheme.typography.headlineMedium)

        if (isFallback) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Backend unreachable — demo list",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search country or city") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { scope.launch { load() } }, enabled = !loading) {
                Text(if (loading) "Refreshing…" else "Refresh")
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(filtered) { s ->
                ServerCard(
                    server = s,
                    selected = s.id == selected?.id,
                    latencyMs = latencies[s.id],
                    enabled = vpnState == TunnelState.DISCONNECTED,
                    onClick = { selected = s },
                )
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: VpnServer,
    selected: Boolean,
    latencyMs: Int?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(server.country, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(server.city, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            latencyMs?.let { ms ->
                SignalBars(ms)
                Spacer(Modifier.width(6.dp))
                Text(
                    "$ms ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (server.premium) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text("PRO", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun SignalBars(ms: Int) {
    val level = when {
        ms < 100 -> 3
        ms < 250 -> 2
        else -> 1
    }
    Row(verticalAlignment = Alignment.Bottom) {
        repeat(3) { i ->
            val active = i < level
            val h = (8 + i * 5).dp
            Box(
                Modifier
                    .padding(horizontal = 1.dp)
                    .size(width = 4.dp, height = h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    ),
            )
        }
    }
}

fun demoServers(): List<VpnServer> = listOf(
    VpnServer("fallback-sg", "Singapore (demo)", "SG", "Singapore", "fallback.invalid", "PLACEHOLDER_PUBKEY"),
    VpnServer("fallback-de", "Germany (demo)", "DE", "Frankfurt", "fallback.invalid", "PLACEHOLDER_PUBKEY"),
    VpnServer("fallback-us", "United States (demo)", "US", "Ashburn", "fallback.invalid", "PLACEHOLDER_PUBKEY"),
    VpnServer("fallback-jp", "Japan (demo)", "JP", "Tokyo", "fallback.invalid", "PLACEHOLDER_PUBKEY"),
)
