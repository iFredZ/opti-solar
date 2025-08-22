package com.fredz.optimanglesolaire.features.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri

fun openPdf(context: Context, uri: Uri) {
  val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, "application/pdf")
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  context.startActivity(Intent.createChooser(intent, "Ouvrir PDF"))
}

fun sharePdf(context: Context, uri: Uri, filename: String = "rapport.pdf") {
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "application/pdf"
    putExtra(Intent.EXTRA_STREAM, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    putExtra(Intent.EXTRA_TITLE, filename)
  }
  context.startActivity(Intent.createChooser(intent, "Partager PDF"))
}
