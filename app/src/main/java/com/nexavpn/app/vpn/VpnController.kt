package com.nexavpn.app.vpn

import android.content.Context
import android.content.Intent
import com.wireguard.crypto.KeyPair

/**
 * Thin controller between UI and TunnelService. Owns peer key generation
 * and passing wg-quick config text to the service.
 */
object VpnController {

    fun ensurePeerKeys(context: Context): KeyPair {
        // Keys are generated fresh each session in this iteration; persist in Datastore later.
        return KeyPair()
    }

    fun start(context: Context, wgQuickConfig: String) {
        val intent = Intent(context, TunnelService::class.java).apply {
            putExtra(TunnelService.EXTRA_CONFIG, wgQuickConfig)
        }
        context.startService(intent)
    }

    fun stop(context: Context) {
        context.startService(Intent(context, TunnelService::class.java))
    }
}
