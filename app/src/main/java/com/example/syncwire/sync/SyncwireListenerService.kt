package com.example.syncwire.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.syncwire.MainActivity
import com.example.syncwire.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Receives system notifications and forwards each to the SyncWire server.
 *
 * Lifecycle: bound by the system once the user grants Notification Access in
 * Settings > Apps > Special access. No UI interaction needed after that.
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
        ensureForeground()
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
        // Skip our own foreground-service notification.
        if (sbn.packageName == packageName) return
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

    private fun ensureForeground() {
        // Android 14+: any FGS must specify its type. We use dataSync.
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Listening for notifications")
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    companion object {
        private const val TAG = "SyncwireListener"
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "syncwire_listener"
    }
}
