package com.example.syncwire.ui.screen

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.syncwire.sync.NotificationRecord
import com.example.syncwire.sync.Settings
import com.example.syncwire.sync.SyncwireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val serverUrl: String = Settings.DEFAULT_SERVER_URL,
    val deviceId: String = "",
    val listenerEnabled: Boolean = false,
    val sending: Boolean = false,
    val loading: Boolean = false,
    val notifications: List<NotificationRecord> = emptyList(),
    val error: String? = null,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = Settings(app)
    private val api = SyncwireApi(
        baseUrlProvider = { settings.getServerUrl() },
        deviceIdProvider = { settings.getOrCreateDeviceId() },
    )

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        ensureChannel(app)
        viewModelScope.launch {
            settings.serverUrlFlow.collect { url ->
                _state.update { it.copy(serverUrl = url) }
            }
        }
        viewModelScope.launch {
            settings.deviceIdFlow.collect { id ->
                _state.update { it.copy(deviceId = id) }
            }
        }
    }

    fun setServerUrl(url: String) {
        viewModelScope.launch {
            settings.setServerUrl(url)
            _state.update { it.copy(serverUrl = url, error = null) }
        }
    }

    fun setListenerEnabled(enabled: Boolean) {
        _state.update { it.copy(listenerEnabled = enabled, error = null) }
    }

    fun sendTest() {
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            val payload = com.example.syncwire.sync.NotificationPayload(
                id = UUID.randomUUID().toString(),
                deviceId = settings.getOrCreateDeviceId(),
                sourceType = "TEST",
                sender = "syncwire-test",
                content = "Hello from SyncWire at ${System.currentTimeMillis()}",
                packageName = "com.example.syncwire",
                timestamp = System.currentTimeMillis(),
            )
            val result = api.postNotification(payload)
            result.onFailure { e ->
                _state.update { it.copy(sending = false, error = e.message ?: "send failed") }
            }.onSuccess {
                _state.update { it.copy(sending = false) }
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = api.listNotifications(limit = 50)
            result.onSuccess { list ->
                _state.update { it.copy(loading = false, notifications = list) }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "load failed") }
            }
        }
    }

    companion object {
        const val LISTENER_CHANNEL_ID = "syncwire_listener"

        fun ensureChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = ctx.getSystemService(NotificationManager::class.java)
                val ch = NotificationChannel(
                    LISTENER_CHANNEL_ID,
                    "SyncWire background service",
                    NotificationManager.IMPORTANCE_LOW,
                )
                nm.createNotificationChannel(ch)
            }
        }
    }
}
