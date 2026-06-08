package com.syncwire.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

/**
 * Android foreground service that maintains a persistent SignalR connection
 * to the SyncWire backend hub and surfaces incoming notifications to the user.
 */
public class NotificationForegroundService extends Service {

    private static final String TAG = "SyncWireFGService";
    public static final String CHANNEL_SERVICE = "syncwire_service";
    public static final String CHANNEL_NOTIFICATIONS = "syncwire_notifications";

    public static final String ACTION_START = "com.syncwire.app.START";
    public static final String ACTION_STOP  = "com.syncwire.app.STOP";
    public static final String EXTRA_DEVICE_ID = "device_id";

    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    private HubConnection hubConnection;
    private String deviceId;

    // -------------------------------------------------------------------------
    // Service lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopForegroundService();
            return START_NOT_STICKY;
        }

        deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
        }

        startForeground(FOREGROUND_NOTIFICATION_ID, buildServiceNotification());
        connectToHub();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        disconnectFromHub();
        super.onDestroy();
    }

    // -------------------------------------------------------------------------
    // SignalR connection
    // -------------------------------------------------------------------------

    private void connectToHub() {
        String hubUrl = BuildConfig.SIGNALR_HUB_URL;
        Log.i(TAG, "Connecting to hub: " + hubUrl);

        hubConnection = HubConnectionBuilder.create(hubUrl).build();

        hubConnection.on("ReceiveNotification", (notification) -> {
            Log.d(TAG, "Received notification: " + notification);
            showNotification(notification);
        }, SyncWireNotification.class);

        hubConnection.onClosed(error -> {
            Log.w(TAG, "Hub connection closed. Error: " + error);
            scheduleReconnect();
        });

        hubConnection.start()
                .doOnComplete(() -> {
                    Log.i(TAG, "Connected to hub");
                    registerDevice();
                })
                .doOnError(error -> Log.e(TAG, "Hub connection failed", error))
                .subscribe();
    }

    private void registerDevice() {
        if (hubConnection != null
                && hubConnection.getConnectionState() == HubConnectionState.CONNECTED
                && deviceId != null) {
            // Join a device-specific channel so targeted notifications are delivered
            hubConnection.send("JoinChannel", deviceId);
            Log.i(TAG, "Registered with channel: " + deviceId);
        }
    }

    private void disconnectFromHub() {
        if (hubConnection != null) {
            hubConnection.stop().subscribe();
            hubConnection = null;
        }
    }

    private void scheduleReconnect() {
        // Simple back-off: retry after 5 seconds
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(this::connectToHub, 5_000);
    }

    private void stopForegroundService() {
        disconnectFromHub();
        stopForeground(true);
        stopSelf();
    }

    // -------------------------------------------------------------------------
    // Notification helpers
    // -------------------------------------------------------------------------

    private void createNotificationChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);

        // Persistent service channel (low importance – silent)
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_SERVICE,
                "SyncWire Service",
                NotificationManager.IMPORTANCE_LOW);
        serviceChannel.setDescription("Keeps the SyncWire connection alive");
        nm.createNotificationChannel(serviceChannel);

        // Incoming notification channel (high importance – makes sound)
        NotificationChannel notifChannel = new NotificationChannel(
                CHANNEL_NOTIFICATIONS,
                "SyncWire Notifications",
                NotificationManager.IMPORTANCE_HIGH);
        notifChannel.setDescription("Incoming real-time notifications from SyncWire");
        nm.createNotificationChannel(notifChannel);
    }

    private Notification buildServiceNotification() {
        Intent stopIntent = new Intent(this, NotificationForegroundService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_SERVICE)
                .setContentTitle("SyncWire")
                .setContentText("Listening for notifications…")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(openPi)
                .addAction(android.R.drawable.ic_delete, "Stop", stopPi)
                .setOngoing(true)
                .build();
    }

    private void showNotification(SyncWireNotification n) {
        NotificationManager nm = getSystemService(NotificationManager.class);

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        int priority = switch (n.priority) {
            case "High" -> NotificationCompat.PRIORITY_HIGH;
            case "Low"  -> NotificationCompat.PRIORITY_LOW;
            default     -> NotificationCompat.PRIORITY_DEFAULT;
        };

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_NOTIFICATIONS)
                .setContentTitle(n.title)
                .setContentText(n.body)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setPriority(priority)
                .setContentIntent(openPi)
                .setAutoCancel(true)
                .build();

        int notifId = (int) System.currentTimeMillis();
        nm.notify(notifId, notification);

        // Acknowledge receipt via the hub
        if (hubConnection != null
                && hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("Acknowledge", n.id);
        }
    }
}
