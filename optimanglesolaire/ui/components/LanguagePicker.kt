package com.fredz.optimanglesolaire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fredz.optimanglesolaire.core.LanguageManager
import java.util.Locale

/**
 * Bouton langue (rond) qui ouvre un menu pour choisir FR / EN / DE / ES.
 * - Affiche l’emoji du drapeau courant
 * - Applique la langue via LanguageManager
 */
@Composable
fun LanguagePicker(
    modifier: Modifier = Modifier,
    onChanged: (() -> Unit)? = null, // si tu veux réagir (snackbar, etc.)
) {
    val context = LocalContext.current
    val expanded = remember { mutableStateOf(false) }
    val currentTag = remember { mutableStateOf(LanguageManager.currentTag()) }

    // Si la locale est changée ailleurs, on resynchronise l’icône
    LaunchedEffect(Unit) { currentTag.value = LanguageManager.currentTag() }

    Box(modifier = modifier) {
        // Bouton rond avec l’emoji drapeau
        Surface(
            shape = CircleShape,
            tonalElevation = 2.dp,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { expanded.value = true }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = flagForTag(currentTag.value),
                    fontSize = 20.sp
                )
            }
        }

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            LanguageMenuItem(
                tag = "fr",
                label = "Français",
                onPick = {
                    LanguageManager.setLanguage(context, "fr")
                    currentTag.value = "fr"
                    expanded.value = false
                    onChanged?.invoke()
                }
            )
            LanguageMenuItem(
                tag = "en",
                label = "English",
                onPick = {
                    LanguageManager.setLanguage(context, "en")
                    currentTag.value = "en"
                    expanded.value = false
                    onChanged?.invoke()
                }
            )
            LanguageMenuItem(
                tag = "de",
                label = "Deutsch",
                onPick = {
                    LanguageManager.setLanguage(context, "de")
                    currentTag.value = "de"
                    expanded.value = false
                    onChanged?.invoke()
                }
            )
            LanguageMenuItem(
                tag = "es",
                label = "Español",
                onPick = {
                    LanguageManager.setLanguage(context, "es")
                    currentTag.value = "es"
                    expanded.value = false
                    onChanged?.invoke()
                }
            )
        }
    }
}

@Composable
private fun LanguageMenuItem(
    tag: String,
    label: String,
    onPick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = flagForTag(tag), fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                Text(text = label)
            }
        },
        onClick = onPick
    )
}

private fun flagForTag(tag: String): String {
    return when (tag.lowercase(Locale.ROOT)) {
        "fr" -> "🇫🇷"
        // tu peux différencier "en-GB" / "en-US" si un jour tu gères les variantes
        "en", "en-gb", "en-us" -> "🇬🇧"
        "de" -> "🇩🇪"
        "es" -> "🇪🇸"
        else -> "🌐"
    }
}
