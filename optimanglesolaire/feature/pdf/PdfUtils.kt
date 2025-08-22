package com.fredz.optimanglesolaire.features.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint
import com.fredz.optimanglesolaire.R
import java.io.File
import java.text.DecimalFormat

data class PdfReportData(
  val date: String,
  val locationName: String,
  val peakPower: String,
  val latitude: String,
  val longitude: String,
  val initialTilt: String,
  val initialAzimuth: String,
  val optimalTilt: String,
  val prodCurrentDaily: Double,
  val prodCurrentMonthly: Double,
  val prodCurrentYearly: Double,
  val prodOptimalDaily: Double,
  val prodOptimalMonthly: Double,
  val prodOptimalYearly: Double,
  val prodIdealDaily: Double,
  val prodIdealMonthly: Double,
  val prodIdealYearly: Double,
  val gainDailyWh: Double,
  val gainMonthlyKWh: Double,
  val gainYearlyKWh: Double
)

data class PdfResult(
  val message: String,
  val uri: Uri?,
  val filename: String
)

fun createPvgisReportPdf(
  context: Context,
  data: PdfReportData
): PdfResult {
  val filename = "OptiSolar_Rapport_${System.currentTimeMillis()}.pdf"
  val pdf = PdfDocument()
  val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
  val page = pdf.startPage(pageInfo)
  val canvas = page.canvas

  val pTitle = Paint().apply { textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.BLACK }
  val pSubtitle = Paint().apply { textSize = 16f; color = Color.DKGRAY }
  val pDate = Paint().apply { textSize = 11f; color = Color.DKGRAY; textAlign = Paint.Align.RIGHT }
  val pTableHeader = Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.WHITE }
  val pTableLabel = Paint().apply { textSize = 11f; color = Color.BLACK }
  val pTableValue = Paint().apply { textSize = 11f; color = Color.DKGRAY; textAlign = Paint.Align.RIGHT }
  val pSectionHeader = Paint().apply { textSize = 11f; isFakeBoldText = true; color = Color.DKGRAY }
  val pFooter = TextPaint().apply { textSize = 9f; color = Color.GRAY }
  val blueHeaderBg = Paint().apply { color = Color.parseColor("#3B82F6"); style = Paint.Style.FILL }
  val lightGrayRowBg = Paint().apply { color = Color.parseColor("#F3F4F6"); style = Paint.Style.FILL }
  val whiteRowBg = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }

  val numFormatKwh = DecimalFormat("0.00")
  val numFormatWh = DecimalFormat("0")
  var y = 40f
  val leftMargin = 40f
  val rightMargin = 555f
  val contentWidth = (rightMargin - leftMargin).toInt()

  try {
    val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 40, 40, true)
    canvas.drawBitmap(scaledBitmap, leftMargin, y - 5, null)
  } catch (e: Exception) { /* On continue sans logo si erreur */ }

  canvas.drawText("Rapport d'Optimisation Solaire", leftMargin + 50, y, pTitle)
  canvas.drawText(data.date, rightMargin, y - 10, pDate)
  y += 25f
  canvas.drawText(data.locationName, leftMargin + 50, y, pSubtitle)
  y += 40f

  // --- Table Helper Function ---
  fun drawRow(label: String, value: String, isEven: Boolean) {
    val bgPaint = if (isEven) lightGrayRowBg else whiteRowBg
    canvas.drawRect(leftMargin, y, rightMargin, y + 22f, bgPaint)
    canvas.drawText(label, leftMargin + 10, y + 16f, pTableLabel)
    canvas.drawText(value, rightMargin - 10, y + 16f, pTableValue)
    y += 22f
  }

  // --- Table 1: Paramètres ---
  val table1HeaderRect = RectF(leftMargin, y, rightMargin, y + 25f)
  canvas.drawRect(table1HeaderRect, blueHeaderBg)
  canvas.drawText("Paramètre", leftMargin + 10, y + 18f, pTableHeader)
  canvas.drawText("Valeur", rightMargin - 10, y + 18f, pTableHeader.apply { textAlign = Paint.Align.RIGHT })
  y += 25f

  drawRow("Localisation", "Lat ${data.latitude}, Lon ${data.longitude}", false)
  drawRow("Puissance installée", data.peakPower, true)
  drawRow("Inclinaison mesurée", data.initialTilt, false)
  drawRow("Azimut mesuré", data.initialAzimuth, true)
  drawRow("Inclinaison recommandée", data.optimalTilt, false)
  drawRow("Inclinaison optimale (théorique)", data.optimalTilt, true)
  y += 30f

  // --- Table 2: Production ---
  val table2HeaderRect = RectF(leftMargin, y, rightMargin, y + 25f)
  canvas.drawRect(table2HeaderRect, blueHeaderBg)
  canvas.drawText("Comparatif de Production Estimée", leftMargin + 10, y + 18f, pTableHeader.apply { textAlign = Paint.Align.LEFT })
  y += 25f

  fun drawProdHeader() {
    val bgPaint = lightGrayRowBg
    canvas.drawRect(leftMargin, y, rightMargin, y + 22f, bgPaint)
    pTableLabel.isFakeBoldText = true
    canvas.drawText("Configuration", leftMargin + 10, y + 16f, pTableLabel)
    canvas.drawText("Journalier", leftMargin + 250, y + 16f, pTableLabel)
    canvas.drawText("Mensuel", leftMargin + 350, y + 16f, pTableLabel)
    canvas.drawText("Annuel", leftMargin + 450, y + 16f, pTableLabel)
    pTableLabel.isFakeBoldText = false
    y += 22f
  }

  fun drawProdRow(label: String, valD: Double, valM: Double, valY: Double, isEven: Boolean) {
    val bgPaint = if (isEven) whiteRowBg else lightGrayRowBg
    canvas.drawRect(leftMargin, y, rightMargin, y + 22f, bgPaint)
    canvas.drawText(label, leftMargin + 10, y + 16f, pTableLabel)
    canvas.drawText("${numFormatKwh.format(valD)} kWh", leftMargin + 250, y + 16f, pTableLabel)
    canvas.drawText("${numFormatKwh.format(valM)} kWh", leftMargin + 350, y + 16f, pTableLabel)
    canvas.drawText("${numFormatKwh.format(valY)} kWh", leftMargin + 450, y + 16f, pTableLabel)
    y += 22f
  }

  drawProdHeader()
  drawProdRow("Actuelle", data.prodCurrentDaily, data.prodCurrentMonthly, data.prodCurrentYearly, false)
  drawProdRow("Optimale (recommandée)", data.prodOptimalDaily, data.prodOptimalMonthly, data.prodOptimalYearly, true)
  drawProdRow("Optimale théorique", data.prodIdealDaily, data.prodIdealMonthly, data.prodIdealYearly, false)
  y += 30f

  // --- Section GAIN ---
  val gainHeaderRect = RectF(leftMargin, y, rightMargin, y + 25f)
  canvas.drawRect(gainHeaderRect, blueHeaderBg)
  canvas.drawText("Gain Potentiel", leftMargin + 10, y + 18f, pTableHeader)
  y += 25f
  drawRow("Journalier", "~ ${numFormatWh.format(data.gainDailyWh)} Wh", false)
  drawRow("Mensuel (approx.)", "~ ${numFormatKwh.format(data.gainMonthlyKWh)} kWh", true)
  drawRow("Annuel", "~ ${numFormatKwh.format(data.gainYearlyKWh)} kWh", false)
  y += 40f

  // --- Pied de page ---
  canvas.drawText("Avertissement Important Concernant la Précision", leftMargin, y, pSectionHeader)
  y += 15f
  val disclaimer = "Les données de production et les mesures d'inclinaison/orientation fournies dans ce rapport sont des estimations. La précision des capteurs peut varier. Les créateurs de cette application ne sauraient être tenus responsables des écarts entre les estimations et la production réelle."
  val disclaimerLayout = StaticLayout.Builder.obtain(disclaimer, 0, disclaimer.length, pFooter, contentWidth).build()
  canvas.save()
  canvas.translate(leftMargin, y)
  disclaimerLayout.draw(canvas)
  canvas.restore()
  y += disclaimerLayout.height + 20f
  canvas.drawText("Explication des estimations", leftMargin, y, pSectionHeader)
  y += 15f
  val explanation = "La 'Production Optimale' compare votre inclinaison actuelle à la meilleure inclinaison possible pour VOTRE orientation. La 'Production Idéale' est une valeur de référence qui montre le potentiel si votre installation était parfaitement orientée plein Sud avec une inclinaison optimale."
  val explanationLayout = StaticLayout.Builder.obtain(explanation, 0, explanation.length, pFooter, contentWidth).build()
  canvas.save()
  canvas.translate(leftMargin, y)
  explanationLayout.draw(canvas)
  canvas.restore()

  pdf.finishPage(page)

  return try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, filename)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.IS_PENDING, 1)
      }
      val resolver = context.contentResolver
      val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw IllegalStateException("Impossible de créer l'URI MediaStore")
      resolver.openOutputStream(uri)?.use { out -> pdf.writeTo(out) } ?: throw IllegalStateException("Flux de sortie indisponible")
      values.clear()
      values.put(MediaStore.Downloads.IS_PENDING, 0)
      resolver.update(uri, values, null, null)
      pdf.close()
      PdfResult("Créé dans Téléchargements : $filename", uri, filename)
    } else {
      @Suppress("DEPRECATION")
      val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      val file = File(dir, filename)
      file.outputStream().use { out -> pdf.writeTo(out) }
      pdf.close()
      PdfResult("Créé dans Téléchargements : ${file.absolutePath}", null, filename)
    }
  } catch (e: Exception) {
    pdf.close()
    PdfResult("Erreur création PDF : ${e.message}", null, filename)
  }
}