package com.benjamin.cajafuerteiot

import java.security.MessageDigest

/**
 * Funciones de seguridad que RETORNAN un resultado (como evaluarTemperatura() de la Sesión 6).
 * Están en un archivo aparte para poder probarlas con pruebas unitarias
 * (ver app/src/test/.../SeguridadTest.kt).
 */
object Seguridad {

    const val MAX_INTENTOS = 3      // intentos de PIN antes de activar la alarma
    const val PIN_MIN = 4
    const val PIN_MAX = 6
    const val PASSWORD_MIN = 6      // mínimo que exige Firebase Authentication

    /** Valida el formato del correo antes de llamar a Firebase. */
    fun correoValido(correo: String): Boolean {
        val patron = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return patron.matches(correo.trim())
    }

    /** Firebase exige al menos 6 caracteres. */
    fun passwordValida(password: String): Boolean {
        return password.length >= PASSWORD_MIN
    }

    /** El PIN debe tener entre 4 y 6 dígitos, solo números. */
    fun pinValido(pin: String): Boolean {
        return pin.length in PIN_MIN..PIN_MAX && pin.all { it.isDigit() }
    }

    /**
     * Cifra el PIN con SHA-256 antes de guardarlo: en Firebase NUNCA queda el PIN real.
     * Se usa el uid del usuario como "sal": el mismo PIN da un hash distinto para cada usuario.
     */
    fun hashPin(pin: String, sal: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$sal:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** true cuando se llega al máximo de intentos fallidos. */
    fun debeActivarAlarma(intentosFallidos: Int): Boolean {
        return intentosFallidos >= MAX_INTENTOS
    }

    /** Puerta abierta con la caja bloqueada o con alarma armada → posible forzamiento. */
    fun aperturaSospechosa(puertaAbierta: Boolean, bloqueada: Boolean, modo: String): Boolean {
        return puertaAbierta && (bloqueada || modo == "ALARMA")
    }
}
