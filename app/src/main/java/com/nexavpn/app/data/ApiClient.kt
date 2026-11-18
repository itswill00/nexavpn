package com.nexavpn.app.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration

/**
 * API client for the NexaVPN backend. All calls are synchronous and should
 * be invoked from a coroutine dispatcher (see VpnManager).
 *
 * Throws [ApiException] with a user-actionable message on failure.
 */
class ApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val deviceToken: String? = null,
) {

    private val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** @return servers, or null when the backend is unreachable (offline fallback). */
    fun fetchServers(): ServerListResponse? = runCatching {
        val req = builder("$baseUrl/servers").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            json.decodeFromString<ServerListResponse>(resp.body?.string() ?: return null)
        }
    }.getOrNull()

    /**
     * Registers the device public key on a server; returns a peer lease.
     * @throws ApiException with a specific, user-readable reason.
     */
    fun registerPeer(serverId: String, publicKey: String): PeerLease {
        val payload = json.encodeToString(
            kotlinx.serialization.serializer<Map<String, String>>(),
            mapOf("server_id" to serverId, "public_key" to publicKey),
        )
        val req = builder("$baseUrl/peers")
            .post(okhttp3.RequestBody.create("application/json".toMediaType(), payload))
            .build()

        return try {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                when {
                    resp.isSuccessful && body != null ->
                        json.decodeFromString<PeerLease>(body)
                    resp.code == 401 || resp.code == 403 ->
                        throw ApiException("Access denied (${resp.code}). Check the app's backend token.")
                    resp.code == 404 ->
                        throw ApiException("Server \"$serverId\" is not registered on the backend (404).")
                    resp.code == 507 ->
                        throw ApiException("This server is full (507). Try another location.")
                    else ->
                        throw ApiException("Backend error HTTP ${resp.code}. Try again later.")
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: java.net.UnknownHostException) {
            throw ApiException(
                "Cannot reach the backend host. If this is a fresh install, " +
                    "no backend has been configured for this build yet.",
            )
        } catch (e: java.net.SocketTimeoutException) {
            throw ApiException("Backend timed out. Check your connection and try again.")
        } catch (e: java.io.IOException) {
            throw ApiException("Network error: ${e.javaClass.simpleName}. Check your connection.")
        } catch (e: Exception) {
            throw ApiException("Unexpected error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

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

/** User-actionable API failure. */
class ApiException(message: String) : Exception(message)
