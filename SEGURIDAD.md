# 🔐 Seguridad — Caja Fuerte IoT

Documento de **verificación y cumplimiento** de seguridad (rúbrica 2.1.3.5 y 2.1.3.6).
Describe qué medidas tiene la app, qué norma respalda cada una, **dónde están en el código** y **cómo se verificó** que funcionan.

---

## 1. Contexto: por qué esta app es un caso OT

La app no solo muestra información: **controla un dispositivo físico** (la cerradura de una caja fuerte, aquí simulada en un segundo dispositivo Android). En la industria esto se llama **OT (Operational Technology)**. Un error o un ataque no solo filtra datos: puede **abrir la caja**. Por eso, además de las normas de seguridad de la información (ISO/IEC 27001) y de IoT (ISO/IEC 27400), se siguen los lineamientos de **IEC 62443**, el estándar de seguridad para sistemas de control industrial.

| Norma | Qué cubre | Cómo se aplica aquí |
|---|---|---|
| **ISO/IEC 27400:2022** | Seguridad y privacidad en dispositivos y servicios IoT | Credenciales, cifrado de comunicaciones y control de acceso al dispositivo |
| **ISO/IEC 27001:2022** (Anexo A) | Controles de seguridad de la información | Control de acceso, autenticación, criptografía, registros y desarrollo seguro |
| **IEC 62443-3-3** | Seguridad de sistemas de automatización y control industrial (OT) | Sus 7 requisitos fundamentales (FR1 a FR7) se usan como lista de verificación |

**Requisitos fundamentales de IEC 62443 (FR):**
- FR1: Identificación y autenticación
- FR2: Control de uso
- FR3: Integridad del sistema
- FR4: Confidencialidad de los datos
- FR5: Flujo restringido de datos
- FR6: Respuesta oportuna a eventos
- FR7: Disponibilidad de recursos

---

## 2. Medidas implementadas

| # | Medida | Riesgo que mitiga | Dónde está en el código | ISO/IEC 27001 / 27400 | IEC 62443 |
|---|---|---|---|---|---|
| S1 | **Login con correo y contraseña** (Firebase Authentication). Las contraseñas se guardan **cifradas (hash)** en Firebase, nunca en la app | Acceso de personas no autorizadas | `LoginActivity.kt` → `registrarUsuario()`, `iniciarSesion()` | 27001 A.5.17, A.8.5 · 27400: credenciales | FR1 |
| S2 | **Validación de entradas** (formato del correo, contraseña de 6 caracteres o más, PIN de 4 a 6 dígitos) antes de llamar a Firebase | Datos inválidos, errores y abuso | `Seguridad.kt` → `correoValido()`, `passwordValida()`, `pinValido()` | 27001 A.8.28 (codificación segura) | FR3 |
| S3 | **Mensaje genérico de error de login**: no se dice si falló el correo o la contraseña | Que un atacante averigüe qué correos están registrados | `LoginActivity.kt` → `iniciarSesion()` | 27001 A.8.5 | FR1 |
| S4 | **Reglas de seguridad de Realtime Database**: cada usuario solo puede leer y escribir **su propio nodo** `usuarios/{uid}` | Que otro usuario lea o controle la caja | `firebase/database.rules.json` | 27001 A.5.15, A.8.3 · 27400: control de acceso | FR2, FR5 |
| S5 | **Validación en el servidor** (reglas `.validate`): tipos de datos, modos permitidos, sensibilidad entre 0 y 100, y rechazo de campos desconocidos | Que se inyecten datos falsos o maliciosos aunque se salten la app | `firebase/database.rules.json` | 27001 A.8.28 | FR3 |
| S6 | **Comunicación cifrada HTTPS/TLS** (Firebase) y prohibición de tráfico sin cifrar | Que alguien intercepte o modifique los comandos en la red Wi-Fi | `AndroidManifest.xml` → `usesCleartextTraffic="false"` | 27001 A.8.20, A.8.24 · 27400: comunicaciones | FR4 |
| S7 | **PIN obligatorio para cada comando**: segundo factor para acciones críticas. El PIN se guarda solo como **hash SHA-256 con sal** (el uid del usuario) | Que alguien use el celular desbloqueado del dueño para abrir la caja | `ActivadorActivity.kt` → `enviarComando()` · `Seguridad.kt` → `hashPin()` | 27001 A.8.24, A.5.17 | FR1, FR2 |
| S8 | **Bloqueo por intentos**: al 3er PIN incorrecto se activa la **alarma** en ambos dispositivos | Ataques de fuerza bruta al PIN | `ActivadorActivity.kt` → `registrarIntentoFallido()` · `Seguridad.kt` → `debeActivarAlarma()` | 27001 A.8.5 | FR1, FR6 |
| S9 | **Detección de forzamiento**: si la puerta se abre con la caja bloqueada o con la alarma armada, se activa la alarma | Apertura física no autorizada | `MonitoreoActivity.kt` → `cambiarSensorPuerta()` · `Seguridad.kt` → `aperturaSospechosa()` | 27001 A.8.16 (monitoreo) | FR6 |
| S10 | **Historial de eventos** (quién, qué y cuándo): comandos, PIN incorrectos, alarmas y cambios de la puerta | Falta de trazabilidad ante un incidente | `ActivadorActivity.kt` y `MonitoreoActivity.kt` → `registrarEvento()` | 27001 A.8.15 (registro) | FR6 |
| S11 | **Cierre de sesión** y protección de pantallas: sin sesión, toda pantalla vuelve al Login y se borra el historial de navegación | Sesión abierta en un celular prestado o perdido | `MenuActivity.kt` → `signOut()` · `volverAlLogin()` en cada pantalla | 27001 A.5.15 | FR2 |
| S12 | **Disponibilidad sin conexión**: caché local, cola de comandos y reconexión automática, con indicador 🟢/🔴 | Pérdida de control o de información si se corta la red | `CajaApp.kt` → `setPersistenceEnabled(true)` · `ActivadorActivity.kt` / `MonitoreoActivity.kt` → `keepSynced(true)`, `escucharConexion()` | 27001 A.8.14 (redundancia) | FR7 |
| S13 | **Sin respaldos automáticos de datos** de la app y **gestión de secretos**: `google-services.json` y las firmas no se suben a GitHub | Filtración de configuración o datos en respaldos y repositorios públicos | `AndroidManifest.xml` → `allowBackup="false"` · `.gitignore` | 27001 A.8.12 (fuga de datos) | FR4 |
| S14 | **Liberación de recursos según el ciclo de vida**: los listeners se quitan en `onStop()` | Consumo innecesario de batería y datos, y fallas | `ActivadorActivity.kt` y `MonitoreoActivity.kt` → `onStop()` (`removeEventListener`) | 27001 A.8.25 (desarrollo seguro) | FR7 |
| S15 | **Permisos mínimos**: solo `INTERNET` y `ACCESS_NETWORK_STATE` (permisos normales), sin permisos peligrosos | Acceso innecesario a datos del celular (principio de mínimo privilegio) | `AndroidManifest.xml` | 27001 A.8.2 · 27400: privacidad | FR2 |

---

## 3. Verificación

Cada medida se verificó el 20/10/2026 con el procedimiento indicado, usando el celular Motorola moto g5 (Android 14), un emulador (API 37) y la consola de Firebase. Las capturas están en la carpeta [`evidencias/`](evidencias/).

| # | Procedimiento de verificación | Resultado esperado | Resultado y evidencia |
|---|---|---|---|
| S1 | Abrir Firebase Console → Authentication → Método de acceso | Correo/contraseña habilitado; las contraseñas no son visibles en la consola | ✅ `S1_authentication_habilitada.png` |
| S2 | En el Login, escribir una contraseña de 5 caracteres | Error "Mínimo 6 caracteres" bajo el campo, sin llamar a Firebase | ✅ `S2_validacion_password_corta.png` |
| S3 | Iniciar sesión con credenciales incorrectas (incluso con un correo que no existe) | Solo dice "Correo o contraseña incorrectos" | ✅ Mismo mensaje genérico con un correo inexistente: no revela qué cuentas existen — `P13_S3_credenciales_incorrectas.png` |
| S4 | Iniciar sesión con **otra** cuenta y abrir el Monitoreo | No ve ni recibe los datos de la caja del dueño | ✅ La otra cuenta vio una caja vacía y no recibió los comandos — `S4_otra_cuenta_ve_caja_vacia_aislamiento.png` · reglas publicadas: `S4_reglas_publicadas.png` |
| S4b | *Zona de pruebas de reglas*: `get` en `/usuarios` **sin autenticación** | **Denegado** | ✅ "Se rechazó la operación de lectura simulada" — `S4b_lectura_sin_login_rechazada.png` |
| S5 | *Zona de pruebas de reglas*: `set` en `/usuarios/abc/caja/estado` con `{"sensibilidad": 500}`, autenticado como uid `abc` (el dueño del nodo) | **Denegado** por `.validate`, aunque sea el dueño | ✅ "Se rechazó la operación de escritura simulada" — `S5_escritura_dato_invalido_rechazada.png` |
| S6 | Revisar el manifest (`usesCleartextTraffic="false"`) y la URL de la base de datos | Solo conexiones cifradas (https) | ✅ `https://cajafuerte-5b586-default-rtdb.firebaseio.com` — `S6_url_firebase_https.png` |
| S7 | Firebase Console → Datos → `caja/config/pinHash` | Se ve un texto de 64 caracteres, **no** el PIN | ✅ `S7_pinHash_cifrado.png` |
| S8 | En el Activador, enviar 3 veces un PIN incorrecto | Al 3er intento: "ALARMA ACTIVADA" en ambos dispositivos | ✅ `P9_alarma_activador_celular.png` · `P9_alarma_monitoreo_emulador.png` |
| S9 | Con la caja bloqueada, activar en el Monitoreo el switch "puerta abierta" | Alarma activada y evento "Posible forzamiento" | ✅ Probado 3 veces — `P11_forzamiento_alarma.png` |
| S10 | Revisar el historial del Monitoreo después de las pruebas | Aparecen todos los eventos con fecha (comandos, PIN incorrectos, alarmas, puerta) | ✅ `P9_P10_historial_alarma_y_desactivacion.png` · `P6_estado_actualizado_al_volver.png` |
| S11 | Cerrar sesión y presionar "atrás" | No se puede volver al menú sin iniciar sesión | ✅ `P12_sin_sesion_queda_en_login.png` |
| S12 | Ver `PRUEBAS.md`, escenario P5 (modo avión) | Indicador 🔴, cola de comandos y sincronización al reconectar | ✅ `P5_1_sin_conexion_celular.png` · `P5_2_reconectado_celular.png` · `P5_3_comando_en_cola_llega_al_emulador.png` |
| S13 | Revisar en GitHub que **no** está `app/google-services.json` | El archivo no aparece en el repositorio | ☐ |
| S15 | En el celular: Ajustes → Apps → Caja Fuerte IoT → Permisos | "No se solicitaron permisos" | ✅ `S15_no_se_solicitaron_permisos.png` · `S15_sin_notificaciones.png` |

**Pruebas unitarias automáticas** (`app/src/test/.../SeguridadTest.kt`): validan el formato del PIN, que el hash no contenga el PIN, la sal por usuario, la activación de la alarma al tercer intento, la detección de forzamiento y la validación de credenciales.
Para ejecutarlas en Android Studio: clic derecho sobre `SeguridadTest` → **Run**. Resultado esperado: **7/7 en verde** ✅ — ejecutadas con `.\gradlew.bat :app:testDebugUnitTest`: BUILD SUCCESSFUL (UNIT_tests_gradle_build_successful.png)

---

## 4. Reglas de Firebase aplicadas

Archivo [`firebase/database.rules.json`](firebase/database.rules.json), publicado en Firebase Console → Realtime Database → Reglas. Reemplaza el "modo de prueba", que deja la base de datos abierta a cualquiera.

```json
"usuarios": {
  "$uid": {
    ".read":  "auth != null && auth.uid === $uid",
    ".write": "auth != null && auth.uid === $uid",
    ...
```

**En simple:** solo un usuario autenticado puede leer y escribir, y solo dentro de su propio nodo. Los dos dispositivos inician sesión con **la cuenta del dueño**, por eso comparten la caja; cualquier otra cuenta queda fuera.

---

## 5. Riesgos residuales y mejoras futuras

Ningún sistema es 100% seguro. Estos son los riesgos que quedan y cómo se abordarían:

| Riesgo residual | Mitigación actual | Mejora futura |
|---|---|---|
| Un PIN de 4 a 6 dígitos con SHA-256 podría adivinarse por fuerza bruta si alguien obtuviera el hash | Las reglas impiden leer el hash a otros usuarios, y hay alarma al tercer intento | Usar un algoritmo lento (PBKDF2/bcrypt) y validar el PIN en el servidor (Cloud Functions) |
| El contador de intentos lo actualiza la app | Las reglas validan el tipo de dato | Llevar el control de intentos al servidor |
| Se usa una sola cuenta (la del dueño) en ambos dispositivos | Login y reglas por uid | Roles (dueño e invitado) con permisos distintos |
| Una app modificada podría usar las credenciales del proyecto | Reglas por usuario | Activar **Firebase App Check** |
| Sin internet no hay control remoto | Caché local y cola de comandos | Modo de respaldo por **Bluetooth** directo (Sesión 8) |
