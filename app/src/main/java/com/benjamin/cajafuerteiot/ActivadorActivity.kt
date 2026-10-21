package com.benjamin.cajafuerteiot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla ACTIVADOR — el control remoto del dueño.
 * ENVÍA comandos a la caja (bloquear, modo, sensibilidad) y exige el PIN.
 * RECIBE en tiempo real el estado de la caja (puerta, alarma, intentos).
 *
 * Los datos se guardan en Firebase en: usuarios/{uid}/caja/...  (igual que la Sesión 9)
 */
class ActivadorActivity : AppCompatActivity() {

    // Firebase (Sesión 9)
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // Vistas
    private lateinit var textConexion: TextView
    private lateinit var switchBloqueada: MaterialSwitch
    private lateinit var radioGroupModo: RadioGroup
    private lateinit var textSensibilidad: TextView
    private lateinit var sliderSensibilidad: Slider
    private lateinit var layoutPin: TextInputLayout
    private lateinit var editTextPin: TextInputEditText
    private lateinit var textAyudaPin: TextView
    private lateinit var btnEnviar: Button
    private lateinit var cardEstadoRemoto: MaterialCardView
    private lateinit var textEstadoRemoto: TextView

    // Últimos datos recibidos de la caja
    private var alarmaActual = false
    private var intentosActuales = 0
    private var pinGuardado: String? = null
    private var pinCargado = false
    private var conectado = false
    private var controlesCargados = false

    // Referencias y listeners de Firebase (se guardan para quitarlos en onStop)
    private var refEstado: DatabaseReference? = null
    private var refPin: DatabaseReference? = null
    private var refConexion: DatabaseReference? = null
    private var listenerEstado: ValueEventListener? = null
    private var listenerPin: ValueEventListener? = null
    private var listenerConexion: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activador)
        title = "Activador"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = Firebase.auth
        database = Firebase.database.reference

        if (auth.currentUser == null) {
            volverAlLogin()
            return
        }

        textConexion = findViewById(R.id.textConexion)
        switchBloqueada = findViewById(R.id.switchBloqueada)
        radioGroupModo = findViewById(R.id.radioGroupModo)
        textSensibilidad = findViewById(R.id.textSensibilidad)
        sliderSensibilidad = findViewById(R.id.sliderSensibilidad)
        layoutPin = findViewById(R.id.layoutPin)
        editTextPin = findViewById(R.id.editTextPin)
        textAyudaPin = findViewById(R.id.textAyudaPin)
        btnEnviar = findViewById(R.id.btnEnviar)
        cardEstadoRemoto = findViewById(R.id.cardEstadoRemoto)
        textEstadoRemoto = findViewById(R.id.textEstadoRemoto)

        // Ciclo de vida (Sesión 7): al rotar, Android restaura solo los controles.
        if (savedInstanceState != null) {
            controlesCargados = true
        }

        // Slider: misma idea que el SeekBar de la Sesión 6
        sliderSensibilidad.addOnChangeListener { _, valor, _ ->
            textSensibilidad.text = "Sensibilidad del sensor: ${valor.toInt()}"
        }

        btnEnviar.setOnClickListener { enviarComando() }
    }

    // onStart(): la pantalla se ve → empezamos a escuchar Firebase
    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) return
        leerEstado()
        leerPin()
        escucharConexion()
    }

    // onStop(): la pantalla ya no se ve → dejamos de escuchar (ahorra batería y datos)
    override fun onStop() {
        listenerEstado?.let { refEstado?.removeEventListener(it) }
        listenerPin?.let { refPin?.removeEventListener(it) }
        listenerConexion?.let { refConexion?.removeEventListener(it) }
        super.onStop()
    }

    // ---------------------------------------------------------------- LEER DATOS (tiempo real)

    /** Escucha el estado de la caja: se dispara cada vez que cambia. */
    private fun leerEstado() {
        val uid = auth.currentUser?.uid ?: return
        // keepSynced: guarda una copia local de la caja (sirve sin internet)
        database.child("usuarios").child(uid).child("caja").keepSynced(true)

        val ref = database.child("usuarios").child(uid).child("caja").child("estado")
        refEstado = ref

        listenerEstado = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bloqueada = snapshot.child("bloqueada").getValue(Boolean::class.java) ?: true
                val modo = snapshot.child("modo").getValue(String::class.java) ?: "NORMAL"
                val sensibilidad = snapshot.child("sensibilidad").getValue(Int::class.java) ?: 50
                val puertaAbierta = snapshot.child("puertaAbierta").getValue(Boolean::class.java) ?: false
                val alarma = snapshot.child("alarma").getValue(Boolean::class.java) ?: false
                val intentos = snapshot.child("intentosFallidos").getValue(Int::class.java) ?: 0
                val actualizado = snapshot.child("actualizado").getValue(Long::class.java) ?: 0L

                alarmaActual = alarma
                intentosActuales = intentos

                // Lo que llega desde el otro dispositivo
                val cerradura = if (bloqueada) "🔒 Bloqueada" else "🔓 Desbloqueada"
                val puerta = if (puertaAbierta) "🚪 Abierta" else "Cerrada"
                val textoAlarma = if (alarma) "🚨 ACTIVADA" else "No"
                val hora = if (actualizado > 0) formatoFecha(actualizado) else "—"

                textEstadoRemoto.text = "Estado actual de la caja\n" +
                    "Cerradura: $cerradura\n" +
                    "Modo: ${nombreModo(modo)}\n" +
                    "Puerta: $puerta\n" +
                    "Alarma: $textoAlarma\n" +
                    "Intentos fallidos: $intentos/${Seguridad.MAX_INTENTOS}\n" +
                    "Actualizado: $hora"

                val color = if (alarma) R.color.rojo_claro else R.color.tarjeta
                cardEstadoRemoto.setCardBackgroundColor(ContextCompat.getColor(this@ActivadorActivity, color))

                // Solo la primera vez: llenar los controles con el estado actual
                if (!controlesCargados) {
                    switchBloqueada.isChecked = bloqueada
                    when (modo) {
                        "VACACIONES" -> radioGroupModo.check(R.id.rbVacaciones)
                        "ALARMA" -> radioGroupModo.check(R.id.rbAlarma)
                        else -> radioGroupModo.check(R.id.rbNormal)
                    }
                    sliderSensibilidad.value = sensibilidad.coerceIn(0, 100).toFloat()
                    textSensibilidad.text = "Sensibilidad del sensor: $sensibilidad"
                    controlesCargados = true
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ActivadorActivity, "Error al leer: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Escucha el PIN guardado (solo su hash). Si no existe, el primer PIN lo crea. */
    private fun leerPin() {
        val uid = auth.currentUser?.uid ?: return
        val ref = database.child("usuarios").child(uid).child("caja").child("config").child("pinHash")
        refPin = ref

        listenerPin = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pinGuardado = snapshot.getValue(String::class.java)
                pinCargado = true
                textAyudaPin.text = if (pinGuardado == null) {
                    "Primera vez: el PIN que escribas quedará configurado como PIN de la caja."
                } else {
                    "Ingresa tu PIN para confirmar el comando."
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ActivadorActivity, "Error al leer el PIN: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Indicador 🟢 / 🔴: ".info/connected" es true cuando hay conexión con Firebase. */
    private fun escucharConexion() {
        val ref = Firebase.database.getReference(".info/connected")
        refConexion = ref

        listenerConexion = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                conectado = snapshot.getValue(Boolean::class.java) ?: false
                if (conectado) {
                    textConexion.text = "🟢 Conectado"
                    textConexion.setTextColor(ContextCompat.getColor(this@ActivadorActivity, R.color.verde))
                } else {
                    textConexion.text = "🔴 Sin conexión"
                    textConexion.setTextColor(ContextCompat.getColor(this@ActivadorActivity, R.color.rojo))
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ---------------------------------------------------------------- ENVIAR COMANDOS

    private fun enviarComando() {
        val uid = auth.currentUser?.uid ?: return
        val pin = editTextPin.text.toString()

        if (!Seguridad.pinValido(pin)) {
            layoutPin.error = "El PIN debe tener entre ${Seguridad.PIN_MIN} y ${Seguridad.PIN_MAX} dígitos"
            return
        }
        layoutPin.error = null

        if (!pinCargado) {
            Toast.makeText(this, "Cargando configuración, intenta en un segundo", Toast.LENGTH_SHORT).show()
            return
        }

        val hashIngresado = Seguridad.hashPin(pin, uid)

        if (pinGuardado == null) {
            // Primera vez: se guarda SOLO el hash del PIN
            database.child("usuarios").child(uid).child("caja").child("config").child("pinHash")
                .setValue(hashIngresado)
            registrarEvento("PIN_CREADO", "Se configuró el PIN de la caja")
            Toast.makeText(this, "PIN creado. Lo necesitarás para cada comando.", Toast.LENGTH_SHORT).show()
            guardarComando()
        } else if (pinGuardado == hashIngresado) {
            guardarComando()
        } else {
            registrarIntentoFallido()
        }

        editTextPin.text?.clear()
    }

    /** PIN correcto: guarda el comando en Firebase y reinicia intentos y alarma. */
    private fun guardarComando() {
        val uid = auth.currentUser?.uid ?: return
        val bloqueada = switchBloqueada.isChecked
        val sensibilidad = sliderSensibilidad.value.toInt()
        val modo = when (radioGroupModo.checkedRadioButtonId) {
            R.id.rbVacaciones -> "VACACIONES"
            R.id.rbAlarma -> "ALARMA"
            else -> "NORMAL"
        }

        val cambios = hashMapOf<String, Any>(
            "bloqueada" to bloqueada,
            "modo" to modo,
            "sensibilidad" to sensibilidad,
            "intentosFallidos" to 0,
            "alarma" to false,
            "ultimoUsuario" to (auth.currentUser?.email ?: ""),
            "actualizado" to ServerValue.TIMESTAMP
        )

        // updateChildren cambia solo estos campos (no borra los demás)
        database.child("usuarios").child(uid).child("caja").child("estado")
            .updateChildren(cambios)
            .addOnFailureListener { error ->
                Toast.makeText(this, "No se pudo enviar: ${error.message}", Toast.LENGTH_SHORT).show()
            }

        if (alarmaActual) {
            registrarEvento("ALARMA_DESACTIVADA", "Alarma desactivada con PIN correcto")
        }
        val accion = if (bloqueada) "Bloqueada" else "Desbloqueada"
        registrarEvento(accion.uppercase(), "$accion · modo ${nombreModo(modo)} · sensibilidad $sensibilidad")

        if (conectado) {
            Toast.makeText(this, "Comando enviado ✅", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Sin conexión: el comando se enviará al reconectar", Toast.LENGTH_SHORT).show()
        }
    }

    /** PIN incorrecto: suma un intento; al tercero se activa la alarma. */
    private fun registrarIntentoFallido() {
        val uid = auth.currentUser?.uid ?: return
        val intentos = intentosActuales + 1
        val activarAlarma = Seguridad.debeActivarAlarma(intentos)

        val cambios = hashMapOf<String, Any>(
            "intentosFallidos" to intentos,
            "alarma" to (activarAlarma || alarmaActual),
            "actualizado" to ServerValue.TIMESTAMP
        )
        database.child("usuarios").child(uid).child("caja").child("estado").updateChildren(cambios)

        registrarEvento("PIN_INCORRECTO", "PIN incorrecto (intento $intentos de ${Seguridad.MAX_INTENTOS})")

        if (activarAlarma && !alarmaActual) {
            registrarEvento("ALARMA", "Alarma activada por $intentos intentos fallidos")
        }

        if (activarAlarma) {
            Toast.makeText(this, "🚨 PIN incorrecto: ALARMA ACTIVADA", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "PIN incorrecto ($intentos/${Seguridad.MAX_INTENTOS})", Toast.LENGTH_SHORT).show()
        }
    }

    /** Agrega un evento al historial. push() crea un id único (Sesión 9). */
    private fun registrarEvento(tipo: String, detalle: String) {
        val uid = auth.currentUser?.uid ?: return
        val evento = hashMapOf<String, Any>(
            "tipo" to tipo,
            "detalle" to detalle,
            "usuario" to (auth.currentUser?.email ?: ""),
            "fecha" to ServerValue.TIMESTAMP
        )
        database.child("usuarios").child(uid).child("caja").child("eventos").push().setValue(evento)
    }

    // ---------------------------------------------------------------- FUNCIONES DE APOYO

    private fun formatoFecha(milisegundos: Long): String {
        return SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(milisegundos))
    }

    private fun nombreModo(modo: String): String {
        return when (modo) {
            "VACACIONES" -> "Vacaciones"
            "ALARMA" -> "Alarma armada"
            else -> "Normal"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun volverAlLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
