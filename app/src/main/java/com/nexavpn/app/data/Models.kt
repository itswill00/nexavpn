package com.nexavpn.app.data

import kotlinx.serialization.Serializable

@Serializable
data class VpnServer(
    val id: String,
    val country: String,
    val countryCode: String,
    val city: String,
    val hostname: String,
    val publicKey: String,
    val endpointPort: Int = 51820,
    val premium: Boolean = false,
    val load: Float = 0f,
)

@Serializable
data class ServerListResponse(
    val servers: List<VpnServer>,
    val updatedAt: Long = 0,
)
