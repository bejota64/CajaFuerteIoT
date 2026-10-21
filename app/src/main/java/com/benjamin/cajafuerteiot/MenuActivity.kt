package com.benjamin.cajafuerteiot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

/**
 * MENÚ: abre el Activador o el Monitoreo, y permite cerrar sesión.
 */
class MenuActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var textSaludo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        title = "Menú"

        auth = Firebase.auth

        if (auth.currentUser == null) {
            volverAlLogin()
            return
        }

        textSaludo = findViewById(R.id.textSaludo)
        textSaludo.text = "Hola, ${auth.currentUser?.email} 👋"

        findViewById<MaterialCardView>(R.id.cardActivador).setOnClickListener {
            startActivity(Intent(this, ActivadorActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardMonitoreo).setOnClickListener {
            startActivity(Intent(this, MonitoreoActivity::class.java))
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            volverAlLogin()
        }
    }

    private fun volverAlLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        // Borra el historial de pantallas: con "atrás" no se vuelve al menú sin login.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
