package com.fredz.optimanglesolaire.feature.estimation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fredz.optimanglesolaire.features.pvgis.fetchPvgisMonthly
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Le ViewModel reçoit maintenant "SavedStateHandle" pour lire les arguments de navigation
class EstimationViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // --- 1. L'ÉTAT (les "souvenirs" de l'écran) ---
    // Les champs sont pré-remplis avec les valeurs passées dans l'URL
    var peakPower by mutableStateOf("1.0") // Valeur par défaut
    var latitude by mutableStateOf(savedStateHandle.get<String>("lat") ?: "")
    var longitude by mutableStateOf(savedStateHandle.get<String>("lon") ?: "")
    var tilt by mutableStateOf(savedStateHandle.get<String>("tilt") ?: "")
    var azimuth by mutableStateOf(savedStateHandle.get<String>("az") ?: "")
    var status by mutableStateOf<String?>(null)
    var results by mutableStateOf<List<ScenarioResult>?>(null)

    // --- 2. LA LOGIQUE (les "actions" du cerveau) ---
    fun calculateScenarios() {
        viewModelScope.launch {
            status = "PVGIS : calcul des scénarios…"
            results = null
            try {
                val pp = peakPower.toNumOrNull()
                val lat = latitude.toNumOrNull()
                val lon = longitude.toNumOrNull()
                val itilt = tilt.toNumOrNull()
                val iaz = azimuth.toNumOrNull()

                if (pp == null || lat == null || lon == null || itilt == null || iaz == null) {
                    status = "Veuillez remplir tous les champs."
                    return@launch
                }

                val tiltOpt = optimalTiltFromLat(lat).toDouble()

                val resA = fetchPvgisMonthly(lat, lon, itilt, iaz, pp)
                val resB = fetchPvgisMonthly(lat, lon, tiltOpt, iaz, pp)
                val resC = fetchPvgisMonthly(lat, lon, tiltOpt, 0.0, pp)

                results = listOf(
                    ScenarioResult("Actuel", itilt, iaz, resA.yearlyKWh),
                    ScenarioResult("Max avec votre orientation", tiltOpt, iaz, resB.yearlyKWh),
                    ScenarioResult("Max idéal (site)", tiltOpt, 0.0, resC.yearlyKWh)
                )
                status = "Calculs terminés."

            } catch (e: Exception) {
                status = "Erreur PVGIS : ${e.message}"
            }
        }
    }
}

// --- 3. PETITS "OUTILS" ---
private fun String.toNumOrNull(): Double? = this.trim().replace(',', '.').toDoubleOrNull()
private fun optimalTiltFromLat(lat: Double): Int = abs(lat).coerceIn(0.0, 60.0).roundToInt()

data class ScenarioResult(
    val name: String,
    val tilt: Double,
    val azimuth: Double,
    val yearlyKWh: Double?
)