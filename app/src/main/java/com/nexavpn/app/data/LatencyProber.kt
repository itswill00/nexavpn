package com.nexavpn.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Measures server reachability/latency by opening a TCP connection to the
 * endpoint port. UDP (WireGuard's transport) can't be probed reliably
 * without the handshake keys, so TCP connect time is used as a proxy.
 */
object LatencyProber {

    private const val TIMEOUT_MS = 2500

    /** Returns latency in ms, or null when unreachable. */
    suspend fun probe(hostname: String, port: Int): Int? = withTimeoutOrNull(TIMEOUT_MS + 500L) {
        coroutineScope {
            async(Dispatchers.IO) {
                val start = System.nanoTime()
                try {
                    Socket().use {
                        it.connect(InetSocketAddress(hostname, port), TIMEOUT_MS)
                    }
                    ((System.nanoTime() - start) / 1_000_000).toInt()
                } catch (_: Exception) {
                    null
                }
            }.await()
        }
    }

    /** Probes all servers concurrently; returns map id -> ms (null = unreachable). */
    suspend fun probeAll(servers: List<VpnServer>): Map<String, Int?> = coroutineScope {
        val deferreds = servers.map { s ->
            async { s.id to probe(s.hostname, s.endpointPort) }
        }
        deferreds.associate { deferred -> deferred.await() }
    }
}
