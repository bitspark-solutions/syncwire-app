package com.example.syncwire

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.syncwire.ui.screen.HomeScreen
import com.example.syncwire.ui.screen.HomeViewModel
import com.example.syncwire.ui.theme.SyncwireTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HomeViewModel.ensureChannel(applicationContext)
        setContent {
            SyncwireTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        HomeScreen()
                    }
                }
            }
        }
    }
}
