package com.fredz.optimanglesolaire.features.pvgis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class PvgisMonthly(val month: Int, val eMonthKWh: Double?)
data class PvgisSummary(
  val yearlyKWh: Double?,
  val monthly: List<PvgisMonthly>,
  val rawJson: String
)

private fun enc(v: Any) = URLEncoder.encode(v.toString(), StandardCharsets.UTF_8.name())

suspend fun fetchPvgisMonthly(
  lat: Double,
  lon: Double,
  tiltDeg: Double,
  azimuthDeg: Double,     // 0=sud, 90=ouest, -90=est
  peakPowerKw: Double,
  systemLossPct: Double = 14.0
): PvgisSummary = withContext(Dispatchers.IO) {
  val base = "https://re.jrc.ec.europa.eu/api/v5_3/PVcalc"
  val qs = listOf(
    "lat=${enc(lat)}",
    "lon=${enc(lon)}",
    "peakpower=${enc(peakPowerKw)}",
    "loss=${enc(systemLossPct)}",
    "angle=${enc(tiltDeg)}",
    "aspect=${enc(azimuthDeg)}",
    "outputformat=json",
    "browser=0"
  ).joinToString("&")

  val url = URL("$base?$qs")
  val conn = (url.openConnection() as HttpURLConnection).apply {
    requestMethod = "GET"
    connectTimeout = 15000
    readTimeout   = 30000
    setRequestProperty("Accept", "application/json")
    setRequestProperty("User-Agent", "OptiSolar/1.0 (Android)")
    instanceFollowRedirects = true
  }

  conn.connect()
  val code = conn.responseCode
  val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
    ?.bufferedReader()?.use { it.readText() } ?: ""

  if (code !in 200..299) {
    // On renvoie le détail pour comprendre (ex: "HTTP 400: ...")
    throw IllegalStateException("HTTP $code: ${body.take(400)}")
  }

  // --- Parse JSON ---
  val root = JSONObject(body)
  val outputs = root.optJSONObject("outputs") ?: JSONObject()

  // Certaines réponses sont à plat, d'autres sous "fixed". On gère les 2.
  val totals = outputs.optJSONObject("totals")
    ?: outputs.optJSONObject("fixed")?.optJSONObject("totals")
    ?: JSONObject()

  fun pickDouble(o: JSONObject?, k: String): Double? =
    o?.optDouble(k, Double.NaN)?.takeIf { !it.isNaN() }

  val yearly = pickDouble(totals, "E_y") ?: pickDouble(totals.optJSONObject("fixed"), "E_y")

  val monthlyArray = outputs.optJSONArray("monthly")
    ?: outputs.optJSONObject("fixed")?.optJSONArray("monthly")

  val monthly = buildList {
    if (monthlyArray != null) {
      for (i in 0 until monthlyArray.length()) {
        val item = monthlyArray.getJSONObject(i)
        val e = pickDouble(item, "E_m")
          ?: pickDouble(item.optJSONObject("fixed"), "E_m")
        val m = item.optInt("month", i + 1)
        add(PvgisMonthly(m, e))
      }
    }
  }

  PvgisSummary(yearlyKWh = yearly, monthly = monthly, rawJson = body)
}
