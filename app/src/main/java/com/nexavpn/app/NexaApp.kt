package com.nexavpn.app

import android.app.Application
import com.wireguard.android.backend.Tunnel

class NexaApp : Application(), Tunnel.StateChangeCallback {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStateChanged(state: Tunnel.State) {
        // Surface state changes to the UI layer via shared flow in a follow-up iteration.
    }
}
