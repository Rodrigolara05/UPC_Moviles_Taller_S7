# Guía del instructor · 90 minutos

## Objetivo y preparación

Al terminar, el participante podrá seguir una pregunta desde Compose hasta Gemini y explicar cómo la respuesta actualiza la pantalla mediante MVVM. Podrá cambiar la personalidad sin tocar la UI.

Antes de la sesión: abre el proyecto, sincroniza y compila; descarga las dependencias; prepara un emulador o teléfono; comprueba Internet, la clave y el modelo de tu cuenta. Prueba dos turnos reales y luego retira la clave del `local.properties` si quieres hacer la configuración en vivo. Recompila sin clave. Mantén la clave fuera de la pantalla proyectada al copiarla. Ten preparados los previews y las pruebas unitarias por si falla la red.

## 0–10 · Qué vamos a construir

Muestra la interfaz y la vista previa de una conversación. Aclara que los textos de Preview son muestras de diseño; las respuestas de la app conectada las genera Gemini.

Explica: un modelo generativo produce texto a partir de instrucciones y contexto; no es una lista de respuestas programadas. Puede equivocarse. Distingue el prompt del sistema (comportamiento) y el mensaje del usuario (pregunta). Las respuestas pueden variar aunque repitamos la pregunta. No hace falta explicar entrenamiento ni implementar un modelo local.

Pregunta al grupo: «Si escribo “hazlo más corto”, ¿qué información necesitará recibir Gemini?». Vuelve a esta pregunta al mostrar el historial.

## 10–20 · Android y Kotlin necesarios

Abre `MainActivity.kt`, `ChatMessage.kt` y `ChatUiState` en este orden. Explica únicamente los conceptos que aparecen: `val`, `data class`, `enum`, lista inmutable, `copy`, función lambda y función `suspend`.

La Activity es el punto de entrada y conecta dependencias. Una `data class` representa datos. `copy` crea un nuevo estado; no se cambia una lista por detrás de Compose. No introduzcas navegación, Hilt, bases de datos ni casos de uso adicionales.

## 20–40 · Compose y estado

Recorre `ChatScreen`: `Scaffold`, cabecera, bienvenida, `LazyColumn`, `MessageBubble` y editor. Abre los previews claro, oscuro y sin clave. Muestra `Modifier`, `padding`, `weight`, claves estables de la lista y el desplazamiento al llegar un mensaje.

Explica que un composable describe qué debe verse para un estado dado. Una recomposición no debe disparar una petición de red. `ChatScreen` recibe `state` y callbacks: puede probarse sin Gemini y sin ViewModel.

En el editor señala `onValueChange`, `canSend` y `KeyboardActions`. Sigue la pulsación de «Enviar» hasta el callback. Pregunta: «¿Quién decide si estamos esperando una respuesta?». Respuesta: el estado del ViewModel; la vista lo representa.

## 40–55 · MVVM, StateFlow y corrutinas

Usa el diagrama del README y recorre `sendMessage()`:

1. Rechazar mensajes vacíos, falta de configuración y doble envío.
2. Crear un mensaje y actualizar el estado a carga antes de iniciar la corrutina.
3. Invocar el contrato `ChatRepository` desde `viewModelScope`.
4. Añadir la respuesta y terminar la carga, o restaurar el borrador si falla.

`MutableStateFlow` es privado; la vista recibe su versión de solo lectura. `collectAsStateWithLifecycle` observa con el ciclo de vida. Una función suspendible permite esperar sin bloquear la UI; la red la ejecuta OkHttp de forma asíncrona.

Muestra `newChat()`: cancela el trabajo y cambia la versión de conversación, de modo que una respuesta antigua no aparezca en el chat nuevo. La rotación conserva el ViewModel; la muerte del proceso no conserva este historial.

## 55–70 · Gemini y repositorio

Abre `ChatRepository` y luego `GeminiChatRepository`. Explica el contrato antes de HTTP. Recorre la petición: endpoint del modelo, cabecera `x-goog-api-key`, `systemInstruction` y `contents` con roles `user` y `model`.

Ejemplo conceptual, sin credenciales:

```json
{
  "systemInstruction": {"parts": [{"text": "Responde en español"}]},
  "contents": [
    {"role": "user", "parts": [{"text": "Me llamo Alex"}]},
    {"role": "model", "parts": [{"text": "Hola, Alex"}]},
    {"role": "user", "parts": [{"text": "¿Cómo me llamo?"}]}
  ]
}
```

Cada petición envía el historial completo; más conversación implica más contexto enviado. Esta demo no implementa resumen ni almacenamiento. Explica la lectura de `candidates/content/parts`, la exclusión de partes de pensamiento y los errores de cuota, conexión y bloqueo. No leas cada línea del adaptador de callbacks: basta explicar cómo la cancelación llega a `Call.cancel()`.

Muestra una prueba de MockWebServer: podemos verificar el contrato HTTP sin gastar cuota ni necesitar la API key. El uso de la clave dentro del APK es una simplificación didáctica; `local.properties` no es un mecanismo de protección del APK.

## 70–80 · Configuración en vivo y solución

**Minutos 70–74:** copia las entradas de `local.properties.example` al `local.properties` existente, preservando `sdk.dir`. Introduce la clave de AI Studio sin comillas. Sincroniza y ejecuta. No pidas a los asistentes que compartan sus claves.

**Minutos 74–77:** envía «Explícame qué es una API». Muestra la respuesta del asistente general. Edita el único `TODO(TALLER)` de Kotlin en `AssistantConfig.kt`.

Solución propuesta:

```kotlin
const val SYSTEM_PROMPT = """
    Eres un profesor paciente que explica tecnología a principiantes.
    Responde en español con un máximo aproximado de 120 palabras.
    Usa una analogía cotidiana y termina con una pregunta de comprobación.
    Si no sabes algo, dilo. Usa texto sencillo, sin tablas.
"""
```

**Minutos 77–80:** recompila, pulsa «Nuevo chat» y repite la pregunta. Compara tono, analogía y pregunta final. El prompt orienta la generación; no garantiza un número exacto de palabras ni respuestas idénticas.

No queda código incompleto necesario para compilar: solo se configura la credencial y se personaliza una constante ya funcional.

## 80–90 · Comprobar y conversar

- Contexto: «Me llamo Alex y estudio informática» → «¿Qué estudio?».
- Generación: «Inventa tres ideas para una aplicación de estudiantes» → «Desarrolla la segunda».
- Cancelación: envía una pregunta y pulsa «Nuevo chat» durante la carga.
- Error: desactiva Internet, envía y observa que se recupera el borrador; activa Internet y reintenta.
- Rotación: gira el dispositivo con mensajes y durante una petición; no debe enviarse de nuevo.
- Cierre: pide al grupo que identifique qué archivo cambiaría para modificar colores, personalidad o proveedor.

Reserva los últimos minutos para preguntas; no agregues funcionalidades en vivo fuera del ejercicio.

## Si falla Internet o la cuota

Continúa con los previews y ejecuta `ChatViewModelTest` y `GeminiChatRepositoryTest` desde Android Studio. Muestra cómo el repositorio falso entrega una respuesta y cómo MockWebServer devuelve JSON. Di explícitamente que esas respuestas son simuladas para probar el flujo; la generación real requiere conexión y acceso a Gemini. Usa la muestra de JSON anterior para explicar el contexto. No hay un modo que finja respuestas de IA en la aplicación.

## Preguntas frecuentes

**¿Por qué MVVM?** Separa el dibujo de la pantalla, su estado y el acceso a datos; cada parte se puede probar de forma independiente.

**¿Por qué no Hilt?** La fábrica manual basta para una pantalla y permite ver las dependencias durante un taller corto.

**¿Por qué no streaming?** Mostrar respuestas completas reduce la complejidad de red y estado; el objetivo es entender el flujo entero en 90 minutos.

**¿Gemini recuerda al usuario?** En esta demo recibe los mensajes anteriores en cada solicitud. «Nuevo chat» borra el contexto local que enviamos; no es una petición de eliminación de datos a Google.

**¿Puedo publicar esta app tal cual?** Antes de publicar hay que cambiar el acceso directo con clave por una integración apropiada para distribución y revisar privacidad, cuotas y almacenamiento.
