package com.nexavpn.app.vpn

import android.content.Context
import com.nexavpn.app.NexaApp
import com.nexavpn.app.data.ApiClient
import com.nexavpn.app.data.PeerLease
import com.nexavpn.app.data.SplitTunnelRepository
import com.nexavpn.app.data.VpnServer
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetAddress

enum class TunnelState { DISCONNECTED, CONNECTING, CONNECTED }

/**
 * Owns the active tunnel. UI observes [state]; connect/disconnect are the
 * only transitions, and every path funnels through [setUp] / [tearDown].
 */
object VpnManager {

    private val _state = MutableStateFlow(TunnelState.DISCONNECTED)
    val state: StateFlow<TunnelState> = _state

    private var tunnel: Handle? = null
    private var lastError: String? = null

    @Volatile
    private var userInitiatedDisconnect: Boolean = false
    private var lastContext: android.content.Context? = null
    private var lastServer: VpnServer? = null
    private var lastApi: ApiClient? = null

    fun error(): String? = lastError

    /** True when the tunnel dropped without a user disconnect request. */
    fun wasUnexpectedDrop(): Boolean = !userInitiatedDisconnect

    /** Attempts an automatic reconnect using the last successful parameters. */
    suspend fun reconnect(context: Context) {
        val server = lastServer ?: return
        val api = lastApi ?: return
        userInitiatedDisconnect = false
        connect(context, server, api)
    }

    /**
     * Full connect flow: provision a peer lease on the backend, build a
     * WireGuard config locally with the device's stored private key, and
     * bring the tunnel up through GoBackend.
     */
    suspend fun connect(
        context: Context,
        server: VpnServer,
        api: ApiClient,
    ): Result<Unit> {
        if (_state.value != TunnelState.DISCONNECTED) return Result.success(Unit)
        _state.value = TunnelState.CONNECTING
        lastError = null
        lastContext = context
        lastServer = server
        lastApi = api

        try {
            val backend = requireNotNull(NexaApp.appBackend) { "backend not initialized" }
            val keyPair = KeysStore.getOrCreateKeyPair(context)
            val lease = api.registerPeer(server.id, keyPair.publicKey.toBase64())

            val excluded = SplitTunnelRepository.excludedApps(context).first()
            val config = buildConfig(keyPair.privateKey.toBase64(), lease, excluded)
            val handle = Handle("nexa-${server.id}")
            backend.setState(handle, Tunnel.State.UP, config)
            tunnel = handle
            _state.value = TunnelState.CONNECTED
            return Result.success(Unit)
        } catch (e: Exception) {
            _state.value = TunnelState.DISCONNECTED
            lastError = e.message ?: e.javaClass.simpleName
            return Result.failure(e)
        }
    }

    suspend fun disconnect(context: Context): Result<Unit> {
        userInitiatedDisconnect = true
        val handle = tunnel ?: return Result.success(Unit).also { _state.value = TunnelState.DISCONNECTED }
        return try {
            val backend = requireNotNull(NexaApp.appBackend)
            backend.setState(handle, Tunnel.State.DOWN, null)
            tunnel = null
            _state.value = TunnelState.DISCONNECTED
            Result.success(Unit)
        } catch (e: Exception) {
            lastError = e.message
            Result.failure(e)
        }
    }

    private fun buildConfig(privateKey: String, lease: PeerLease, excludedApps: Set<String>): Config {
        val iface = Interface.Builder()
            .parsePrivateKey(privateKey)
            .addAddress(InetNetwork.parse(lease.address))
            .addDnsServers(lease.dns.map { InetAddress.getByName(it) })
            .apply {
                excludedApps.forEach { excludeApplication(it) }
            }
            .build()

        val peer = Peer.Builder()
            .parsePublicKey(lease.serverPublicKey)
            .addAllowedIp(InetNetwork.parse("0.0.0.0/0"))
            .parseEndpoint(lease.endpoint)
            .setPersistentKeepalive(25)
            .build()

        return Config.Builder()
            .setInterface(iface)
            .addPeer(peer)
            .build()
    }

    private class Handle(private val name: String) : Tunnel {
        override fun getName(): String = name
        override fun onStateChange(newState: Tunnel.State) {
            if (newState == Tunnel.State.DOWN) {
                tunnel = null
                _state.value = TunnelState.DISCONNECTED
            }
        }
    }

    /** Holds a breach state until user reconnects manually. */
    fun acknowledgeBreach() {
        userInitiatedDisconnect = true
    }
}
