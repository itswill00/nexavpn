package com.nexavpn.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.splitTunnelStore by preferencesDataStore(name = "nexa_split_tunnel")

/**
 * Split tunneling: a set of package names to EXCLUDE from the VPN tunnel.
 * Excluded apps use the underlying network directly.
 */
object SplitTunnelRepository {

    private val EXCLUDED = stringSetPreferencesKey("excluded_packages")

    /** Packages selected for exclusion. */
    fun excludedApps(context: Context): Flow<Set<String>> =
        context.splitTunnelStore.data.map { it[EXCLUDED] ?: emptySet() }

    suspend fun setExcluded(context: Context, packages: Set<String>) {
        context.splitTunnelStore.data.edit { it[EXCLUDED] = packages }
    }

    suspend fun toggle(context: Context, packageName: String) {
        val current = excludedApps(context).first()
        val next = if (packageName in current) current - packageName else current + packageName
        setExcluded(context, next)
    }

    data class InstalledApp(
        val packageName: String,
        val label: String,
        val isSystem: Boolean,
    )

    /** User-installed apps (plus internet-capable system apps), sorted by label. */
    fun listInstalledApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val flags = PackageManager.GET_META_DATA
        val apps = pm.getInstalledApplications(flags)
        return apps.asSequence()
            .filter { it.packageName != context.packageName }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map {
                InstalledApp(
                    packageName = it.packageName,
                    label = pm.getApplicationLabel(it).toString(),
                    isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
