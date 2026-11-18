package com.nexavpn.app.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferences_key
import androidx.datastore.preferences.preferencesDataStore
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.keyStore by preferencesDataStore(name = "nexa_keys")

/**
 * Persists this device's WireGuard keypair locally. The private key never
 * leaves the device — only the public key is transmitted during provisioning.
 */
object KeysStore {

    private val PRIVATE_KEY = stringPreferences_key("wg_private_key")

    suspend fun getOrCreatePrivateKey(context: Context): String {
        val existing = context.keyStore.data
            .map { prefs -> prefs[PRIVATE_KEY] }
            .first()
        if (existing != null) return existing

        val fresh = KeyPair().privateKey.toBase64()
        context.keyStore.data.edit { it[PRIVATE_KEY] = fresh }
        return fresh
    }

    suspend fun getOrCreateKeyPair(context: Context): KeyPair {
        return KeyPair(getOrCreatePrivateKey(context))
    }

    suspend fun publicKey(context: Context): String {
        return getOrCreateKeyPair(context).publicKey.toBase64()
    }
}

/** Helper for the system VPN consent dialog. */
object Consent {
    /** Returns an intent to launch for user consent, or null if already granted. */
    fun prepare(context: Context): Intent? = VpnService.prepare(context)
}
