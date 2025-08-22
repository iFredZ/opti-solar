package com.fredz.optimanglesolaire.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
  primary = ProSolarAmber,
  secondary = ProSolarBlue,
  tertiary = ProSolarAmber,
  background = Navy,
  surface = LightNavy,
  onPrimary = DarkNavy,
  onSecondary = DarkNavy,
  onTertiary = DarkNavy,
  onBackground = LightSlate,
  onSurface = LightSlate,
  onSurfaceVariant = Slate,

  // LA CORRECTION EST ICI : on utilise notre nouveau bleu
  error = ActiveBlue,
  errorContainer = ActiveBlue, // Le bouton actif utilise cette couleur
  onErrorContainer = DarkNavy,

  outline = Slate.copy(alpha = 0.5f)
)

@Composable
fun OptiSolarTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = DarkColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.background.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}