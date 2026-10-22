# Trazzo Camera — cómo probarlo

Requisitos: Windows 10/11, webcam, Python 3.12 o superior (https://www.python.org/downloads/,
marcar "Add python.exe to PATH" al instalar).

## 1. Instalar (una sola vez)

Abre PowerShell dentro de la carpeta `camera` y ejecuta:

    python -m venv .venv
    .\.venv\Scripts\python -m pip install -r requirements-dev.txt

Los modelos ya vienen en el ZIP (`models/`): no hay que descargar nada.
Si faltara alguno: `.\.venv\Scripts\python scripts\download_models.py`.

## 2. Verificar que todo funciona

    .\.venv\Scripts\python -m pytest -q

Debe terminar en `149 passed`.

## 3. Ventana de prueba

    .\.venv\Scripts\python test_camera.py

- **REGISTRAR (E)**: escribe un ID (ej. tu nombre), Enter, y sigue las instrucciones en
  pantalla (mira al frente, gira la cabeza, vuelve al frente).
- **VERIFICAR (V)**: escribe el ID y confirma que eres esa persona.
- **IDENTIFICAR (I)**: sin escribir nada, dice quién está frente a la cámara.
- **ELIMINAR (D)**: borra un ID. **CERRAR (Q)**: sale.

Arriba a la izquierda se ve el veredicto anti-spoof: **REAL**, **POSIBLE FALSO** o **POCA LUZ**.
Ponte con luz de frente (lámpara o ventana delante de ti, no detrás).

Qué probar:
1. Registrar 2 o más personas e identificar a cada una: no debe confundirlas.
2. Identificar a alguien NO registrado: debe decir "no se encontro coincidencia".
3. Mostrar una foto en el celular en vez de la cara: debe rechazarla.
4. Medir el punto 3 con detalle: `.\.venv\Scripts\python scripts\evaluate_anti_spoof.py`
   (teclas R = persona real, 1 = foto en celular, 2 = foto impresa, 3 = video, Q = reporte).

## 4. Servicio (lo que usará la web)

    .\.venv\Scripts\python src\server.py

Abre un WebSocket en `ws://localhost:9002`. Los datos se guardan cifrados en
`C:\ProgramData\TrazzoAgent\faces.db`.

Si la cámara no abre, cambia el índice: `$env:CAMERA_INDEX = "0"` antes de ejecutar.
Detalles técnicos y mediciones: `ANALISIS_CAMARA.md`.
