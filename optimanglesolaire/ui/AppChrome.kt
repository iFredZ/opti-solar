@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fredz.optimanglesolaire.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity

class ChromeController {
  var bottomHidden by mutableStateOf(false)
}

val LocalChromeController = staticCompositionLocalOf<ChromeController> {
  ChromeController()
}

private const val SUPPORT_EMAIL = "finjalrac+opti.solar@gmail.com"
private const val PAYPAL_URL    = "https://paypal.me/iFredZ"

@Composable
fun AppShell(
  onLanguageChange: (String) -> Unit = {},
  onHelp: () -> Unit = {},
  onSettings: () -> Unit = {},
  content: @Composable (PaddingValues) -> Unit
) {
  val ctx = LocalContext.current
  val density = LocalDensity.current
  val imeBottomPx = WindowInsets.ime.getBottom(density)
  val isKeyboardVisible = imeBottomPx > 0

  val chrome = remember { ChromeController() }

  var showHelp by remember { mutableStateOf(false) }
  var showSettings by remember { mutableStateOf(false) }

  CompositionLocalProvider(LocalChromeController provides chrome) {
    Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
          navigationIcon = { LanguageMenu(onLanguageChange) },
          title = {
            Text(
              "Opti SOLAR",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
          },
          actions = {
            TextButton(onClick = { showHelp = true; onHelp() }) { Text("❓") }
            TextButton(onClick = { showSettings = true; onSettings() }) { Text("⚙️") }
          }
        )
      },
      bottomBar = {
        val showBottom = !isKeyboardVisible && !chrome.bottomHidden
        if (showBottom) {
          BottomAppBar(
            // LA CORRECTION EST ICI : on fixe la hauteur
            modifier = Modifier.height(56.dp),
            tonalElevation = 3.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            actions = {
              val emailIntent = remember {
                Intent(Intent.ACTION_SENDTO).apply {
                  data = Uri.parse("mailto:")
                  putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                  putExtra(Intent.EXTRA_SUBJECT, "Opti SOLAR - Bug / Suggestion")
                }
              }
              TextButton(onClick = {
                runCatching { ctx.startActivity(emailIntent) }
              }) { Text("✉️ Bug / Suggestion", fontSize = 12.sp) } // Taille de texte réduite

              Spacer(Modifier.weight(1f))

              TextButton(onClick = {
                runCatching {
                  val i = Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_URL))
                  ctx.startActivity(i)
                }
              }) { Text("💝 Don PayPal", fontSize = 12.sp) } // Taille de texte réduite
            }
          )
        }
      }
    ) { pad -> content(pad) }

    if (showHelp) { /* ... */ }
    if (showSettings) { /* ... */ }
  }
}

// Les fonctions AlertDialog et LanguageMenu ne changent pas
@Composable
private fun LanguageMenu(onLanguageChange: (String) -> Unit) { /* ... */ }