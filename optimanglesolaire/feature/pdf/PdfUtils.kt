package com.fredz.optimanglesolaire.features.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

data class PdfResult(
  val message: String,
  val uri: Uri?,        // Android 10+ → on renvoie l'URI MediaStore (Téléchargements)
  val filename: String  // utile pour le partage
)

/**
 * Crée un PDF simple et renvoie (message, uri?, filename).
 * - Android 10+ (API 29+) : sauvegarde dans Téléchargements (MediaStore) → uri non-nulle.
 * - Android 9 et moins : sauvegarde dans dossier de l'app → uri nulle (on gèrera plus tard si besoin).
 */
fun createSimplePdf(
  context: Context,
  filename: String = "rapport_demo.pdf",
  title: String = "Mon PDF",
  lines: List<String> = listOf("Contenu de test", "Ligne 2", "Ligne 3")
): PdfResult {
  // 1) Dessin PDF (A4 ~ 595x842 @72dpi)
  val pdf = PdfDocument()
  val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
  val page = pdf.startPage(pageInfo)
  val canvas = page.canvas
  val paint = Paint().apply { textSize = 14f }
  var y = 40f
  canvas.drawText(title, 40f, y, paint); y += 24f
  for (line in lines) { canvas.drawText(line, 40f, y, paint); y += 20f }
  pdf.finishPage(page)

  return try {
    if (Build.VERSION.SDK_INT >= 29) {
      val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, filename)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
      }
      val uri = context.contentResolver.insert(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
      ) ?: throw IllegalStateException("Impossible de créer l'URI MediaStore")

      context.contentResolver.openOutputStream(uri)?.use { out ->
        pdf.writeTo(out)
      } ?: throw IllegalStateException("Flux de sortie indisponible")

      pdf.close()
      PdfResult("Créé dans Téléchargements : $filename", uri, filename)
    } else {
      val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
      val file = File(dir, filename)
      file.outputStream().use { out -> pdf.writeTo(out) }
      pdf.close()
      PdfResult("Créé (dossier de l'app) : ${file.absolutePath}", null, filename)
    }
  } catch (e: Exception) {
    pdf.close()
    PdfResult("Erreur création PDF : ${e.message}", null, filename)
  }
}
