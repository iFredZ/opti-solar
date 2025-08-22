package com.fredz.optimanglesolaire.feature.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Essaie d'abord une position "last known" (rapide, même en intérieur),
 * puis une lecture "courante" si possible (API 30+).
 */
suspend fun getLocationOnce(ctx: Context): Location? {
  val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
  val providers = listOf(
    LocationManager.GPS_PROVIDER,
    LocationManager.NETWORK_PROVIDER,
    LocationManager.PASSIVE_PROVIDER
  ).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(true) }

  // 1) Fallback rapide: lastKnownLocation (souvent dispo grâce au Wi-Fi/cellulaire)
  for (p in providers) {
    try { lm.getLastKnownLocation(p)?.let { return it } } catch (_: SecurityException) {}
  }

  // 2) Lecture courante (API 30+)
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val provider = providers.firstOrNull() ?: LocationManager.NETWORK_PROVIDER
    return suspendCancellableCoroutine { cont ->
      try {
        lm.getCurrentLocation(provider, null, ctx.mainExecutor) { loc ->
          cont.resume(loc)
        }
      } catch (e: SecurityException) {
        cont.resumeWithException(e)
      }
    }
  }

  // 3) Rien trouvé
  return null
}
