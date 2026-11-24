package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.data.firebase.configureAppCheck
import com.example.ui.navigation.DatalabsApp
import com.example.ui.theme.DatalabsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val launchIntent = intent
    if (launchIntent?.hasExtra("FIREBASE_APPCHECK_DEBUG_TOKEN") == true) {
      lifecycleScope.launch(Dispatchers.IO) {
        configureAppCheck(applicationContext, launchIntent)
      }
    }
    enableEdgeToEdge()
    setContent {
      DatalabsTheme {
        DatalabsApp()
      }
    }
  }
}
