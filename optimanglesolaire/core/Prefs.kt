package com.fredz.optimanglesolaire.core

import android.content.Context

data class LastLocation(val lat: Double, val lon: Double)
data class LastAngles(val tilt: Double, val azimuth: Double)

object Prefs {
  private const val NAME = "opti_prefs"
  private const val K_LAT = "last_lat"
  private const val K_LON = "last_lon"
  private const val K_TILT = "last_tilt"
  private const val K_AZ = "last_az"

  private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

  fun saveLastLatLon(context: Context, lat: Double, lon: Double) {
    sp(context).edit()
      .putString(K_LAT, lat.toString())
      .putString(K_LON, lon.toString())
      .apply()
  }
  fun loadLastLatLon(context: Context): LastLocation? {
    val p = sp(context)
    val lat = p.getString(K_LAT, null)?.toDoubleOrNull() ?: return null
    val lon = p.getString(K_LON, null)?.toDoubleOrNull() ?: return null
    return LastLocation(lat, lon)
  }

  fun saveLastAngles(context: Context, tilt: Double, azimuth: Double) {
    sp(context).edit()
      .putString(K_TILT, tilt.toString())
      .putString(K_AZ, azimuth.toString())
      .apply()
  }
  fun loadLastAngles(context: Context): LastAngles? {
    val p = sp(context)
    val t = p.getString(K_TILT, null)?.toDoubleOrNull() ?: return null
    val a = p.getString(K_AZ, null)?.toDoubleOrNull() ?: return null
    return LastAngles(t, a)
  }

  fun clearAll(context: Context) {
    sp(context).edit().clear().apply()
  }
}
