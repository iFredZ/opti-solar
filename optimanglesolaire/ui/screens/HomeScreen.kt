@file:OptIn(ExperimentalMaterial3Api::class)

package com.fredz.optimanglesolaire.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fredz.optimanglesolaire.feature.home.EntryMode
import com.fredz.optimanglesolaire.feature.home.HomeViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.DecimalFormat
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  onGoToEstimation: (lat: String, lon: String, tilt: String, az: String) -> Unit
) {
  val context = LocalContext.current
  val locationClient = LocationServices.getFusedLocationProviderClient(context)
  val view = LocalView.current

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
    onResult = { permissions ->
      if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
          .addOnSuccessListener { location ->
            if (location != null) {
              viewModel.onLocationResult(location.latitude, location.longitude)
            } else { viewModel.onLocationError() }
          }
          .addOnFailureListener { viewModel.onLocationError() }
      } else { viewModel.onLocationError() }
    }
  )

  if (viewModel.showDatePicker) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { viewModel.onDatePickerDismissed() },
      confirmButton = { Button(onClick = { datePickerState.selectedDateMillis?.let { viewModel.onDateSelected(it) } }) { Text("OK") } },
      dismissButton = { Button(onClick = { viewModel.onDatePickerDismissed() }) { Text("Annuler") } }
    ) { DatePicker(state = datePickerState) }
  }

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // La colonne principale qui ne scrolle PAS
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      AnimatedVisibility(visible = viewModel.entryMode != EntryMode.SENSORS) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min)
          ) {
            Column(
              modifier = Modifier.fillMaxHeight(),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("Loc.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("Latitude", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
              value = viewModel.latitudeText,
              onValueChange = { viewModel.onLatitudeChanged(it) },
              modifier = Modifier.width(150.dp),
              textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 22.sp),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            GpsButton(onClick = {
              view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
              viewModel.onLocationRequested()
              locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
              )
            })
          }
          Spacer(Modifier.height(8.dp))
          Text(
            text = viewModel.selectedDate,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
              .clip(MaterialTheme.shapes.medium)
              .clickable { viewModel.onDateClicked() }
              .padding(horizontal = 16.dp, vertical = 8.dp)
          )
        }
      }

      Spacer(Modifier.height(16.dp))

      CockpitDisplay(
        angle = viewModel.solarAngle
      )

      Spacer(Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
      ) {
        ModeButton(
          text = "Utiliser Capteurs",
          isSelected = viewModel.entryMode == EntryMode.SENSORS,
          onClick = { viewModel.onEntryModeSelected(EntryMode.SENSORS, context) },
          modifier = Modifier.weight(1f)
        )
        ModeButton(
          text = "Saisie Manuelle",
          isSelected = viewModel.entryMode == EntryMode.MANUAL,
          onClick = { viewModel.onEntryModeSelected(EntryMode.MANUAL, context) },
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(Modifier.height(16.dp))

      AnimatedVisibility(visible = viewModel.entryMode == EntryMode.SENSORS) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          SensorReadoutPanel(
            tilt = viewModel.measuredTilt,
            azimuth = viewModel.measuredAzimuth,
            numericTilt = viewModel.numericTilt ?: 0f,
            numericAzimuth = viewModel.numericAzimuth ?: 0f
          )
          Spacer(Modifier.height(16.dp))
          MemorizeButton(
            showSuccess = viewModel.showMemorizeSuccess,
            onClick = {
              view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
              viewModel.onMemorizeClicked()
            }
          )
        }
      }

      AnimatedVisibility(visible = viewModel.entryMode == EntryMode.MANUAL) {
        ManualEntryPanel(
          tilt = viewModel.manualTiltText,
          onTiltChange = { viewModel.onManualTiltChanged(it) },
          azimuth = viewModel.manualAzimuthText,
          onAzimuthChange = { viewModel.onManualAzimuthChanged(it) }
        )
      }
    }

    Spacer(Modifier.weight(1f))

    Button(
      onClick = {
        val tilt: String
        val azimuth: String
        if (viewModel.entryMode == EntryMode.SENSORS) {
          val deviation = (viewModel.memorizedNumericAzimuth ?: 0f) - 180f
          tilt = DecimalFormat("0").format(viewModel.memorizedNumericTilt)
          azimuth = DecimalFormat("0").format(deviation)
        } else {
          tilt = viewModel.manualTiltText
          azimuth = viewModel.manualAzimuthText
        }
        onGoToEstimation(viewModel.latitudeText, viewModel.numericLongitude?.toString() ?: "", tilt, azimuth)
      },
      shape = CircleShape,
      modifier = Modifier.fillMaxWidth().height(50.dp),
      enabled = viewModel.isEstimateButtonEnabled,
      colors = ButtonDefaults.buttonColors(
        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
      )
    ) {
      Text("Estimer la Production", fontSize = 18.sp)
    }
  }
}

@Composable
fun GpsButton(onClick: () -> Unit) {
  FloatingActionButton(
    onClick = onClick,
    shape = CircleShape,
    modifier = Modifier.size(52.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.primary,
    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
  ) {
    Icon(Icons.Default.GpsFixed, contentDescription = "Obtenir la position GPS")
  }
}

@Composable
fun CockpitDisplay(angle: String) {
  val cockpitColor = MaterialTheme.colorScheme.secondary
  Box(
    modifier = Modifier
      .size(170.dp)
      .drawBehind {
        drawCircle(color = cockpitColor.copy(alpha = 0.3f), radius = size.minDimension / 2, style = Stroke(width = 1.dp.toPx()))
        drawCircle(color = cockpitColor.copy(alpha = 0.8f), radius = (size.minDimension / 2) - 8.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
        drawCircle(brush = Brush.radialGradient(colors = listOf(cockpitColor.copy(alpha = 0.4f), Color.Transparent), radius = size.minDimension / 2), radius = size.minDimension / 2)
      },
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = angle,
        fontSize = 52.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(4.dp))
      Text(
        "Angle Recommandé",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}


@Composable
fun MemorizeButton(showSuccess: Boolean, onClick: () -> Unit) {
  OutlinedButton(
    onClick = onClick,
    shape = CircleShape,
    modifier = Modifier.size(140.dp),
    contentPadding = PaddingValues(0.dp),
    enabled = !showSuccess,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
      contentColor = MaterialTheme.colorScheme.onSurface
    )
  ) {
    if (showSuccess) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Check, contentDescription = "Mémorisé", modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Mémorisé !", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
      }
    } else {
      Text("Mémoriser", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
fun SensorReadoutPanel(tilt: String, azimuth: String, numericTilt: Float, numericAzimuth: Float) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    StyledInfoPanel(modifier = Modifier.weight(1f)) {
      CompassVisual(label = "Orientation", value = azimuth, angle = numericAzimuth)
    }
    StyledInfoPanel(modifier = Modifier.weight(1f)) {
      InclinometerVisual(label = "Inclinaison", value = tilt, angle = numericTilt)
    }
  }
}

@Composable
fun StyledInfoPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
  val borderColor = MaterialTheme.colorScheme.secondary
  Box(
    modifier = modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)).border(width = 1.dp, brush = Brush.verticalGradient(colors = listOf(borderColor.copy(alpha = 0.6f), borderColor.copy(alpha = 0.2f))), shape = RoundedCornerShape(12.dp)).hexagonGridBackground(color = borderColor.copy(alpha = 0.05f), hexagonSize = 20.dp).padding(8.dp),
    contentAlignment = Alignment.Center,
    content = content
  )
}

fun Modifier.hexagonGridBackground(color: Color, hexagonSize: Dp): Modifier = this.drawBehind {
  val hexagonRadius = hexagonSize.toPx() / 2f
  val hexHeight = sqrt(3f) * hexagonRadius
  val hexWidth = 2 * hexagonRadius
  val horizontalSpacing = hexWidth * 0.75f
  val verticalSpacing = hexHeight
  val path = Path()
  for (row in -1..((size.height / verticalSpacing).toInt() + 1)) {
    for (col in -1..((size.width / horizontalSpacing).toInt() + 1)) {
      val xOffset = if (row % 2 == 0) 0f else horizontalSpacing / 2f
      val centerX = col * horizontalSpacing + xOffset
      val centerY = row * verticalSpacing
      path.reset()
      for (i in 0..5) {
        val angle = 60f * i - 30f
        val pointX = centerX + hexagonRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val pointY = centerY + hexagonRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        if (i == 0) path.moveTo(pointX, pointY) else path.lineTo(pointX, pointY)
      }
      path.close()
      drawPath(path, color, style = Stroke(width = 1.dp.toPx()))
    }
  }
}

@Composable
fun InclinometerVisual(label: String, value: String, angle: Float, modifier: Modifier = Modifier) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Box(modifier = Modifier.fillMaxWidth().height(20.dp).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
      HorizontalDivider(modifier = Modifier.fillMaxWidth(0.9f).rotate(angle), color = MaterialTheme.colorScheme.primary, thickness = 2.dp)
    }
    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun CompassVisual(label: String, value: String, angle: Float, modifier: Modifier = Modifier) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
      Icon(Icons.Default.Navigation, contentDescription = "Boussole", modifier = Modifier.fillMaxSize().rotate(angle), tint = MaterialTheme.colorScheme.primary)
    }
    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun ManualEntryPanel(
  tilt: String,
  onTiltChange: (String) -> Unit,
  azimuth: String,
  onAzimuthChange: (String) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(value = tilt, onValueChange = onTiltChange, label = { Text("Inclinaison") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = azimuth, onValueChange = onAzimuthChange, label = { Text("Orientation (0=Sud)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
  }
}

@Composable
fun ModeButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val colors = if (isSelected) {
    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
  } else {
    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
  }
  Button(onClick = onClick, modifier = modifier.height(50.dp), shape = CircleShape, colors = colors) {
    Text(text)
  }
}