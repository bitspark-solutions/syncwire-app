package com.syncwire.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private EditText etChannel;

    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) startService();
                        else Toast.makeText(this,
                                "Notification permission required", Toast.LENGTH_SHORT).show();
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        etChannel = findViewById(R.id.etChannel);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop  = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> requestPermissionAndStart());
        btnStop.setOnClickListener(v -> stopService());
    }

    private void requestPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        startService();
    }

    private void startService() {
        Intent intent = new Intent(this, NotificationForegroundService.class);
        intent.setAction(NotificationForegroundService.ACTION_START);

        String channel = etChannel.getText().toString().trim();
        if (!channel.isEmpty()) {
            intent.putExtra(NotificationForegroundService.EXTRA_DEVICE_ID, channel);
        }

        ContextCompat.startForegroundService(this, intent);
        tvStatus.setText(getString(R.string.status_connected));
    }

    private void stopService() {
        Intent intent = new Intent(this, NotificationForegroundService.class);
        intent.setAction(NotificationForegroundService.ACTION_STOP);
        startService(intent);
        tvStatus.setText(getString(R.string.status_disconnected));
    }
}
