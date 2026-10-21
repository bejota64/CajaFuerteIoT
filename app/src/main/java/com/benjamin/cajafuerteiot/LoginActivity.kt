package com.benjamin.cajafuerteiot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

/**
 * Pantalla de LOGIN — mismo patrón de la Sesión 9:
 * registrarUsuario() e iniciarSesion() con Firebase Authentication.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var layoutCorreo: TextInputLayout
    private lateinit var layoutPassword: TextInputLayout
    private lateinit var editTextCorreo: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var btnRegistrar: Button
    private lateinit var progreso: ProgressBar
    private lateinit var textViewEstado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()

        auth = Firebase.auth

        // Si ya hay sesión activa, no se vuelve a pedir el login (Sesión 9).
        if (auth.currentUser != null) {
            irAlMenu()
            return
        }

        layoutCorreo = findViewById(R.id.layoutCorreo)
        layoutPassword = findViewById(R.id.layoutPassword)
        editTextCorreo = findViewById(R.id.editTextCorreo)
        editTextPassword = findViewById(R.id.editTextPassword)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        progreso = findViewById(R.id.progreso)
        textViewEstado = findViewById(R.id.textViewEstado)

        btnRegistrar.setOnClickListener { registrarUsuario() }
        btnIniciarSesion.setOnClickListener { iniciarSesion() }
    }

    /** Registra un usuario nuevo. Firebase guarda la contraseña cifrada. */
    private fun registrarUsuario() {
        val correo = editTextCorreo.text.toString().trim()
        val password = editTextPassword.text.toString()

        if (!camposValidos(correo, password)) return
        mostrarCargando(true)

        auth.createUserWithEmailAndPassword(correo, password)
            .addOnCompleteListener(this) { tarea ->
                mostrarCargando(false)
                if (tarea.isSuccessful) {
                    Toast.makeText(this, "Usuario registrado", Toast.LENGTH_SHORT).show()
                    irAlMenu()
                } else {
                    textViewEstado.text = "Error al registrar: ${tarea.exception?.message}"
                }
            }
    }

    /** Inicia sesión con un usuario que ya existe. */
    private fun iniciarSesion() {
        val correo = editTextCorreo.text.toString().trim()
        val password = editTextPassword.text.toString()

        if (!camposValidos(correo, password)) return
        mostrarCargando(true)

        auth.signInWithEmailAndPassword(correo, password)
            .addOnCompleteListener(this) { tarea ->
                mostrarCargando(false)
                if (tarea.isSuccessful) {
                    irAlMenu()
                } else {
                    // Seguridad: no se dice si falló el correo o la contraseña.
                    textViewEstado.text = "Correo o contraseña incorrectos"
                }
            }
    }

    /** Revisa los campos ANTES de llamar a Firebase y muestra el error bajo cada uno. */
    private fun camposValidos(correo: String, password: String): Boolean {
        var valido = true

        if (!Seguridad.correoValido(correo)) {
            layoutCorreo.error = "Escribe un correo válido"
            valido = false
        } else {
            layoutCorreo.error = null
        }

        if (!Seguridad.passwordValida(password)) {
            layoutPassword.error = "Mínimo ${Seguridad.PASSWORD_MIN} caracteres"
            valido = false
        } else {
            layoutPassword.error = null
        }

        return valido
    }

    /** Muestra la ruedita de carga y bloquea los botones mientras Firebase responde. */
    private fun mostrarCargando(cargando: Boolean) {
        progreso.visibility = if (cargando) View.VISIBLE else View.GONE
        btnIniciarSesion.isEnabled = !cargando
        btnRegistrar.isEnabled = !cargando
        if (cargando) textViewEstado.text = ""
    }

    private fun irAlMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }
}
