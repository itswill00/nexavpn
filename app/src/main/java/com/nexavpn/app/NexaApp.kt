package com.nexavpn.app

import android.app.Application
import com.wireguard.android.backend.GoBackend

class NexaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // GoBackend starts its own VpnService when a tunnel is brought up.
        appBackend = GoBackend(this)
    }

    companion object {
        @Volatile
        var appBackend: GoBackend? = null
            private set
    }
}
