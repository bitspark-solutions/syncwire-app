package com.example.syncwire.ui.screen

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.syncwire.sync.NotificationRecord

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    // Local edit buffer for the URL field — committed on "Save".
    var urlDraft by remember { mutableStateOf(state.serverUrl) }
    LaunchedEffect(state.serverUrl) { urlDraft = state.serverUrl }

    // Poll whether the system has bound our NotificationListenerService.
    LaunchedEffect(Unit) {
        while (true) {
            vm.setListenerEnabled(isListenerEnabled(ctx))
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("SyncWire", style = MaterialTheme.typography.headlineMedium)
        Text("Forward every phone notification to the server.", style = MaterialTheme.typography.bodyMedium)

        // Server URL
        OutlinedTextField(
            value = urlDraft,
            onValueChange = { urlDraft = it },
            label = { Text("Server URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.setServerUrl(urlDraft) }) { Text("Save") }
            TextButton(onClick = { urlDraft = state.serverUrl }) { Text("Reset") }
        }
        Text(
            "Default for emulator: http://10.0.2.2:18080/api",
            style = MaterialTheme.typography.bodySmall,
        )

        HorizontalDivider()

        // Listener
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Notification access", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (state.listenerEnabled)
                        "Enabled — notifications will be forwarded."
                    else
                        "Disabled. Grant access to start forwarding.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openNotificationAccessSettings(ctx) }) {
                        Text("Open settings")
                    }
                    TextButton(onClick = { vm.refresh() }) { Text("Check") }
                }
                Text(
                    "Device id: ${state.deviceId.ifBlank { "(generating on first send)" }}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Test + refresh
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.sendTest() },
                enabled = !state.sending && state.deviceId.isNotBlank(),
            ) { Text(if (state.sending) "Sending…" else "Send test") }
            TextButton(onClick = { vm.refresh() }) { Text("Refresh list") }
        }

        state.error?.let { msg ->
            Text("Error: $msg", color = MaterialTheme.colorScheme.error)
        }

        HorizontalDivider()

        Text(
            "Last ${state.notifications.size} notifications",
            style = MaterialTheme.typography.titleMedium,
        )

        if (state.notifications.isEmpty()) {
            Text(
                "Nothing yet. Send a test or trigger a real notification.",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.notifications, key = { it.id }) { n -> NotificationRow(n) }
            }
        }
    }

    // Refresh on first composition.
    LaunchedEffect(Unit) { vm.refresh() }
}

@Composable
private fun NotificationRow(n: NotificationRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(n.sender, style = MaterialTheme.typography.titleSmall)
            Text(n.content, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${n.sourceType} • ${n.packageName}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                n.receivedAt,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun openNotificationAccessSettings(ctx: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(intent)
}

private fun isListenerEnabled(ctx: Context): Boolean {
    val flat = android.provider.Settings.Secure.getString(
        ctx.contentResolver,
        "enabled_notification_listeners",
    ) ?: return false
    return flat.split(":").any { it.startsWith(ctx.packageName + "/") }
}
