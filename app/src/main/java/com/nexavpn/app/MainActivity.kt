package com.nexavpn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexavpn.app.data.ApiClient
import com.nexavpn.app.data.AppSettings
import com.nexavpn.app.data.LatencyProber
import com.nexavpn.app.data.SettingsRepository
import com.nexavpn.app.data.VpnServer
import com.nexavpn.app.ui.SettingsScreen
import com.nexavpn.app.ui.SplitTunnelScreen
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
                DashboardScreen(onRequestConsent = ::requestVpnConsent)
            }
        }
    }

    private fun requestVpnConsent(onResult: (Boolean) -> Unit) {
        val intent = Consent.prepare(this)
        if (intent == null) {
            onResult(true) // already granted
        } else {
            pendingConsentCallback = onResult
            vpnPermissionLauncher.launch(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onRequestConsent: (onResult: (Boolean) -> Unit) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val vpnState by VpnManager.state.collectAsState()

    var servers by remember { mutableStateOf<List<VpnServer>>(emptyList()) }
    var selected by remember { mutableStateOf<VpnServer?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSplitTunnel by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var latencies by remember { mutableStateOf<Map<String, Int?>>(emptyMap()) }
    var isFallbackList by remember { mutableStateOf(false) }
    val api = remember { ApiClient() }

    suspend fun loadServers() {
        loading = true
        val remote = withContext(Dispatchers.IO) { api.fetchServers() }
        isFallbackList = remote == null
        servers = remote?.servers ?: defaultServers()
        if (selected == null || servers.none { it.id == selected?.id }) {
            selected = servers.firstOrNull()
        }
        loading = false
        latencies = LatencyProber.probeAll(servers)
        // Auto-select fastest reachable server if user hasn't picked one
        if (servers.none { it.id == selected?.id } || latencies[selected?.id] == null) {
            val fastest = servers.minByOrNull { latencies[it.id] ?: Int.MAX_VALUE }
            if (fastest != null) selected = fastest
        }
    }

    LaunchedEffect(Unit) { loadServers() }

    when {
        showSettings -> {
            SettingsScreen(
                onBack = { showSettings = false },
                onOpenSplitTunnel = { showSplitTunnel = true },
            )
            return
        }
        showSplitTunnel -> {
            SplitTunnelScreen(onBack = { showSplitTunnel = false })
            return
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NexaVPN", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSplitTunnel = true }) {
                        Icon(Icons.Outlined.CompareArrows, contentDescription = "Split tunneling")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp),
        ) {
            ConnectionCard(
                state = vpnState,
                server = selected,
                onToggle = {
                    scope.launch {
                        error = null
                        val server = selected ?: return@launch
                        when (vpnState) {
                            TunnelState.DISCONNECTED -> {
                                onRequestConsent { granted ->
                                    if (granted) {
                                        scope.launch {
                                            val result = VpnManager.connect(context, server, api)
                                            result.onFailure { error = it.message }
                                        }
                                    } else {
                                        error = "VPN permission is required to connect"
                                    }
                                }
                            }
                            else -> {
                                val result = VpnManager.disconnect(context)
                                result.onFailure { error = it.message }
                            }
                        }
                    }
                },
            )

            error?.let { msg ->
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
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        )
                        IconButton(onClick = { error = null }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Servers",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    )
                    if (isFallbackList) {
                        Text(
                            "Offline — showing demo servers",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = { scope.launch { loadServers() } }) {
                    Text("Refresh")
                }
            }

            when {
                loading -> Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                servers.isEmpty() -> Text(
                    "No servers available",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(servers) { s ->
                        ServerRow(
                            server = s,
                            selected = s.id == selected?.id,
                            enabled = vpnState == TunnelState.DISCONNECTED,
                            latencyMs = latencies[s.id],
                            onClick = { selected = s },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionCard(state: TunnelState, server: VpnServer?, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                TunnelState.CONNECTED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                TunnelState.CONNECTING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                TunnelState.DISCONNECTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when (state) {
                    TunnelState.DISCONNECTED -> "Not Protected"
                    TunnelState.CONNECTING -> "Connecting…"
                    TunnelState.CONNECTED -> "Protected"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = server?.let { "${it.city}, ${it.countryCode}" } ?: "Select a server",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onToggle,
                enabled = state != TunnelState.CONNECTING && server != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    when (state) {
                        TunnelState.DISCONNECTED -> "Connect"
                        TunnelState.CONNECTING -> "Cancel"
                        TunnelState.CONNECTED -> "Disconnect"
                    },
                    fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
fun ServerRow(server: VpnServer, selected: Boolean, enabled: Boolean, latencyMs: Int?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(server.country, fontWeight = FontWeight.SemiBold)
                Text(
                    server.city,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
            latencyMs?.let { ms ->
                val (color, label) = when {
                    ms < 100 -> MaterialTheme.colorScheme.tertiary to "$ms ms"
                    ms < 250 -> MaterialTheme.colorScheme.secondary to "$ms ms"
                    else -> MaterialTheme.colorScheme.error to "$ms ms"
                }
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = color,
                )
            }
            if (server.premium) {
                AssistChip(onClick = {}, label = { Text("PRO") })
            }
        }
    }
}

fun defaultServers(): List<VpnServer> = listOf(
    // Placeholder list shown only when the backend is unreachable.
    VpnServer("fallback-sg", "Singapore (demo)", "SG", "Singapore", "fallback.invalid", "PLACEHOLDER_PUBKEY"),
    VpnServer("fallback-de", "Germany (demo)", "DE", "Frankfurt", "fallback.invalid", "PLACEHOLDER_PUBKEY"),
    VpnServer("fallback-us", "United States (demo)", "US", "Ashburn", "fallback.invalid", "PLACEHOLDER_PUBKEY"),
    VpnServer("fallback-jp", "Japan (demo)", "JP", "Tokyo", "fallback.invalid", "PLACEHOLDER_PUBKEY"),
)
