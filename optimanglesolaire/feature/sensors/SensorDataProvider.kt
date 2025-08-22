package com.fredz.optimanglesolaire.feature.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

// On définit un "conteneur" pour renvoyer toutes les données d'un coup
data class SensorData(
    val tilt: Float,
    val azimuth: Float,
    val isStable: Boolean
)

class SensorDataProvider(
    context: Context,
    private val onDataChanged: (SensorData) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Variables pour la logique de stabilité (traduite du JS)
    private val stabilityReadings = mutableListOf<Pair<Float, Float>>()
    private val STABILITY_BUFFER_SIZE = 5 // 5 dernières lectures pour la stabilité
    private val TILT_THRESHOLD = 2.0f // Seuil de stabilité pour l'inclinaison
    private val AZIMUTH_THRESHOLD = 2.0f // Seuil de stabilité pour l'azimut

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun start() {
        stabilityReadings.clear()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, event.values.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
        }

        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            // L'inclinaison est l'opposé du pitch, et on s'assure qu'elle est positive
            val tilt = abs(pitch)

            val isStable = checkStability(tilt, azimuth)

            onDataChanged(SensorData(tilt, azimuth, isStable))
        }
    }

    private fun checkStability(currentTilt: Float, currentAzimuth: Float): Boolean {
        stabilityReadings.add(currentTilt to currentAzimuth)
        if (stabilityReadings.size > STABILITY_BUFFER_SIZE) {
            stabilityReadings.removeAt(0)
        }
        if (stabilityReadings.size < STABILITY_BUFFER_SIZE) return false

        val firstReading = stabilityReadings.first()
        return stabilityReadings.all { reading ->
            val tiltDifference = abs(reading.first - firstReading.first)
            // Calcul de la différence sur un cercle pour l'azimut
            val azimuthDifference = abs(reading.second - firstReading.second)
            val shortestAzimuthDifference = minOf(azimuthDifference, 360 - azimuthDifference)

            tiltDifference <= TILT_THRESHOLD && shortestAzimuthDifference <= AZIMUTH_THRESHOLD
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}