package com.fredz.optimanglesolaire.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fredz.optimanglesolaire.core.Prefs
// L'import de LanguagePicker n'est plus nécessaire

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var peakPower by remember { mutableStateOf(Prefs.loadPeakPower(context)) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirmation") },
            text = { Text("Voulez-vous vraiment effacer toutes les préférences sauvegardées ?") },
            confirmButton = {
                Button(
                    onClick = {
                        Prefs.clearAll(context)
                        peakPower = Prefs.loadPeakPower(context)
                        showResetDialog = false
                        Toast.makeText(context, "Préférences réinitialisées", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Annuler") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Retour") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section pour la puissance crête
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Préférences de calcul", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = peakPower,
                    onValueChange = { peakPower = it },
                    label = { Text("Puissance crête par défaut (kWc)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Button(
                    onClick = {
                        Prefs.savePeakPower(context, peakPower)
                        Toast.makeText(context, "Puissance sauvegardée !", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Enregistrer")
                }
            }

            HorizontalDivider()

            // La section Langue a été retirée d'ici

            // Section Données
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Données sauvegardées", style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Réinitialiser les préférences") }
            }
        }
    }
}