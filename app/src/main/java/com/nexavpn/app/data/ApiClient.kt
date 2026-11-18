package com.nexavpn.app.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration

/**
 * API client for the NexaVPN backend (see /backend).
 * Falls back to bundled defaults when the backend is unreachable.
 */
class ApiClient(private val baseUrl: String = DEFAULT_BASE_URL) {

    private val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun fetchServers(): ServerListResponse? = runCatching {
        val req = Request.Builder().url("$baseUrl/servers").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            json.decodeFromString<ServerListResponse>(body)
        }
    }.getOrNull()

    /** Ask backend to register this device's public key on a server and get a wg config. */
    fun registerPeer(serverId: String, publicKey: String): PeerRegistration? = runCatching {
        val payload = json.encodeToString(
            kotlinx.serialization.serializer<Map<String, String>>(),
            mapOf("server_id" to serverId, "public_key" to publicKey),
        )
        val req = Request.Builder()
            .url("$baseUrl/peers")
            .post(okhttp3.RequestBody.create("application/json".toMediaType(), payload))
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            json.decodeFromString<PeerRegistration>(body)
        }
    }.getOrNull()

    companion object {
        // Change to your deployed backend URL
        const val DEFAULT_BASE_URL = "https://api.nexavpn.example.com"
    }
}

@Serializable
data class PeerRegistration(
    val config: String,
    val expiresAt: Long = 0,
)
