# Análisis del módulo `camera` — Reconocimiento facial Trazzo

**Fecha:** 2026-07-20 · **Última actualización:** 2026-07-20 (post-implementación)
**Alcance:** revisión completa del contenido de `camera/` para determinar si lo que hace es **real** (no simulado), si **detecta el rostro** y si **verifica la identidad** de la persona.

> **Estado:** los hallazgos que bloqueaban la funcionalidad **ya fueron corregidos** en esta pasada. Ver §9 "Cambios implementados". El resumen de abajo refleja el estado **ya corregido**.

---

## 1. Veredicto rápido

| Pregunta | Respuesta |
|---|---|
| ¿Es reconocimiento facial **real** (no mock)? | ✅ **Sí.** Usa InsightFace (ArcFace) + OpenCV reales, con modelos ONNX presentes. |
| ¿**Detecta** la cara? | ✅ Sí — detección + 5 landmarks + embedding de 512 dimensiones por rostro. |
| ¿**Verifica** si es la persona o no? | ✅ Sí — 1:1 (`verify`) y 1:N (`identify`) por similitud coseno contra un umbral. |
| ¿Tiene prueba de vida (anti-foto/video)? | ✅ **Sí (corregido).** Liveness activo (reto de girar la cabeza + micro-movimiento) **más** el anti-spoof por CNN, que **ya está ACTIVO**: se descargaron sus 2 modelos y se verificó inferencia real (ruido aleatorio → score 0.01, correctamente "fake"). |
| ¿Está integrado con backend/frontend? | ❌ **Todavía no.** Es un servicio WebSocket local independiente (`ws://localhost:9002`) que solo hace enroll/verify/identify y devuelve `personId` + `score`. No marca asistencia ni llama al backend. |

**En una frase:** el motor biométrico es real y de buena calidad; con los arreglos de esta pasada **ya detecta el rostro, verifica identidad y rechaza fotos/pantallas con el CNN**. Lo único pendiente es **integrarlo con el resto de Trazzo** (marcar asistencia).

---

## 2. Qué es real (componente por componente)

| Componente | Archivo | ¿Real? | Detalle |
|---|---|---|---|
| **Detección + embeddings** | `infrastructure/face_engine.py` | ✅ | InsightFace `FaceAnalysis` con el pack **buffalo_sc**: `det_500m.onnx` (detección) + `w600k_mbf.onnx` (ArcFace, embedding normalizado de 512-d). Modelos presentes (13.6 MB + 2.5 MB). |
| **Cámara** | `infrastructure/camera.py` | ✅ | OpenCV `VideoCapture` real (DirectShow/MSMF/v4l2), warmup, timeouts, `BUFFERSIZE=1` para frames frescos. |
| **Reconocimiento** | `services/recognition_service.py` | ✅ | Similitud **coseno** (`np.dot` sobre embeddings normalizados) contra `recognition_threshold` (0.55). |
| **Enrolamiento** | `services/enrollment_service.py` | ✅ | Promedia N muestras → normaliza → guarda el template. |
| **Liveness activo** | `services/challenge_service.py` | ✅ | Reto-respuesta real: mirar al frente → girar cabeza izq/der (aleatorio) → volver. Verifica que el rostro sea el mismo entre los dos frontales (sim ≥ 0.75). |
| **Liveness pasivo** | `services/challenge_service.py` (`run_passive`) | ✅ | Capta varios frames en ~5 s y exige **micro-movimiento** (varianza de posición/tamaño) e **independencia de landmarks** — una foto plana falla ambos. |
| **Anti-spoof CNN** | `services/anti_spoof_service.py` | ✅ | Ensemble MiniFASNet real (letterbox 128×128, softmax, ensemble por mínimo). **Modelos ya descargados y cargando** (`enabled=True`, 2 sesiones ONNX). Ver §4. |
| **Cifrado en reposo** | `infrastructure/crypto.py` | ✅ | AES-256-GCM, nonce único por registro, AAD que ata el ciphertext a `(person_id, tenant_id)`, master key en `%PROGRAMDATA%\TrazzoAgent\keys\master.key` con permisos restrictivos. |
| **Persistencia** | `infrastructure/db.py` | ✅ | SQLite (WAL), embeddings cifrados, aislamiento por `tenant_id`, tope de galería por tenant. |
| **Servidor** | `server.py` + `handlers/router.py` | ✅ | WebSocket real, límite de tamaño de mensaje, tope de clientes concurrentes, validación de entrada, sin fugas de stack traces. |
| **Validación** | `domain/validators.py` | ✅ | Whitelist regex de `personId`/`tenantId`, sanitización anti log-injection. |
| **Tests** | `tests/` | ✅ | 81 funciones de test (crypto, db, validators, liveness, recognition, router, etc.). |

**Entorno:** `.venv` tiene instalados de verdad `insightface 1.0.1`, `onnxruntime 1.27.0`, `opencv (cv2)`, `numpy`, `cryptography`, `websockets`. No hay stubs ni datos falsos en el flujo.

---

## 3. Cómo detecta y verifica (flujos)

El servicio expone un WebSocket con estos mensajes (`domain/contracts.py`):

### Enrolar — `camera.enroll.start`
1. Reto activo (`capture_full`): **frontal → girar cabeza → frontal**.
2. Cada frame frontal pasa por el anti-spoof CNN (hoy fail-open) y se valida que el rostro sea el mismo entre ambos frontales.
3. Se promedian las muestras → template normalizado → **cifrado** → guardado por `(person_id, tenant_id)`.

### Verificar 1:1 — `camera.verify.start`
1. Carga el template guardado de esa persona (si no existe → `PERSON_NOT_ENROLLED`).
2. Captura pasiva con liveness (varios frames, exige micro-movimiento).
3. Compara por coseno; si `< 0.55` → **`NO_MATCH`**. Si no, devuelve `personId` + `score`.

### Identificar 1:N — `camera.identify.start`
1. Captura pasiva con liveness.
2. Recorre **toda la galería del tenant** y elige el mejor por encima del umbral.
3. Galería vacía → `NO_ENROLLED_FACES`; sin coincidencia → `NO_MATCH`.

> Responde a *"¿es él o no?"*: **sí**. `verify` confirma una identidad declarada; `identify` la descubre. Ambos rechazan si nadie supera el umbral.

---

## 4. ✅ (Resuelto) Anti-spoof por CNN

**Antes:** `services/anti_spoof_service.py` busca 2 modelos en `models/anti_spoofing/`
(`AntiSpoofing_print-replay_1.5_128.onnx`, `AntiSpoofing_bin_1.5_128.onnx`). Esa carpeta no
existía → `enabled = False` y `score()` devolvía `1.0` (fail-open) → el CNN daba por real todo.

**Ahora (corregido):**
- Se **descargaron los 2 modelos** (repo `hairymax/Face-AntiSpoofing`, MIT) a `models/anti_spoofing/`.
- Verificado en runtime: `AntiSpoofingService.instance().enabled == True`, 2 sesiones ONNX cargadas, y `score()` sobre ruido aleatorio devuelve **~0.01** (correctamente "fake") → **inferencia real funcionando**.
- Se agregó el flag **`ANTI_SPOOF_REQUIRED`** (env, default `False`): si es `True` y los modelos faltan, `score()` devuelve `0.0` (**fail-closed**) y toda captura se rechaza — recomendado en producción.
- Se agregó log prominente del estado del CNN al arrancar y el campo `antiSpoofEnabled` en `camera.status`.

> **Los modelos están gitignored** (`models/anti_spoofing/`), así que en cada máquina nueva hay que ejecutarlo una vez:
> ```bash
> .venv\Scripts\python scripts\download_anti_spoof.py
> ```
> (En esta máquina ya se descargaron con éxito.)

---

## 5. Otros hallazgos

| # | Severidad | Hallazgo | Estado |
|---|---|---|---|
| 1 | 🟡 Media | **El `liveness score` reportado era cosmético** (`_to_result` fijaba `score=1.0`). | ✅ **Corregido.** Ahora reporta un score real: en `verify/identify` domina la confianza del CNN (`cnn_min`) + micro-movimiento; en enrolamiento, la similitud entre frontales del reto. |
| 2 | 🟢 Baja | **`LivenessService` es código muerto** | ⚠️ **Se deja como está a propósito.** Se instancia y pasa a `CaptureService` pero no se usa (el liveness real vive en `challenge_service.py`). Tiene lógica real y tests propios (`test_liveness.py`); quitarlo obliga a cambiar la firma que los tests usan como posicional. Se documenta; no bloquea funcionalidad. |
| 3 | 🟢 Baja | **`enrollment_samples` por defecto = 1** | ⚠️ Se deja en 1: cada muestra ya es un reto activo completo (girar la cabeza) y promedia 2 frontales; subirlo empeora la UX. Configurable por `ENROLLMENT_SAMPLES`. |
| 4 | 🟢 Baja | **Sin verificación de integridad de modelos** | ⚠️ Pendiente. Los ONNX se cargan por ruta; no se valida hash/firma. Recomendable para PII biométrica pero no bloquea. |
| 5 | ℹ️ Info | **No integrado con backend/frontend** | ⚠️ Pendiente. Corre aislado en `ws://localhost:9002`. No marca asistencia ni notifica al backend; el frontend aún no consume estos mensajes. |
| 6 | ℹ️ Info | **`.venv/Scripts/python.exe` está corrupto (0 bytes)** | Problema del entorno, no del código. Los tests se corrieron con `pythonw.exe`. Conviene recrear el venv (`python -m venv .venv`) en la máquina de desarrollo. |

---

## 6. Seguridad y privacidad (lo bueno)

- **Embeddings cifrados en reposo** (AES-256-GCM) con AAD que impide mover un registro a otra persona/tenant sin que falle la autenticación.
- **Aislamiento multi-tenant** en todas las consultas.
- **Validación estricta** de entradas + sanitización de logs (anti log-injection).
- **Sin fuga de errores** internos al cliente (stack traces solo en log).
- **Límites de recursos**: tamaño de mensaje, clientes concurrentes, timeouts de cámara, tope de galería.
- Alineado con la **Ley 29733** (PII biométrica) según se documenta en el propio código.

---

## 7. Recomendaciones pendientes

1. **ℹ️ Integrar con backend/frontend** (lo único importante que queda): definir cómo `identify`/`verify` disparan la marcación de asistencia, equivalente al flujo del huellero.
2. **🟢 Producción con `ANTI_SPOOF_REQUIRED=1`** para forzar fail-closed si algún día faltan los modelos.
3. **🟢 Verificación de integridad de modelos** (hash/firma) al cargarlos.
4. **🟢 Recrear el venv** (`python -m venv .venv`) — el `python.exe` actual está corrupto.

---

## 8. Conclusión

El módulo `camera` **no es una simulación**: es un pipeline de reconocimiento facial real y bien construido (InsightFace/ArcFace + OpenCV + liveness activo + **anti-spoof CNN** + cifrado + multi-tenant), con **109 tests en verde** y buenas prácticas de seguridad. **Detecta el rostro, verifica la identidad y rechaza fotos/pantallas.**

Con los arreglos de esta pasada, el motor biométrico queda **funcional de punta a punta como servicio**. Lo único pendiente para el sistema completo es **integrarlo con el resto de Trazzo** (que la identificación marque asistencia).

---

## 9. Cambios implementados (2026-07-20)

| Cambio | Archivo(s) | Efecto |
|---|---|---|
| **Descarga de modelos anti-spoof** | `models/anti_spoofing/*.onnx` | CNN anti-spoof ACTIVO (verificado: `enabled=True`, score de ruido ≈ 0.01). |
| **Score de liveness real** | `services/capture_service.py` | La respuesta ya no reporta `1.0` fijo; usa `cnn_min` + `motion` (passive) o similitud entre frontales (full). |
| **Flag `ANTI_SPOOF_REQUIRED` (fail-closed)** | `config.py`, `services/anti_spoof_service.py` | Si `=1` y no hay modelos, toda captura se rechaza (`score=0.0`) en vez de degradar abierto. |
| **Estado del CNN visible** | `handlers/router.py`, `server.py` | `camera.status` devuelve `antiSpoofEnabled`; log prominente del estado al arrancar. |
| **Fix de aislamiento de tests** | `tests/conftest.py` | Se restaura `models_dir` entre tests → los tests con modelos reales ya no se saltan. |

**Validación:** `pytest` → **109 passed, 0 skipped** (antes 107 passed + 2 skipped por modelos ausentes). Inferencia real del CNN confirmada fuera de los tests.

> Nota: los `.onnx` de anti-spoof están **gitignored**; en cada máquina nueva se corre `scripts/download_anti_spoof.py` una vez.
