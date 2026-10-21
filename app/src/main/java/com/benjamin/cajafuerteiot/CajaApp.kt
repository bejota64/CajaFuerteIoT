package com.benjamin.cajafuerteiot

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.database.database

/**
 * Clase Application: se ejecuta UNA vez al abrir la app, antes que cualquier pantalla.
 * (Se registra en AndroidManifest.xml con android:name=".CajaApp").
 */
class CajaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Caché sin conexión de Firebase: si se pierde internet, la app sigue mostrando
        // el último estado conocido y guarda los comandos en una cola que se envía sola
        // al reconectar. Debe activarse ANTES de cualquier otro uso de la base de datos.
        Firebase.database.setPersistenceEnabled(true)
    }
}
