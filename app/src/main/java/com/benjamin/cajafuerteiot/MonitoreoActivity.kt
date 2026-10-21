package com.benjamin.cajafuerteiot

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla MONITOREO — el panel de la caja fuerte (el dispositivo IoT simulado).
 * MUESTRA en tiempo real lo que envía el Activador.
 * ENVÍA el estado del sensor de puerta (switch).
 * Detecta aperturas sospechosas y muestra el historial guardado en Firebase.
 */
class MonitoreoActivity : AppCompatActivity() {

    // Firebase (Sesión 9)
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // Vistas
    private lateinit var textConexion: TextView
    private lateinit var cardEstado: MaterialCardView
    private lateinit var textIconoEstado: TextView
    private lateinit var textEstadoCerradura: TextView
    private lateinit var textActualizado: TextView
    private lateinit var textModo: TextView
    private lateinit var cardAlarma: MaterialCardView
    private lateinit var textAlarma: TextView
    private lateinit var textPuerta: TextView
    private lateinit var textSensibilidad: TextView
    private lateinit var switchPuerta: MaterialSwitch
    private lateinit var textHistorial: TextView

    // Últimos datos recibidos
    private var bloqueadaActual = true
    private var modoActual = "NORMAL"
    private var estadoRecibido = false   // true cuando llegó el primer dato real de Firebase

    // Referencias y listeners (se quitan en onStop)
    private var refEstado: DatabaseReference? = null
    private var consultaHistorial: Query? = null
    private var refConexion: DatabaseReference? = null
    private var listenerEstado: ValueEventListener? = null
    private var listenerHistorial: ValueEventListener? = null
    private var listenerConexion: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitoreo)
        title = "Monitoreo"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = Firebase.auth
        database = Firebase.database.reference

        if (auth.currentUser == null) {
            volverAlLogin()
            return
        }

        textConexion = findViewById(R.id.textConexion)
        cardEstado = findViewById(R.id.cardEstado)
        textIconoEstado = findViewById(R.id.textIconoEstado)
        textEstadoCerradura = findViewById(R.id.textEstadoCerradura)
        textActualizado = findViewById(R.id.textActualizado)
        textModo = findViewById(R.id.textModo)
        cardAlarma = findViewById(R.id.cardAlarma)
        textAlarma = findViewById(R.id.textAlarma)
        textPuerta = findViewById(R.id.textPuerta)
        textSensibilidad = findViewById(R.id.textSensibilidad)
        switchPuerta = findViewById(R.id.switchPuerta)
        textHistorial = findViewById(R.id.textHistorial)

        // setOnClickListener: solo se ejecuta cuando la PERSONA toca el switch
        // (no al rotar la pantalla ni cuando llegan datos). Error E1 corregido, ver PRUEBAS.md.
        switchPuerta.setOnClickListener {
            if (!estadoRecibido) {
                switchPuerta.isChecked = !switchPuerta.isChecked
                Toast.makeText(this, "Esperando datos de la caja...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            cambiarSensorPuerta(switchPuerta.isChecked)
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) return
        leerEstado()
        leerHistorial()
        escucharConexion()
    }

    override fun onStop() {
        listenerEstado?.let { refEstado?.removeEventListener(it) }
        listenerHistorial?.let { consultaHistorial?.removeEventListener(it) }
        listenerConexion?.let { refConexion?.removeEventListener(it) }
        super.onStop()
    }

    // ---------------------------------------------------------------- LEER DATOS (tiempo real)

    /** Escucha el estado de la caja y lo muestra en las tarjetas. */
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
                val actualizado = snapshot.child("actualizado").getValue(Long::class.java) ?: 0L

                bloqueadaActual = bloqueada
                modoActual = modo
                estadoRecibido = true

                // Mover el switch desde el código NO dispara setOnClickListener
                switchPuerta.isChecked = puertaAbierta

                if (alarma) {
                    textIconoEstado.text = "🚨"
                    textEstadoCerradura.text = "ALARMA"
                } else if (bloqueada) {
                    textIconoEstado.text = "🔒"
                    textEstadoCerradura.text = "BLOQUEADA"
                } else {
                    textIconoEstado.text = "🔓"
                    textEstadoCerradura.text = "DESBLOQUEADA"
                }

                textActualizado.text = if (actualizado > 0) {
                    "Actualizado: ${formatoFecha(actualizado)}"
                } else {
                    "Sin datos todavía"
                }

                textModo.text = "Modo\n${nombreModo(modo)}"
                textAlarma.text = "Alarma\n${if (alarma) "🚨 ACTIVADA" else "No"}"
                textPuerta.text = "Puerta\n${if (puertaAbierta) "Abierta" else "Cerrada"}"
                textSensibilidad.text = "Sensibilidad\n$sensibilidad"

                val colorEstado = if (alarma) R.color.rojo_claro else if (bloqueada) R.color.verde_claro else R.color.morado_claro
                cardEstado.setCardBackgroundColor(ContextCompat.getColor(this@MonitoreoActivity, colorEstado))
                val colorAlarma = if (alarma) R.color.rojo_claro else R.color.tarjeta
                cardAlarma.setCardBackgroundColor(ContextCompat.getColor(this@MonitoreoActivity, colorAlarma))
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MonitoreoActivity, "Error al leer: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Escucha los últimos 20 eventos del historial (del más nuevo al más antiguo). */
    private fun leerHistorial() {
        val uid = auth.currentUser?.uid ?: return
        val consulta = database.child("usuarios").child(uid).child("caja").child("eventos")
            .orderByChild("fecha").limitToLast(20)
        consultaHistorial = consulta

        listenerHistorial = consulta.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lineas = mutableListOf<String>()
                for (evento in snapshot.children) {
                    val fecha = evento.child("fecha").getValue(Long::class.java) ?: 0L
                    val detalle = evento.child("detalle").getValue(String::class.java) ?: ""
                    lineas.add("${formatoFecha(fecha)} · $detalle")
                }
                lineas.reverse() // el más nuevo primero

                textHistorial.text = if (lineas.isEmpty()) "Sin eventos todavía" else lineas.joinToString("\n")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MonitoreoActivity, "Error al leer el historial: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Indicador 🟢 / 🔴 de conexión con Firebase. */
    private fun escucharConexion() {
        val ref = Firebase.database.getReference(".info/connected")
        refConexion = ref

        listenerConexion = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conectado = snapshot.getValue(Boolean::class.java) ?: false
                if (conectado) {
                    textConexion.text = "🟢 Conectado"
                    textConexion.setTextColor(ContextCompat.getColor(this@MonitoreoActivity, R.color.verde))
                } else {
                    textConexion.text = "🔴 Sin conexión"
                    textConexion.setTextColor(ContextCompat.getColor(this@MonitoreoActivity, R.color.rojo))
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ---------------------------------------------------------------- SENSOR DE PUERTA

    /** El "sensor" detectó un cambio en la puerta: se guarda y le llega al Activador. */
    private fun cambiarSensorPuerta(abierta: Boolean) {
        val uid = auth.currentUser?.uid ?: return

        val cambios = hashMapOf<String, Any>(
            "puertaAbierta" to abierta,
            "actualizado" to ServerValue.TIMESTAMP
        )

        registrarEvento(
            if (abierta) "PUERTA_ABIERTA" else "PUERTA_CERRADA",
            if (abierta) "Sensor: puerta abierta" else "Sensor: puerta cerrada"
        )

        // Puerta abierta con la caja bloqueada o con alarma armada → alarma
        if (Seguridad.aperturaSospechosa(abierta, bloqueadaActual, modoActual)) {
            cambios["alarma"] = true
            registrarEvento("FORZAMIENTO", "¡Puerta abierta con la caja bloqueada o armada! Posible forzamiento")
            Toast.makeText(this, "🚨 Apertura sospechosa: ALARMA ACTIVADA", Toast.LENGTH_SHORT).show()
        }

        database.child("usuarios").child(uid).child("caja").child("estado")
            .updateChildren(cambios)
            .addOnFailureListener { error ->
                Toast.makeText(this, "No se pudo enviar el sensor: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** Agrega un evento al historial con push() (Sesión 9). */
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
