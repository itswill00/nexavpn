package com.nexavpn.app.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration

/**
 * API client for the NexaVPN backend. All calls are synchronous and should
 * be invoked from a coroutine dispatcher (see VpnManager).
 */
class ApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val deviceToken: String? = null,
) {

    private val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun fetchServers(): ServerListResponse? = runCatching {
        val req = builder("$baseUrl/servers").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            json.decodeFromString<ServerListResponse>(resp.body?.string() ?: return null)
        }
    }.getOrNull()

    /** Registers the device public key on a server; returns a peer lease. */
    fun registerPeer(serverId: String, publicKey: String): PeerLease? = runCatching {
        val payload = json.encodeToString(
            kotlinx.serialization.serializer<Map<String, String>>(),
            mapOf("server_id" to serverId, "public_key" to publicKey),
        )
        val req = builder("$baseUrl/peers")
            .post(okhttp3.RequestBody.create("application/json".toMediaType(), payload))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            json.decodeFromString<PeerLease>(resp.body?.string() ?: return null)
        }
    }.getOrNull()

    private fun builder(url: String): Request.Builder {
        val b = Request.Builder().url(url)
        deviceToken?.let { b.header("Authorization", "Bearer $it") }
        return b
    }

    companion object {
        // Point at your deployed backend before building a release.
        const val DEFAULT_BASE_URL = "https://api.nexavpn.example.com"
    }
}
