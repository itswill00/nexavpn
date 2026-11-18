package com.nexavpn.app.vpn

import com.nexavpn.app.NexaApp
import com.wireguard.android.backend.Statistics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Live session numbers shown under the connect button while protected. */
data class SessionStats(
    val secondsConnected: Long = 0,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val handshakeAtEpochMs: Long? = null,
)

object SessionMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _stats = MutableStateFlow(SessionStats())
    val stats: StateFlow<SessionStats> = _stats

    private var connectStart: Long = 0

    fun onTunnelUp() {
        connectStart = System.currentTimeMillis()
        scope.launch {
            while (isActive && VpnManager.state.value == TunnelState.CONNECTED) {
                _stats.value = sample()
                delay(1000)
            }
        }
    }

    fun reset() {
        _stats.value = SessionStats()
    }

    private fun sample(): SessionStats {
        val backend = NexaApp.appBackend ?: return _stats.value
        val tunnel = VpnManager.activeHandle() ?: return _stats.value
        val s: Statistics = runCatching { backend.getStatistics(tunnel) }.getOrNull()
            ?: return _stats.value.copy(
                secondsConnected = (System.currentTimeMillis() - connectStart) / 1000,
            )
        var rx = 0L
        var tx = 0L
        var handshake: Long? = null
        for (peer in s.peers()) {
            val ps = s.peer(peer) ?: continue
            rx += ps.rxBytes()
            tx += ps.txBytes()
            handshake = ps.latestHandshakeEpochMillis().takeIf { it > 0 } ?: handshake
        }
        return SessionStats(
            secondsConnected = (System.currentTimeMillis() - connectStart) / 1000,
            rxBytes = rx,
            txBytes = tx,
            handshakeAtEpochMs = handshake,
        )
    }
}
