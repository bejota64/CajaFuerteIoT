package com.benjamin.cajafuerteiot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de las reglas de seguridad.
 * Se ejecutan en el computador (no necesitan emulador):
 * clic derecho sobre esta clase → Run 'SeguridadTest'.
 */
class SeguridadTest {

    @Test
    fun pinValido_aceptaEntre4y6Digitos() {
        assertTrue(Seguridad.pinValido("1234"))
        assertTrue(Seguridad.pinValido("123456"))
    }

    @Test
    fun pinValido_rechazaFormatosIncorrectos() {
        assertFalse(Seguridad.pinValido("123"))      // muy corto
        assertFalse(Seguridad.pinValido("1234567"))  // muy largo
        assertFalse(Seguridad.pinValido("12a4"))     // contiene letras
        assertFalse(Seguridad.pinValido(""))         // vacío
    }

    @Test
    fun hashPin_esDeterministaYNoGuardaElPinEnTextoPlano() {
        val hash1 = Seguridad.hashPin("1234", "uidA")
        val hash2 = Seguridad.hashPin("1234", "uidA")
        assertEquals(hash1, hash2)          // mismo PIN + mismo usuario = mismo hash
        assertEquals(64, hash1.length)      // SHA-256 en hexadecimal = 64 caracteres
        assertFalse(hash1.contains("1234")) // el PIN no aparece en el hash
    }

    @Test
    fun hashPin_mismoPinDistintoUsuario_daHashDistinto() {
        assertNotEquals(Seguridad.hashPin("1234", "uidA"), Seguridad.hashPin("1234", "uidB"))
    }

    @Test
    fun alarma_seActivaAlTercerIntento() {
        assertFalse(Seguridad.debeActivarAlarma(1))
        assertFalse(Seguridad.debeActivarAlarma(2))
        assertTrue(Seguridad.debeActivarAlarma(3))
        assertTrue(Seguridad.debeActivarAlarma(4))
    }

    @Test
    fun aperturaSospechosa_detectaForzamiento() {
        // Puerta abierta con la caja bloqueada → sospechoso
        assertTrue(Seguridad.aperturaSospechosa(true, true, "NORMAL"))
        // Puerta abierta con alarma armada → sospechoso
        assertTrue(Seguridad.aperturaSospechosa(true, false, "ALARMA"))
        // Puerta abierta, desbloqueada y modo normal → uso normal
        assertFalse(Seguridad.aperturaSospechosa(true, false, "NORMAL"))
        // Puerta cerrada → nunca es sospechoso
        assertFalse(Seguridad.aperturaSospechosa(false, true, "ALARMA"))
    }

    @Test
    fun credenciales_validacionDeCorreoYContrasena() {
        assertTrue(Seguridad.correoValido("dueno@correo.cl"))
        assertFalse(Seguridad.correoValido("dueno@"))
        assertFalse(Seguridad.correoValido("sin-arroba.cl"))
        assertTrue(Seguridad.passwordValida("123456"))
        assertFalse(Seguridad.passwordValida("12345"))
    }
}
