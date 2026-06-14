package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.CashbackMainScreen
import com.example.ui.CashbackViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: CashbackViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Schedule monthly reminder alarms on startup only if notifications were enabled by user
    val prefs = getSharedPreferences("cashback_prefs", android.content.Context.MODE_PRIVATE)
    if (prefs.getBoolean("notifications_enabled", false)) {
        com.example.notification.CashbackNotificationReceiver.scheduleMonthlyAlarm(this)
    }

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          CashbackMainScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

