package com.nexavpn.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VpnServer(
    val id: String,
    val country: String,
    @SerialName("country_code") val countryCode: String,
    val city: String,
    val hostname: String,
    @SerialName("public_key") val publicKey: String,
    @SerialName("endpoint_port") val endpointPort: Int = 51820,
    val premium: Boolean = false,
    val load: Float = 0f,
)

@Serializable
data class ServerListResponse(
    val servers: List<VpnServer>,
    @SerialName("updatedAt") val updatedAt: Long = 0,
)

/**
 * Lease returned by the backend after registering the device's public key.
 * Contains everything needed to build a WireGuard config — no private
 * material ever crosses the network.
 */
@Serializable
data class PeerLease(
    val address: String,                              // e.g. "10.66.66.7/32"
    @SerialName("server_public_key") val serverPublicKey: String,
    val endpoint: String,                             // "host:port"
    val dns: List<String> = listOf("1.1.1.1", "1.0.0.1"),
    @SerialName("expires_at") val expiresAt: Long = 0,
)
