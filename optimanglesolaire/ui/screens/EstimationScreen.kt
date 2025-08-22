@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fredz.optimanglesolaire.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fredz.optimanglesolaire.feature.estimation.EstimationViewModel
import com.fredz.optimanglesolaire.feature.estimation.ScenarioResult

@Composable
fun EstimationScreen(
  viewModel: EstimationViewModel, // Le "cerveau" est maintenant passé en paramètre
  onBack: () -> Unit = {}
) {
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
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // La carte ne fait plus que lire et écrire dans le ViewModel
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Récapitulatif (modifiable)", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
          value = viewModel.peakPower, onValueChange = { viewModel.peakPower = it },
          label = { Text("Puissance crête (kWc)") }, singleLine = true
        )
        OutlinedTextField(
          value = viewModel.latitude, onValueChange = { viewModel.latitude = it },
          label = { Text("Latitude") }, singleLine = true
        )
        OutlinedTextField(
          value = viewModel.longitude, onValueChange = { viewModel.longitude = it },
          label = { Text("Longitude") }, singleLine = true
        )
        OutlinedTextField(
          value = viewModel.tilt, onValueChange = { viewModel.tilt = it },
          label = { Text("Inclinaison (°)") }, singleLine = true
        )
        OutlinedTextField(
          value = viewModel.azimuth, onValueChange = { viewModel.azimuth = it },
          label = { Text("Azimut (0=sud, 90=ouest, -90=est)") }, singleLine = true
        )

        // Le bouton appelle maintenant directement la fonction du "cerveau"
        Button(
          onClick = { viewModel.calculateScenarios() },
          modifier = Modifier.fillMaxWidth()
        ) { Text("Calculer avec PVGIS (3 scénarios)") }

        // On affiche le statut depuis le ViewModel
        viewModel.status?.let {
          Text(
            it,
            color = if (it.startsWith("Erreur")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
          )
        }

        // On affiche les résultats depuis le ViewModel
        viewModel.results?.let { list ->
          Spacer(Modifier.height(12.dp))
          ScenarioTable(list)
        }
      }
    }
  }
}

@Composable
private fun ScenarioTable(items: List<ScenarioResult>) {
  // (Cette partie n'a pas changé)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
      .padding(12.dp)
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      HeadCell("Scénario", weight = 1.6f)
      HeadCell("Tilt", weight = 0.7f, align = TextAlign.End)
      HeadCell("Az",   weight = 0.7f, align = TextAlign.End)
      HeadCell("Annuel", weight = 1.0f, align = TextAlign.End)
    }
    HorizontalDivider(Modifier.padding(vertical = 6.dp))
    items.forEach { r ->
      Row(
        Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Cell(r.name, weight = 1.6f)
        Cell("${"%.0f".format(r.tilt)}°", weight = 0.7f, align = TextAlign.End)
        Cell("${"%.0f".format(r.azimuth)}°", weight = 0.7f, align = TextAlign.End)
        Cell(r.yearlyKWh?.let { "${"%.0f".format(it)} kWh" } ?: "n/d", weight = 1.0f, align = TextAlign.End)
      }
    }
  }
}

@Composable
private fun RowScope.HeadCell(text: String, weight: Float = 1f, align: TextAlign = TextAlign.Start) {
  Text(text = text, modifier = Modifier.weight(weight).padding(vertical = 6.dp, horizontal = 8.dp), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), textAlign = align)
}

@Composable
private fun RowScope.Cell(text: String, weight: Float = 1f, align: TextAlign = TextAlign.Start) {
  Text(text = text, modifier = Modifier.weight(weight).padding(vertical = 6.dp, horizontal = 8.dp), style = MaterialTheme.typography.bodyMedium, textAlign = align)
}