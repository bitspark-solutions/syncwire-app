package com.example.syncwire.sync

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Receives system notifications and forwards each to the SyncWire server.
 *
 * Lifecycle: bound by the system once the user grants Notification Access in
 * Settings > Apps > Special access > Notification access. The system keeps
 * this service alive as long as access is granted, so no foreground-service
 * notification is required for M1. (M3 will add a dataSync FGS for the
 * offline outbox + reliability work.)
 *
 * Each notification is POSTed as JSON to {serverUrl}/notifications. On
 * failure we log and drop (M1); M3 adds a Room outbox + retry.
 */
class SyncwireListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settings: Settings
    private lateinit var api: SyncwireApi

    override fun onCreate() {
        super.onCreate()
        settings = Settings(applicationContext)
        api = SyncwireApi(
            baseUrlProvider = { settings.getServerUrl() },
            deviceIdProvider = { settings.getOrCreateDeviceId() },
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected — user may have revoked access")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // We don't skip our own package — the Send-Test button posts a real
        // system notification to exercise the full listener→server path.
        // NotificationManager.notify() doesn't recurse, so this is safe.
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val sender = if (title.isNotBlank()) title else sbn.packageName

        val payload = NotificationPayload(
            id = UUID.randomUUID().toString(),
            deviceId = "", // filled at POST time from settings (deferred)
            sourceType = "NOTIFICATION",
            sender = sender,
            content = text,
            packageName = sbn.packageName,
            timestamp = sbn.postTime,
        )

        scope.launch {
            val deviceId = settings.getOrCreateDeviceId()
            val withDevice = payload.copy(deviceId = deviceId)
            api.postNotification(withDevice)
                .onFailure { Log.w(TAG, "POST failed for ${withDevice.id}", it) }
                .onSuccess { Log.d(TAG, "Forwarded ${withDevice.id} (${sbn.packageName})") }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    companion object {
        private const val TAG = "SyncwireListener"
    }
}
