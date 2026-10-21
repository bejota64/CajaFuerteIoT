# 🧪 Pruebas de funcionamiento — Caja Fuerte IoT

Plan y registro de pruebas de **interconexión, estabilidad y eficiencia** (rúbrica 2.1.4.7 y 2.1.4.8).

**Entorno de prueba:**

| Elemento | Detalle |
|---|---|
| Dispositivo A (Activador) | Celular físico Motorola moto g5 · Android 14 (API 34), conectado por USB para instalar |
| Dispositivo B (Monitoreo) | Emulador Android Studio "Medium Phone" · API 37 (Google Play) |
| Conexión | Celular: Wi-Fi → Internet · Emulador: red del PC → Internet · ambos → Firebase (HTTPS/TLS) |
| Cuenta | La misma cuenta del dueño en ambos dispositivos |
| Fecha de pruebas | 20 / 10 / 2026 |
| Nota sobre las horas | El historial del emulador muestra la hora en UTC (por ejemplo, 23:37 corresponde a las 20:37 de Chile) |

> Cómo completar: ejecuta cada prueba, anota el resultado (✅ / ❌) y el tiempo medido, y guarda una captura en la carpeta `evidencias/`.

---

## 1. Interconexión entre dispositivos (2.1.4.7)

| # | Escenario | Pasos | Resultado esperado | Tiempo | Resultado |
|---|---|---|---|---|---|
| P1 | Login en ambos dispositivos | Iniciar sesión con la misma cuenta en A y en B | Ambos llegan al Menú | — | ✅ (P0_app_instalada_celular_motorola.png) |
| P2 | Comando A → B | En A: desbloquear, modo Vacaciones, sensibilidad 80, PIN → Enviar | En B, la tarjeta cambia a 🔓 DESBLOQUEADA, Vacaciones, 80 | — | ✅ (P2_P3_activador_celular.png · P2_P3_P4_monitoreo_emulador.png) |
| P3 | Sensor B → A | En B: activar el switch "puerta abierta" | En A, "Puerta: 🚪 Abierta" en el estado actual | — | ✅ (P2_P3_activador_celular.png · P2_P3_P4_monitoreo_emulador.png) |
| P4 | Almacenamiento y consulta | Revisar el historial en B y Firebase Console → Data | Los eventos de P2 y P3 aparecen con fecha y usuario | — | ✅ (P2_P3_P4_monitoreo_emulador.png) |

## 2. Estabilidad en diferentes escenarios de uso (2.1.4.8)

| # | Escenario | Pasos | Resultado esperado | Resultado |
|---|---|---|---|---|
| P5 | **Pérdida de conexión y reconexión** | En A, bajar la cortina → activar **Modo avión** → enviar un comando con PIN → desactivar el modo avión | Con modo avión: indicador 🔴 "Sin conexión" y mensaje "se enviará al reconectar". Al reconectar: 🟢, y **B recibe el comando solo**, sin reenviarlo | ✅ (P5_1_sin_conexion_celular.png · P5_2_reconectado_celular.png · P5_3_comando_en_cola_llega_al_emulador.png) |
| P6 | **App en segundo plano** | En B presionar Home → en A enviar un comando → volver a abrir B | B muestra el estado nuevo al volver, sin errores | ✅ El celular envió "Bloqueada · Vacaciones · 52" (23:37:10) mientras el emulador no estaba mostrando esa caja; al volver, el Monitoreo mostró el estado actualizado y el historial completo (P6_estado_actualizado_al_volver.png) |
| P7 | **Rotación de pantalla** | Rotar A y B (botón de rotar del emulador) | No se cierra la app; se mantiene el estado y los controles | ❌ → corregido (sección 5) · **re-prueba ✅** (P7_reprueba_rotacion_sin_falsa_alarma.png · P7_reprueba_historial_sin_eventos_falsos.png) |
| P8 | **Comandos rápidos repetidos** | En A, enviar 5 comandos seguidos alternando bloquear y desbloquear | B termina mostrando el estado del **último** comando enviado; la app no se congela | ✅ 6 comandos en 11 s (23:43:44 a 23:43:55) alternando bloquear y desbloquear: todos llegaron en orden y el estado final coincide con el último (BLOQUEADA) (P8_comandos_rapidos.png) |
| P9 | **PIN incorrecto (seguridad)** | En A, enviar 3 veces un PIN incorrecto | Avisa 1/3 y 2/3; al tercero, "ALARMA ACTIVADA" y B se pone en rojo 🚨 | ✅ (P9_alarma_activador_celular.png · P9_alarma_monitoreo_emulador.png) |
| P10 | **Recuperación de la alarma** | En A, enviar un comando con el PIN correcto | Se desactiva la alarma en ambos y se reinician los intentos (0/3) | ✅ (P9_P10_historial_alarma_y_desactivacion.png: "Alarma desactivada con PIN correcto") |
| P11 | **Apertura sospechosa** | Con la caja bloqueada, en B abrir la puerta (switch) | Alarma en ambos dispositivos y evento "Posible forzamiento" | ✅ Con la caja bloqueada, abrir la puerta activó la alarma y el aviso "Apertura sospechosa" (probado 3 veces: 23:44:54, 23:45:06 y 23:45:24); se desactivó con el PIN correcto (P11_forzamiento_alarma.png · P11_alarma_desactivada_y_puerta_cerrada.png) |
| P12 | **Acceso sin sesión** | Cerrar sesión en A y presionar "atrás" | Vuelve al Login; no se puede entrar a Activador ni Monitoreo | ✅ Después de cerrar sesión y presionar atrás, la app queda en el Login (P12_sin_sesion_queda_en_login.png) |
| P13 | **Credenciales incorrectas** | Iniciar sesión con una contraseña incorrecta | "Correo o contraseña incorrectos"; la app no se cierra | ✅ Se muestra "Correo o contraseña incorrectos" y la app no se cierra (P13_S3_credenciales_incorrectas.png) |
| P14 | **Reinicio de la app** | Cerrar la app por completo en B (deslizar desde recientes) y abrirla | Entra directo al Menú (sesión recordada) y muestra el último estado | ✅ Al cerrar la app por completo y volver a abrirla, entra directo al Menú con la sesión recordada (P14_reabrir_app_entra_directo_al_menu.png) |

## 3. Eficiencia

| Aspecto | Cómo se logra | Verificación |
|---|---|---|
| Sin consultas repetidas | Los listeners en tiempo real reciben solo los cambios (no se recarga todo cada cierto tiempo) | Revisión de código: `addValueEventListener` en `leerEstado()` y `leerHistorial()` |
| No bloquea la pantalla | Firebase trabaja en su propio hilo y entrega el resultado en el hilo principal | La app responde mientras envía (P8) |
| Ahorro de batería y datos | Los listeners se quitan en `onStop()` (ciclo de vida) | P6: al volver, se vuelven a registrar en `onStart()` |
| Escrituras mínimas | `updateChildren()` envía solo los campos que cambian | Revisión de código: `updateChildren()` en `guardarComando()` y `cambiarSensorPuerta()` |
| Historial liviano | Solo se descargan los últimos 20 eventos (`limitToLast`) con índice por fecha (`.indexOn`) | Revisión de código y reglas |

## 4. Pruebas unitarias automáticas

`app/src/test/java/com/benjamin/cajafuerteiot/SeguridadTest.kt` (en Android Studio: clic derecho → **Run 'SeguridadTest'**)

Ejecutadas con `.\gradlew.bat :app:testDebugUnitTest` → **BUILD SUCCESSFUL** (evidencia: UNIT_tests_gradle_build_successful.png).

| Prueba | Qué verifica | Resultado |
|---|---|---|
| `pinValido_aceptaEntre4y6Digitos` | Acepta PIN válidos | ✅ |
| `pinValido_rechazaFormatosIncorrectos` | Rechaza PIN cortos, largos, con letras o vacíos | ✅ |
| `hashPin_esDeterministaYNoGuardaElPinEnTextoPlano` | Hash SHA-256 de 64 caracteres que no contiene el PIN | ✅ |
| `hashPin_mismoPinDistintoUsuario_daHashDistinto` | La sal por usuario funciona | ✅ |
| `alarma_seActivaAlTercerIntento` | La alarma se activa en el tercer intento | ✅ |
| `aperturaSospechosa_detectaForzamiento` | Detecta aperturas sospechosas | ✅ |
| `credenciales_validacionDeCorreoYContrasena` | Valida el correo y la contraseña | ✅ |

## 5. Errores encontrados y corregidos

| # | Prueba | Error observado | Causa | Corrección | Re-prueba |
|---|---|---|---|---|---|
| E1 | P7 (rotación) | Al girar la pantalla del Monitoreo con la puerta abierta, se registraba un nuevo "Sensor: puerta abierta" y se activaba una **falsa alarma de forzamiento**, aunque la caja estaba desbloqueada (ver `P7_rotacion_ERROR_detectado.png` y los eventos de las 23:21:20, 23:21:21 y 23:21:28 en `P9_P10_historial_alarma_y_desactivacion.png`) | Al recrear la pantalla, Android restaura el estado del switch y eso disparaba `setOnCheckedChangeListener` antes de recibir los datos de Firebase. En ese momento `estadoActual` tenía valores por defecto (`bloqueada = true`) | Se cambió a `setOnClickListener`, que solo reacciona cuando la persona toca el switch, y se bloquea el sensor hasta recibir el primer estado real (`estadoRecibido`) — `MonitoreoActivity.kt` | ✅ Después de girar varias veces con la puerta abierta, no aparecen eventos nuevos de sensor ni forzamiento después de las 23:31:13 |

## 6. Conclusión

Se ejecutaron **14 escenarios** entre un celular físico (Motorola moto g5, Android 14) y un emulador (API 37), más **7 pruebas unitarias automáticas**.

- **Interconexión (P1–P4):** los comandos del Activador llegaron al Monitoreo y el sensor de puerta del Monitoreo llegó al Activador, en ambos sentidos, con historial guardado y consultable.
- **Estabilidad (P5–P14):** sin internet, el comando quedó en cola y se entregó solo al reconectar. La app recuperó el estado tras estar en segundo plano, cerrarse y reabrirse. 6 comandos enviados en 11 segundos llegaron en orden, con el estado final correcto. Las protecciones (PIN, alarma por intentos, forzamiento, sesión y credenciales) respondieron según lo esperado.
- **Error encontrado y corregido (E1):** al rotar la pantalla se generaban falsas alarmas de forzamiento. Se identificó la causa (restauración automática del switch), se corrigió el código y la re-prueba fue exitosa.
- **Resultado final: 14/14 escenarios ✅ y 7/7 pruebas unitarias ✅**, confirmados además con la versión final del código entre **2 celulares físicos** (sección 7).

## 7. Re-prueba con la versión final (código reorganizado + 2 celulares físicos)

El 20/10 el código se reorganizó para seguir la estructura vista en clases: cada pantalla con su propio código y Firebase usado directamente, como en la Sesión 9. **Las pantallas y la lógica no cambiaron.** Se repiten las pruebas principales con **dos celulares físicos** conectados por Wi-Fi a través de Firebase.

| Dispositivo | Detalle |
|---|---|
| Celular 1 (Activador) | Motorola moto g5 · Android 14 |
| Celular 2 (Monitoreo) | HONOR ALT-NX3 · Android 15 (API 35) |

| # | Prueba repetida | Resultado |
|---|---|---|
| R1 | P1: login con la misma cuenta en ambos celulares | ✅ Ambos en el Menú con bejota@gmail.com (R1_menu_motorola.png · R1_menu_honor.png) |
| R2 | P2: comando Activador → Monitoreo | ✅ "Bloqueada · modo Normal · sensibilidad 100" enviado desde el Motorola ("Comando enviado ✅") y recibido en el HONOR (21/10 12:16:42) (R2_comando_activador_motorola.png · R2_comando_recibido_monitoreo_honor.png) |
| R3 | P3: sensor de puerta Monitoreo → Activador | ✅ La puerta abierta en el HONOR (12:18:04) aparece en el Activador del Motorola: "Puerta: Abierta" (R3_sensor_y_alarma_recibidos_en_activador_motorola.png) |
| R4 | P5: modo avión en un celular, comando en cola, reconexión | ✅ En modo avión, el Motorola mostró 🔴 y "Sin conexión: el comando se enviará al reconectar" (12:22). Al volver la conexión, el HONOR recibió solo "Bloqueada · modo Normal · sensibilidad 45" (12:23:07) (R4_1_comando_sin_conexion_motorola.png · R4_2_honor_sin_conexion.png · R4_3_comando_llega_al_reconectar_honor.png) |
| R5 | P9: 3 PIN incorrectos → alarma en ambos | ✅ Intentos 1, 2 y 3 (12:19:17 a 12:19:32): "PIN incorrecto: ALARMA ACTIVADA" en ambos; se desactivó con el PIN correcto (12:20:04) (R5_pin_incorrecto_alarma_motorola.png · R5_pin_incorrecto_alarma_honor.png · R5_alarma_desactivada_pin_correcto_honor.png) |
| R6 | P11: apertura con caja bloqueada → alarma | ✅ Caja bloqueada en modo Alarma armada: al abrir la puerta en el HONOR saltó "Posible forzamiento" y la alarma se vio en ambos celulares (R6_forzamiento_alarma_honor.png · R3_sensor_y_alarma_recibidos_en_activador_motorola.png) |
| R7 | Pruebas unitarias (`.\gradlew.bat :app:testDebugUnitTest`) | ✅ BUILD SUCCESSFUL con la versión final (R7_tests_build_successful_version_final.png) |

**Resultado de la re-prueba: 7/7 ✅.** La versión final funciona igual que la anterior, ahora entre dos celulares físicos (Motorola moto g5 con Android 14 y HONOR ALT-NX3 con Android 15).
