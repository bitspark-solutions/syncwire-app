package com.example.syncwire.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "syncwire_prefs")

/**
 * DataStore-backed settings. Two values:
 *  - serverUrl  base URL of the SyncWire server, e.g. "http://10.0.2.2:18080/api"
 *  - deviceId   opaque per-install identifier; generated lazily on first access
 */
class Settings(private val context: Context) {

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val DEVICE_ID = stringPreferencesKey("device_id")
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:18080/api"
    }

    val serverUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SERVER_URL] ?: DEFAULT_SERVER_URL
    }

    val deviceIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DEVICE_ID] ?: ""
    }

    suspend fun getServerUrl(): String = serverUrlFlow.first()

    /**
     * Returns the existing deviceId or generates + persists a new one. M1: this
     * is a local UUID. M2 will replace it with a server-issued id from
     * POST /api/devices.
     */
    suspend fun getOrCreateDeviceId(): String {
        val current = deviceIdFlow.first()
        if (current.isNotBlank()) return current
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { it[DEVICE_ID] = newId }
        return newId
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL] = url }
    }
}
