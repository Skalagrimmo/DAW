package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DAWViewModel
import com.example.ui.screens.MainStudioScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private var dawViewModel: DAWViewModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val vm: DAWViewModel = viewModel()
        dawViewModel = vm
        MainStudioScreen(viewModel = vm)
      }
    }
  }

  override fun onStop() {
    super.onStop()
    // Life cycle safety: Stop the synthesiser threads when app is backgrounded
    dawViewModel?.stopPlayback()
  }
}
