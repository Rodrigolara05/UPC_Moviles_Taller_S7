package com.example.myupcchat.data

import com.example.myupcchat.model.Author
import com.example.myupcchat.model.ChatMessage
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class GeminiChatRepository(
    private val apiKey: String,
    private val model: String,
    private val systemPrompt: String = AssistantConfig.SYSTEM_PROMPT,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS).build(),
    private val baseUrl: HttpUrl = "https://generativelanguage.googleapis.com/".toHttpUrl(),
) : ChatRepository {
    override suspend fun generateReply(history: List<ChatMessage>): String {
        if (apiKey.isBlank()) throw ChatException(
            "Añade GEMINI_API_KEY en local.properties, sincroniza y vuelve a ejecutar la app."
        )
        val payload = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray { add(textPart(systemPrompt.trimIndent())) })
            })
            put("contents", buildJsonArray {
                history.forEach { message ->
                    add(buildJsonObject {
                        put("role", if (message.author == Author.USER) "user" else "model")
                        put("parts", buildJsonArray { add(textPart(message.text)) })
                    })
                }
            })
        }
        val url = baseUrl.newBuilder().addPathSegment("v1beta").addPathSegment("models")
            .addPathSegment("$model:generateContent").build()
        val request = Request.Builder().url(url).header("x-goog-api-key", apiKey)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        // OkHttp trabaja fuera del hilo principal. La cancelación del ViewModel cancela HTTP.
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val message = if (e is InterruptedIOException) {
                        "Gemini tardó demasiado. Vuelve a intentarlo."
                    } else "No se pudo conectar. Revisa tu conexión a Internet."
                    continuation.resumeWithException(ChatException(message))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use {
                            if (!it.isSuccessful) throw ChatException(httpError(it.code))
                            parseReply(it.body?.string().orEmpty())
                        }
                    }
                    result.fold(
                        onSuccess = { continuation.resume(it) },
                        onFailure = {
                            continuation.resumeWithException(when (it) {
                                is ChatException -> it
                                is InterruptedIOException -> ChatException("Gemini tardó demasiado. Vuelve a intentarlo.")
                                is IOException -> ChatException("Se interrumpió la conexión. Vuelve a intentarlo.")
                                else -> ChatException("No se pudo interpretar la respuesta de Gemini.")
                            })
                        },
                    )
                }
            })
        }
    }

    private fun textPart(text: String) = buildJsonObject { put("text", text) }

    private fun parseReply(body: String): String {
        val root = Json.parseToJsonElement(body).jsonObject
        val blocked = root["promptFeedback"]?.jsonObject?.get("blockReason")?.jsonPrimitive?.content
        if (blocked != null && blocked != "BLOCK_REASON_UNSPECIFIED") {
            throw ChatException("Gemini no puede responder a esta solicitud. Prueba a reformularla.")
        }
        val candidate = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
        val finish = candidate?.get("finishReason")?.jsonPrimitive?.content
        if (finish in setOf("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY")) {
            throw ChatException("Gemini no puede responder a esta solicitud. Prueba a reformularla.")
        }
        val text = candidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?.map { it.jsonObject }
            ?.filter { it["thought"]?.jsonPrimitive?.booleanOrNull != true }
            ?.mapNotNull { it["text"]?.jsonPrimitive?.contentOrNull }
            ?.joinToString("")?.trim().orEmpty()
        if (text.isBlank()) throw ChatException("Gemini devolvió una respuesta vacía. Inténtalo de nuevo.")
        return if (finish == "MAX_TOKENS") "$text\n\n[Respuesta recortada por el límite del modelo.]" else text
    }

    private fun httpError(code: Int) = when (code) {
        400 -> "Revisa la API key, el modelo y la configuración de la solicitud."
        401, 403 -> "La API key no es válida o no tiene acceso. Revisa Google AI Studio."
        404 -> "El modelo no está disponible. Revisa GEMINI_MODEL en local.properties."
        429 -> "Se alcanzó la cuota de Gemini. Espera un poco o revisa tu cuota en AI Studio."
        in 500..599 -> "Gemini no está disponible ahora. Inténtalo más tarde."
        else -> "No se pudo completar la solicitud (HTTP $code)."
    }
}
