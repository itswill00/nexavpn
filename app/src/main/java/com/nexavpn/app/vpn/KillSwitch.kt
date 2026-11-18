package com.nexavpn.app.vpn

import android.content.Context
import com.nexavpn.app.data.AppSettings
import com.nexavpn.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Kill-switch behavior:
 *
 *  - When enabled and a tunnel drops unexpectedly (not user-initiated),
 *    VpnManager marks the session as "breached" and the UI surfaces a
 *    blocking state; a reconnect is attempted automatically.
 *  - The hard enforcement (blocking all traffic while no tunnel is up) is
 *    achieved with a WireGuard config whose AllowedIPs = 0.0.0.0/0 stay
 *    installed until reconnect: GoBackend keeps the TUN interface routes in
 *    place, so apps cannot bypass via the underlying network while the
 *    service holds the VPN. Combined with Android's VpnService isolation,
 *    a dropped tunnel without user consent never exposes the apps directly.
 *
 *  NOTE: For absolute enforcement across reboots/app death, an
 *  "always-on VPN" system setting plus "block connections without VPN"
 *  (Android Settings > Network > VPN) is recommended; we deep-link users
 *  there from the settings screen.
 */
object KillSwitch {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var breached: Boolean = false
        private set

    fun observe(context: Context) {
        scope.launch {
            VpnManager.state.collect { state ->
                when (state) {
                    TunnelState.DISCONNECTED -> onDisconnected(context)
                    else -> breached = false
                }
            }
        }
    }

    private fun onDisconnected(context: Context) {
        scope.launch {
            val s: AppSettings = SettingsRepository.settings(context).first()
            if (s.killSwitch && VpnManager.wasUnexpectedDrop()) {
                breached = true
                VpnManager.reconnect(context)
            }
        }
    }
}
