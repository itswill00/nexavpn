package com.nexavpn.app.vpn

import android.content.Intent
import android.net.VpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.BufferedReader

/**
 * WireGuard-based VPN service. Clean-room implementation using the official
 * wireguard-android library (wireguard-go under the hood) — the same engine
 * family observed in the reference app, but original code.
 *
 * GoBackend, when constructed inside a running VpnService, uses that
 * service's Builder to establish the tunnel, so no manual fd handling is
 * needed here.
 */
class TunnelService : VpnService(), Tunnel {

    private val backend: GoBackend by lazy { GoBackend(this) }
    private var active: TunnelHandle? = null

    class TunnelHandle(private val name: String, val config: Config) : Tunnel {
        override fun getName(): String = name
        override fun onStateChange(newState: Tunnel.State) {}
    }

    override fun onRevoke() {
        active = null
        stopSelf()
    }

    override fun onStateChange(state: Tunnel.State) {
        // TODO: broadcast to UI via a shared flow
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cfgText = intent?.getStringExtra(EXTRA_CONFIG)
        if (cfgText != null) {
            runCatching { startTunnel(cfgText) }
                .onFailure { stopSelf() }
        } else {
            stopTunnel()
        }
        return START_NOT_STICKY
    }

    private fun startTunnel(cfgText: String) {
        val config = Config.parse(BufferedReader(cfgText.reader()))
        val handle = TunnelHandle("nexavpn0", config)
        backend.setState(handle, Tunnel.State.UP)
        active = handle
    }

    private fun stopTunnel() {
        active?.let { runCatching { backend.setState(it, Tunnel.State.DOWN) } }
        active = null
        stopSelf()
    }

    companion object {
        const val EXTRA_CONFIG = "config"
    }
}
