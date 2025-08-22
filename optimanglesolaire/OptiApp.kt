package com.fredz.optimanglesolaire

import android.app.Application
import com.fredz.optimanglesolaire.core.LanguageManager

class OptiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Applique la langue sauvegardée dès le démarrage
        LanguageManager.applySaved(this)
    }
}
