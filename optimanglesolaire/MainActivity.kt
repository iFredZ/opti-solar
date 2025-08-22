package com.fredz.optimanglesolaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fredz.optimanglesolaire.ui.AppNav // On importe le nouveau chef d'orchestre
import com.fredz.optimanglesolaire.ui.theme.OptiSolarTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      OptiSolarTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          // On appelle le chef d'orchestre qui gère toute la navigation
          AppNav()
        }
      }
    }
  }
}