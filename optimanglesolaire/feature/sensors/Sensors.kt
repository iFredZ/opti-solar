package com.fredz.optimanglesolaire.feature.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.*

data class AngleReading(val tiltDeg: Double, val pvAzimuthDeg: Double)

private fun normalizeTo180(a: Double): Double =
  ((a + 180.0) % 360.0 + 360.0) % 360.0 - 180.0

private fun circularMeanDeg(values: List<Double>): Double {
  if (values.isEmpty()) return 0.0
  var sx = 0.0; var sy = 0.0
  for (d in values) {
    val r = Math.toRadians(d)
    sx += cos(r); sy += sin(r)
  }
  return Math.toDegrees(atan2(sy / values.size, sx / values.size)).let { normalizeTo180(it) }
}

/**
 * Mesure les angles en prenant plusieurs échantillons pour être stable.
 * - Poser le DOS du téléphone contre le plan du panneau (écran vers vous).
 * - tiltDeg : 0° = horizontal, 90° = vertical
 * - pvAzimuthDeg : format PVGIS (0=sud, 90=ouest, -90=est)
 */
suspend fun measureAnglesOnce(context: Context, timeoutMs: Long = 1200): AngleReading? {
  val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  val rot = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
  val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

  // On essaie d'abord ROTATION_VECTOR (plus fiable), sinon fallback accéléro+mag
  return withTimeoutOrNull(timeoutMs) {
    suspendCancellableCoroutine { cont ->

      fun resumeSafe(v: AngleReading?) { if (!cont.isCompleted) cont.resume(v) }

      if (rot != null) {
        val tilts = ArrayList<Double>(16)
        val azims = ArrayList<Double>(16)

        val listener = object : SensorEventListener {
          override fun onSensorChanged(e: SensorEvent) {
            if (e.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            val R = FloatArray(9)
            val O = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(R, e.values)
            SensorManager.getOrientation(R, O)

            // Azimut appareil (0=nord, 90=est, 180=sud...)
            val azDev = (Math.toDegrees(O[0].toDouble()) + 360.0) % 360.0
            val pvAz = normalizeTo180(azDev - 180.0) // PVGIS: 0=sud, +90=ouest, -90=est

            // Inclinaison : angle entre l'axe Z de l'appareil et l'axe "Up" du monde.
            val cosTheta = abs(R[8].toDouble()).coerceIn(0.0, 1.0)
            val tilt = Math.toDegrees(acos(cosTheta))

            tilts += tilt
            azims += pvAz

            // On prend ~8 échantillons minimum pour stabiliser
            if (tilts.size >= 8) {
              sm.unregisterListener(this)
              val tiltAvg = tilts.average()
              val azAvg = circularMeanDeg(azims)
              resumeSafe(AngleReading(tiltAvg, azAvg))
            }
          }
          override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, rot, SensorManager.SENSOR_DELAY_UI)
        cont.invokeOnCancellation { sm.unregisterListener(listener) }

      } else if (accel != null && mag != null) {
        // Fallback : moyenne sur accéléro + magnétomètre
        val R = FloatArray(9)
        val I = FloatArray(9)
        val O = FloatArray(3)
        var aVals = FloatArray(3)
        var mVals = FloatArray(3)
        var haveA = false
        var haveM = false
        val tilts = ArrayList<Double>(16)
        val azims = ArrayList<Double>(16)

        val listener = object : SensorEventListener {
          override fun onSensorChanged(e: SensorEvent) {
            when (e.sensor.type) {
              Sensor.TYPE_ACCELEROMETER  -> { aVals = e.values.clone(); haveA = true }
              Sensor.TYPE_MAGNETIC_FIELD -> { mVals = e.values.clone(); haveM = true }
            }
            if (haveA && haveM && SensorManager.getRotationMatrix(R, I, aVals, mVals)) {
              SensorManager.getOrientation(R, O)
              val azDev = (Math.toDegrees(O[0].toDouble()) + 360.0) % 360.0
              val pvAz = normalizeTo180(azDev - 180.0)

              val g = sqrt((aVals[0]*aVals[0] + aVals[1]*aVals[1] + aVals[2]*aVals[2]).toDouble())
              val cosTheta = (if (g > 0) abs(aVals[2].toDouble())/g else abs(R[8].toDouble())).coerceIn(0.0, 1.0)
              val tilt = Math.toDegrees(acos(cosTheta))

              tilts += tilt
              azims += pvAz

              if (tilts.size >= 10) {
                sm.unregisterListener(this)
                val tiltAvg = tilts.average()
                val azAvg = circularMeanDeg(azims)
                resumeSafe(AngleReading(tiltAvg, azAvg))
              }
            }
          }
          override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        sm.registerListener(listener, mag,   SensorManager.SENSOR_DELAY_UI)
        cont.invokeOnCancellation { sm.unregisterListener(listener) }

      } else {
        // Aucun capteur exploitable
        resumeSafe(null)
      }
    }
  }
}
