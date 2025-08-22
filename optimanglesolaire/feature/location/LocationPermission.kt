package com.fredz.optimanglesolaire.feature.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberFineLocationPermission(): Pair<Boolean, () -> Unit> {
  val ctx = LocalContext.current
  var granted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
    )
  }
  val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { ok -> granted = ok }
  return Pair(granted) { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
}
