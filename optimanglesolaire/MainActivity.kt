package com.fredz.optimanglesolaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fredz.optimanglesolaire.ui.AppNav
import com.fredz.optimanglesolaire.ui.theme.OptiSolarTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      // ON ENVELOPPE L'APPLICATION DANS NOTRE THÈME
      OptiSolarTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AppNav()
        }
      }
    }
  }
}