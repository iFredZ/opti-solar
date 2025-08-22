package com.fredz.optimanglesolaire.feature.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Retourne (granted, ask). granted=true si COARSE ou FINE est accordée.
 */
@Composable
fun rememberLocationPermission(): Pair<Boolean, () -> Unit> {
  val ctx = LocalContext.current
  var granted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    )
  }
  val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { res ->
    granted = (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
              (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
  }

  val ask: () -> Unit = {
    launcher.launch(arrayOf(
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ))
  }
  return Pair(granted, ask)
}
