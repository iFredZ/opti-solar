package com.fredz.optimanglesolaire.feature.estimation

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.fredz.optimanglesolaire.core.Prefs
import com.fredz.optimanglesolaire.features.pdf.PdfReportData
import com.fredz.optimanglesolaire.features.pdf.createPvgisReportPdf
import com.fredz.optimanglesolaire.features.pdf.sharePdf
import com.fredz.optimanglesolaire.features.pvgis.fetchPvgisMonthly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class Frequency { DAILY, MONTHLY, YEARLY }

data class ScenarioResult(
    val name: String,
    val tilt: Double,
    val azimuth: Double,
    val yearlyKWh: Double?
)

class EstimationViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    var peakPower by mutableStateOf(Prefs.loadPeakPower(application))
    var latitude by mutableStateOf("")
    var longitude by mutableStateOf("")
    var tilt by mutableStateOf("")
    var azimuth by mutableStateOf("")
    var status by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
    var selectedFrequency by mutableStateOf(Frequency.YEARLY)
    private var resultsYearly by mutableStateOf<List<ScenarioResult>?>(null)
    private var potentialGainYearlyKWh by mutableStateOf<Double?>(null)

    init {
        latitude = savedStateHandle.get<String>("lat") ?: ""
        longitude = savedStateHandle.get<String>("lon") ?: ""
        tilt = savedStateHandle.get<String>("tilt") ?: ""
        azimuth = savedStateHandle.get<String>("az") ?: ""
    }

    val frequencyLabel: String
        get() = when(selectedFrequency) {
            Frequency.DAILY -> "journalière"
            Frequency.MONTHLY -> "mensuelle"
            Frequency.YEARLY -> "annuelle"
        }

    val gainValue: Double?
        get() {
            val yearlyGain = potentialGainYearlyKWh ?: return null
            return when(selectedFrequency) {
                Frequency.DAILY -> (yearlyGain * 1000) / 365.25
                Frequency.MONTHLY -> yearlyGain / 12.0
                Frequency.YEARLY -> yearlyGain
            }
        }

    val gainUnit: String
        get() = if (selectedFrequency == Frequency.DAILY) "Wh" else "kWh"

    fun getResultForFrequency(name: String): ScenarioResult? {
        val yearlyResult = resultsYearly?.firstOrNull { it.name == name } ?: return null
        val yearlyProd = yearlyResult.yearlyKWh ?: return yearlyResult
        val newProd = when(selectedFrequency) {
            Frequency.DAILY -> yearlyProd / 365.25
            Frequency.MONTHLY -> yearlyProd / 12.0
            Frequency.YEARLY -> yearlyProd
        }
        return yearlyResult.copy(yearlyKWh = newProd)
    }

    fun onFrequencySelected(frequency: Frequency) {
        selectedFrequency = frequency
    }

    fun calculateScenarios() {
        viewModelScope.launch {
            isLoading = true
            status = "PVGIS : calcul des scénarios…"
            resultsYearly = null
            potentialGainYearlyKWh = null
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

                val resultList = listOf(
                    ScenarioResult("Votre configuration", itilt, iaz, resA.yearlyKWh),
                    ScenarioResult("Inclinaison optimale", tiltOpt, iaz, resB.yearlyKWh),
                    ScenarioResult("Configuration IDÉALE", tiltOpt, 0.0, resC.yearlyKWh)
                )
                resultsYearly = resultList
                status = "Calculs terminés."

                val actualProd = resA.yearlyKWh
                val idealProd = resC.yearlyKWh
                if (actualProd != null && idealProd != null) {
                    potentialGainYearlyKWh = max(0.0, idealProd - actualProd)
                }
            } catch (e: Exception) {
                status = "Erreur PVGIS : ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun fetchCityName(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=10&addressdetails=1"
            val response = URL(url).readText()
            val json = JSONObject(response)
            val address = json.optJSONObject("address")
            address?.optString("city", null)
                ?: address?.optString("town", null)
                ?: address?.optString("village", "Lieu inconnu")
        } catch (e: Exception) {
            "Lieu inconnu"
        }
    }

    fun generateAndSharePdf(context: Context) {
        viewModelScope.launch {
            val data = preparePdfData()
            if (data == null) {
                Toast.makeText(context, "Données de résultat non disponibles pour le PDF", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val result = createPvgisReportPdf(context, data)
            if (result.uri != null) {
                sharePdf(context, result.uri, result.filename)
            } else {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun preparePdfData(): PdfReportData? {
        val results = resultsYearly ?: return null
        val gainYearly = potentialGainYearlyKWh ?: return null
        val resCurrent = results.firstOrNull { it.name == "Votre configuration" } ?: return null
        val resOptimalTilt = results.firstOrNull { it.name == "Inclinaison optimale" } ?: return null
        val resIdeal = results.firstOrNull { it.name == "Configuration IDÉALE" } ?: return null
        val lat = latitude.toNumOrNull() ?: return null
        val lon = longitude.toNumOrNull() ?: return null
        val tiltOpt = optimalTiltFromLat(lat)
        val cityName = fetchCityName(lat, lon)

        return PdfReportData(
            date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            locationName = cityName ?: "Inconnu",
            peakPower = "${peakPower.toNumOrNull()} kWc",
            latitude = this.latitude,
            longitude = this.longitude,
            initialTilt = "${tilt.toNumOrNull()?.toInt()}°",
            initialAzimuth = "${azimuth.toNumOrNull()?.toInt()}°",
            optimalTilt = "${tiltOpt}°",
            prodCurrentDaily = (resCurrent.yearlyKWh ?: 0.0) / 365.25,
            prodCurrentMonthly = (resCurrent.yearlyKWh ?: 0.0) / 12.0,
            prodCurrentYearly = resCurrent.yearlyKWh ?: 0.0,
            prodOptimalDaily = (resOptimalTilt.yearlyKWh ?: 0.0) / 365.25,
            prodOptimalMonthly = (resOptimalTilt.yearlyKWh ?: 0.0) / 12.0,
            prodOptimalYearly = resOptimalTilt.yearlyKWh ?: 0.0,
            prodIdealDaily = (resIdeal.yearlyKWh ?: 0.0) / 365.25,
            prodIdealMonthly = (resIdeal.yearlyKWh ?: 0.0) / 12.0,
            prodIdealYearly = resIdeal.yearlyKWh ?: 0.0,
            gainDailyWh = (gainYearly * 1000) / 365.25,
            gainMonthlyKWh = gainYearly / 12.0,
            gainYearlyKWh = gainYearly
        )
    }
}

private fun String.toNumOrNull(): Double? = this.trim().replace(',', '.').toDoubleOrNull()
private fun optimalTiltFromLat(lat: Double): Int = abs(lat).coerceIn(0.0, 60.0).roundToInt()