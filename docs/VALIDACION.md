# Validación del taller

## Automatizada

Ejecutar los comandos del README para compilar debug, pruebas JVM, lint y APK de pruebas de interfaz. Las pruebas HTTP usan un servidor local, sin llamar a Gemini.

### Resultado de esta preparación · 16 de octubre de 2026

- `assembleDebug`: correcto; APK generado sin clave.
- `testDebugUnitTest`: 11 pruebas, 0 fallos (incluye la prueba de ejemplo de la base).
- `lintDebug`: 0 errores y 20 avisos, relativos a versiones disponibles y elementos heredados de la plantilla.
- `connectedDebugAndroidTest`: 6 pruebas, 0 fallos en `Medium_Phone_API_37.0`, Android 17 (incluye la prueba de contexto de la base).
- Se verificaron la recreación de Activity conservando el borrador y la visibilidad del editor y «Enviar» con teclado software abierto.
- Se revisaron visualmente las capturas de bienvenida clara y conversación larga oscura. Las conversaciones de pruebas son ficticias.
- Se actualizaron Espresso a 3.7.0 y AndroidX Test JUnit a 1.3.0 porque las versiones originales fallaban al acceder al sistema de entrada de Android 17.

Capturas: [bienvenida clara](images/welcome-light.png) y [mensaje largo en tema oscuro](images/conversation-dark.png). La captura de conversación está desplazada hacia la última respuesta, de modo que el mensaje anterior queda parcialmente fuera de la lista visible.

No se ha probado Gemini en vivo: no hay API key configurada. Sigue pendiente el ensayo con la cuenta del instructor y la revisión manual completa en el dispositivo que se utilizará en el taller.

## Revisión en dispositivo antes del taller

- [ ] Arranque sin clave: bienvenida visible, explicación de configuración y envío deshabilitado.
- [ ] Tema claro y oscuro: contraste, burbujas, editor y avisos legibles.
- [ ] Teclado abierto en vertical y horizontal: editor accesible y mensajes desplazables.
- [ ] Mensaje largo: ajuste de líneas, selección de texto y desplazamiento correcto.
- [ ] Rotación con conversación y durante carga: conserva estado, sin duplicar peticiones.
- [ ] «Nuevo chat» durante carga: lista vacía y ninguna respuesta antigua posterior.
- [ ] Error de red: pregunta recuperada, reintento sin duplicación.
- [ ] Fuente grande: cabecera, botón y campo utilizables.
- [ ] Pruebas instrumentadas: `connectedDebugAndroidTest` con dispositivo conectado.

## Comprobación real con tu cuenta

- [ ] Clave válida y modelo disponible en AI Studio.
- [ ] Primer mensaje produce una respuesta real.
- [ ] Segundo mensaje demuestra contexto («Me llamo Alex» → «¿Cómo me llamo?»).
- [ ] Tras cambiar el prompt, recompilar e iniciar chat nuevo, se observa el nuevo estilo.

Estas comprobaciones requieren dispositivo y credenciales. No deben darse por superadas por el hecho de pasar las pruebas simuladas.
