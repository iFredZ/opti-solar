@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fredz.optimanglesolaire.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fredz.optimanglesolaire.feature.estimation.EstimationViewModel
import com.fredz.optimanglesolaire.feature.estimation.Frequency
import java.text.DecimalFormat

@Composable
fun EstimationScreen(
  viewModel: EstimationViewModel,
  onBack: () -> Unit = {}
) {
  val context = LocalContext.current // On récupère le contexte pour le PDF

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Estimation de production", fontWeight = FontWeight.Bold) },
        navigationIcon = { TextButton(onClick = onBack) { Text("← Retour") } }
      )
    }
  ) { pad ->
    Column(
      Modifier
        .padding(pad)
        .fillMaxSize()
        .verticalScroll(androidx.compose.foundation.rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Panneau de configuration (inchangé)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Paramètres de l'estimation", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = viewModel.peakPower, onValueChange = { viewModel.peakPower = it }, label = { Text("Puissance crête (kWc)") }, singleLine = true)
        OutlinedTextField(value = viewModel.latitude, onValueChange = { viewModel.latitude = it }, label = { Text("Latitude") }, singleLine = true)
        OutlinedTextField(value = viewModel.longitude, onValueChange = { viewModel.longitude = it }, label = { Text("Longitude") }, singleLine = true)
        OutlinedTextField(value = viewModel.tilt, onValueChange = { viewModel.tilt = it }, label = { Text("Inclinaison (°)") }, singleLine = true)
        OutlinedTextField(value = viewModel.azimuth, onValueChange = { viewModel.azimuth = it }, label = { Text("Azimut (0=sud, 90=ouest, -90=est)") }, singleLine = true)
        Button(
          onClick = { viewModel.calculateScenarios() },
          modifier = Modifier.fillMaxWidth(),
          enabled = !viewModel.isLoading
        ) { Text("Calculer le gain potentiel") }
      }

      // Section de résultats (inchangée)
      AnimatedVisibility(visible = viewModel.status != null) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (viewModel.isLoading) {
            CircularProgressIndicator()
            Text(text = viewModel.status ?: "", style = MaterialTheme.typography.bodySmall)
          } else {
            if (viewModel.getResultForFrequency("Votre configuration") != null) {
              // ... (Sélecteur de fréquence et cartes de résultats inchangés) ...
              SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50), onClick = { viewModel.onFrequencySelected(Frequency.DAILY) }, selected = viewModel.selectedFrequency == Frequency.DAILY) { Text("Jour") }
                SegmentedButton(shape = RoundedCornerShape(0.dp), onClick = { viewModel.onFrequencySelected(Frequency.MONTHLY) }, selected = viewModel.selectedFrequency == Frequency.MONTHLY) { Text("Mois") }
                SegmentedButton(shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50), onClick = { viewModel.onFrequencySelected(Frequency.YEARLY) }, selected = viewModel.selectedFrequency == Frequency.YEARLY) { Text("An") }
              }
              val resultCurrent = viewModel.getResultForFrequency("Votre configuration")
              val resultOptimalTilt = viewModel.getResultForFrequency("Inclinaison optimale")
              val resultIdeal = viewModel.getResultForFrequency("Configuration IDÉALE")
              resultCurrent?.let { ResultCard(label = "Production (${it.name})", value = it.yearlyKWh, unit = "kWh", subLabel = "Inclinaison: ${it.tilt.toInt()}°, Azimut: ${it.azimuth.toInt()}°") }
              resultOptimalTilt?.let { ResultCard(label = "Production (${it.name})", value = it.yearlyKWh, unit = "kWh", subLabel = "Inclinaison: ${it.tilt.toInt()}°, Azimut: ${it.azimuth.toInt()}°") }
              viewModel.gainValue?.let { HighlightCard(label = "Gain potentiel (${viewModel.frequencyLabel})", value = it, unit = viewModel.gainUnit) }
              resultIdeal?.let { ResultCard(label = "Production (${it.name})", value = it.yearlyKWh, unit = "kWh", subLabel = "Inclinaison: ${it.tilt.toInt()}°, Azimut: ${it.azimuth.toInt()}°") }

              // BOUTON PDF AJOUTÉ ICI
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = { viewModel.generateAndSharePdf(context) },
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.Share, contentDescription = "Exporter")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exporter en PDF")
              }
            }
          }
        }
      }
    }
  }
}

// Les composants ResultCard et HighlightCard ne changent pas
// ...
@Composable
private fun ResultCard(label: String, value: Double?, unit: String, subLabel: String) {
  Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(text = label, textAlign = TextAlign.Center)
      Text(text = value?.let { "${DecimalFormat("0.00").format(it)} $unit" } ?: "N/A", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Text(text = subLabel, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
  }
}
@Composable
private fun HighlightCard(label: String, value: Double, unit: String) {
  Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(text = label, textAlign = TextAlign.Center)
      Text(text = "~ ${DecimalFormat("0.00").format(value)} $unit", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
  }
}