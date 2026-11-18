package com.nexavpn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexavpn.app.data.ApiClient
import com.nexavpn.app.data.VpnServer
import com.nexavpn.app.ui.theme.NexaTheme
import com.nexavpn.app.vpn.VpnController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexaTheme {
                DashboardScreen()
            }
        }
    }
}

enum class TunnelState { DISCONNECTED, CONNECTING, CONNECTED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val scope = rememberCoroutineScope()
    var servers by remember { mutableStateOf<List<VpnServer>>(emptyList()) }
    var selected by remember { mutableStateOf<VpnServer?>(null) }
    var state by remember { mutableStateOf(TunnelState.DISCONNECTED) }
    var error by remember { mutableStateOf<String?>(null) }
    val api = remember { ApiClient() }

    LaunchedEffect(Unit) {
        // Try backend, fall back to demo server entries
        servers = api.fetchServers()?.servers ?: defaultServers()
        if (selected == null) selected = servers.firstOrNull()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NexaVPN", fontWeight = FontWeight.Bold) },
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
                state = state,
                server = selected,
                onToggle = {
                    scope.launch {
                        error = null
                        when (state) {
                            TunnelState.DISCONNECTED -> {
                                state = TunnelState.CONNECTING
                                // Real connection requires provisioning a peer + user consent
                                // (VpnService.prepare). Scaffolded here; see README.
                                state = TunnelState.CONNECTED
                            }
                            else -> state = TunnelState.DISCONNECTED
                        }
                    }
                },
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }

            Text(
                "Servers",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(servers) { s ->
                    ServerRow(
                        server = s,
                        selected = s.id == selected?.id,
                        onClick = { selected = s },
                    )
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
            containerColor = if (state == TunnelState.CONNECTED)
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
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
fun ServerRow(server: VpnServer, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
            if (server.premium) {
                AssistChip(onClick = {}, label = { Text("PRO") })
            }
        }
    }
}

fun defaultServers(): List<VpnServer> = listOf(
    VpnServer("sg-1", "Singapore", "SG", "Singapore", "sg1.nexavpn.example.com", "PLACEHOLDER_PUBKEY"),
    VpnServer("de-1", "Germany", "DE", "Frankfurt", "de1.nexavpn.example.com", "PLACEHOLDER_PUBKEY"),
    VpnServer("us-1", "United States", "US", "Ashburn", "us1.nexavpn.example.com", "PLACEHOLDER_PUBKEY"),
    VpnServer("jp-1", "Japan", "JP", "Tokyo", "jp1.nexavpn.example.com", "PLACEHOLDER_PUBKEY"),
)
