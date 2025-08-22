package com.fredz.optimanglesolaire.feature.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fredz.optimanglesolaire.feature.sensors.SensorData
import com.fredz.optimanglesolaire.feature.sensors.SensorDataProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt

enum class EntryMode {
    NONE, SENSORS, MANUAL
}

class HomeViewModel : ViewModel() {

    private var sensorDataProvider: SensorDataProvider? = null
    private val formatter = DecimalFormat("0")

    private var lastDisplayedTilt: Float? = null
    private var lastDisplayedAzimuth: Float? = null

    var entryMode by mutableStateOf(EntryMode.NONE)
        private set
    var latitudeText by mutableStateOf("44.21446")
    var numericLongitude by mutableStateOf<Double?>(4.028)
        private set
    var numericTilt by mutableStateOf<Float?>(null)
        private set
    var numericAzimuth by mutableStateOf<Float?>(null)
        private set
    var isLocationLoading by mutableStateOf(false)
    var measuredTilt by mutableStateOf("—")
        private set
    var measuredAzimuth by mutableStateOf("—")
        private set
    var isSensorsActive by mutableStateOf(false)
        private set
    var areSensorsStable by mutableStateOf(false)
        private set
    var selectedDate by mutableStateOf("—")
        private set
    var solarAngle by mutableStateOf("—°")
        private set
    var showDatePicker by mutableStateOf(false)
        private set
    var manualTiltText by mutableStateOf("")
    var manualAzimuthText by mutableStateOf("")
    var memorizedNumericTilt by mutableStateOf<Float?>(null)
        private set
    var memorizedNumericAzimuth by mutableStateOf<Float?>(null)
        private set
    var showMemorizeSuccess by mutableStateOf(false)
        private set
    private var currentDate: LocalDate = LocalDate.now()

    val isEstimateButtonEnabled: Boolean
        get() {
            val hasValidLat = latitudeText.replace(',', '.').toDoubleOrNull() != null
            if (!hasValidLat) return false

            return when (entryMode) {
                EntryMode.SENSORS -> memorizedNumericTilt != null && memorizedNumericAzimuth != null
                EntryMode.MANUAL -> manualTiltText.isNotBlank() && manualAzimuthText.isNotBlank()
                EntryMode.NONE -> false
            }
        }

    init {
        updateDate(currentDate)
        calculateSolarAngle()
    }

    fun onLatitudeChanged(newLatitude: String) {
        latitudeText = newLatitude
        calculateSolarAngle()
    }

    fun onLocationResult(lat: Double, lon: Double) {
        val locFormatter = DecimalFormat("0.00")
        latitudeText = locFormatter.format(lat).replace(',', '.')
        numericLongitude = lon
        isLocationLoading = false
        calculateSolarAngle()
    }

    fun onLocationRequested() { isLocationLoading = true }

    fun onLocationError() {
        isLocationLoading = false
        latitudeText = "Erreur"
        solarAngle = "—°"
    }

    fun onEntryModeSelected(mode: EntryMode, context: Context) {
        val newMode = if (entryMode == mode) EntryMode.NONE else mode
        if (entryMode == EntryMode.SENSORS && newMode != EntryMode.SENSORS) { stopSensors() }
        entryMode = newMode
        if (entryMode == EntryMode.SENSORS) { startSensors(context) }
        calculateSolarAngle()
    }

    fun onMemorizeClicked() {
        if (!areSensorsStable) return
        memorizedNumericTilt = numericTilt
        memorizedNumericAzimuth = numericAzimuth
        viewModelScope.launch {
            showMemorizeSuccess = true
            delay(1500L)
            showMemorizeSuccess = false
        }
    }

    private fun startSensors(context: Context) {
        if (isSensorsActive) return
        isSensorsActive = true
        memorizedNumericTilt = null
        memorizedNumericAzimuth = null
        lastDisplayedTilt = null
        lastDisplayedAzimuth = null
        sensorDataProvider = SensorDataProvider(context) { data: SensorData ->
            numericTilt = data.tilt
            numericAzimuth = data.azimuth
            areSensorsStable = data.isStable
            calculateSolarAngle()

            val lastTilt = lastDisplayedTilt
            if (lastTilt == null || abs(data.tilt - lastTilt) >= 1.0f) {
                measuredTilt = formatter.format(data.tilt)
                lastDisplayedTilt = data.tilt
            }

            val lastAzimuth = lastDisplayedAzimuth
            val azimuthDifference = if(lastAzimuth != null) minOf(abs(data.azimuth - lastAzimuth), 360 - abs(data.azimuth - lastAzimuth)) else null
            if (lastAzimuth == null || (azimuthDifference != null && azimuthDifference >= 1.0f)) {
                measuredAzimuth = formatAzimuth(data.azimuth)
                lastDisplayedAzimuth = data.azimuth
            }
        }
        sensorDataProvider?.start()
    }

    private fun stopSensors() {
        sensorDataProvider?.stop()
        sensorDataProvider = null
        isSensorsActive = false
        areSensorsStable = false
        measuredTilt = "—"
        measuredAzimuth = "—"
        numericTilt = null
        numericAzimuth = null
    }

    private fun formatAzimuth(azimuth: Float): String {
        val normalizedAzimuth = (azimuth + 360) % 360
        val tolerance = 5.0
        when {
            normalizedAzimuth >= 360 - tolerance || normalizedAzimuth <= tolerance -> return "NORD"
            abs(normalizedAzimuth - 90) <= tolerance -> return "-90°E"
            abs(normalizedAzimuth - 180) <= tolerance -> return "SUD"
            abs(normalizedAzimuth - 270) <= tolerance -> return "90°O"
        }
        var deviation = normalizedAzimuth - 180
        if (deviation > 180) deviation -= 360
        if (deviation < -180) deviation += 360
        val formattedDeviation = formatter.format(deviation)
        return if (deviation >= 0) "$formattedDeviation°O" else "$formattedDeviation°E"
    }

    fun onDateClicked() { showDatePicker = true }

    fun onDateSelected(millis: Long) {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        updateDate(date)
        showDatePicker = false
    }

    fun onDatePickerDismissed() { showDatePicker = false }

    fun onManualTiltChanged(text: String) { manualTiltText = text; calculateSolarAngle() }
    fun onManualAzimuthChanged(text: String) { manualAzimuthText = text; calculateSolarAngle() }

    private fun updateDate(date: LocalDate) {
        currentDate = date
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        selectedDate = date.format(formatter)
        calculateSolarAngle()
    }

    private fun calculateSolarAngle() {
        val lat = latitudeText.replace(',', '.').toDoubleOrNull()
        if (lat == null) {
            solarAngle = "—°"
            return
        }
        val dayOfYear = currentDate.dayOfYear
        val declination = -23.45 * cos(Math.toRadians(360.0 / 365.0 * (dayOfYear + 10)))
        var optimalTilt = lat - declination
        val panelAzimuthDeviation: Double? = when (entryMode) {
            EntryMode.SENSORS -> {
                val az = numericAzimuth
                if (az == null) { null } else {
                    val normalizedAzimuth = (az + 360) % 360
                    var deviation = normalizedAzimuth - 180.0
                    if (deviation > 180) deviation -= 360.0
                    deviation
                }
            }
            EntryMode.MANUAL -> manualAzimuthText.replace(',', '.').toDoubleOrNull()
            EntryMode.NONE -> 0.0
        }
        if (panelAzimuthDeviation != null) {
            val penalty = min(6.0, abs(panelAzimuthDeviation) / 12.0)
            optimalTilt -= penalty
        }
        val finalAngle = optimalTilt.coerceIn(0.0, 90.0).roundToInt()

        // CORRECTION POUR LE CENTRAGE VISUEL
        val angleString = finalAngle.toString()
        val paddedAngleString = if (angleString.length < 2) " $angleString" else angleString
        solarAngle = "$paddedAngleString°"
    }

    override fun onCleared() {
        sensorDataProvider?.stop()
        super.onCleared()
    }
}