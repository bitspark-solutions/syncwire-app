package com.example.syncwire.ui.screen

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.syncwire.MainActivity
import com.example.syncwire.sync.NotificationRecord
import com.example.syncwire.sync.Settings
import com.example.syncwire.sync.SyncwireApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        ensureTestChannel(app)
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

    /**
     * Posts a real system notification via NotificationManager. The OS
     * delivers it to our NotificationListenerService, which forwards it
     * to the SyncWire server — exercising the full listener path.
     *
     * On API 33+ we also need POST_NOTIFICATIONS at runtime to show
     * anything in the shade. If it's not granted, we surface a clear error.
     */
    fun sendTest() {
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            val ctx = getApplication<Application>()
            val nm = ctx.getSystemService(NotificationManager::class.java)

            // API 33+ runtime permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ctx.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    _state.update {
                        it.copy(
                            sending = false,
                            error = "POST_NOTIFICATIONS not granted — open the app once and accept the prompt",
                        )
                    }
                    return@launch
                }
            }

            val pi = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notif = NotificationCompat.Builder(ctx, TEST_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
                .setContentTitle("SyncWire test")
                .setContentText("Hello from SyncWire at ${System.currentTimeMillis()}")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Hello from SyncWire at ${System.currentTimeMillis()}"))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            nm.notify(NOTIF_ID, notif)

            // Give the listener a beat to forward, then refresh the list.
            // The refresh is best-effort — even if it fails, the POST happened.
            delay(800)
            _state.update { it.copy(sending = false) }
            refresh()
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
        private const val TEST_CHANNEL_ID = "syncwire_test"
        private const val NOTIF_ID = 1001

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

        private fun ensureTestChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = ctx.getSystemService(NotificationManager::class.java)
                val ch = NotificationChannel(
                    TEST_CHANNEL_ID,
                    "SyncWire tests",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
                ch.description = "Notifications produced by the Send-Test button"
                nm.createNotificationChannel(ch)
            }
        }
    }
}
