package com.nexavpn.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "nexa_settings")

/** App-level settings persisted locally. */
data class AppSettings(
    val killSwitch: Boolean = false,
    val autoConnect: Boolean = false,
)

object SettingsRepository {

    private val KILL_SWITCH = booleanPreferencesKey("kill_switch")
    private val AUTO_CONNECT = booleanPreferencesKey("auto_connect")

    fun settings(context: Context): Flow<AppSettings> =
        context.settingsStore.data.map {
            AppSettings(
                killSwitch = it[KILL_SWITCH] ?: false,
                autoConnect = it[AUTO_CONNECT] ?: false,
            )
        }

    suspend fun setKillSwitch(context: Context, enabled: Boolean) {
        context.settingsStore.edit { it[KILL_SWITCH] = enabled }
    }

    suspend fun setAutoConnect(context: Context, enabled: Boolean) {
        context.settingsStore.edit { it[AUTO_CONNECT] = enabled }
    }
}
