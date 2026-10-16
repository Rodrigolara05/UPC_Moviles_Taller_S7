package com.example.myupcchat

import com.example.myupcchat.data.ChatException
import com.example.myupcchat.data.GeminiChatRepository
import com.example.myupcchat.model.Author
import com.example.myupcchat.model.ChatMessage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GeminiChatRepositoryTest {
    private lateinit var server: MockWebServer
    @Before fun setUp() { server = MockWebServer(); server.start() }
    @After fun tearDown() { server.shutdown() }
    private fun repository(key: String = "test-key", timeoutMillis: Long = 2000) = GeminiChatRepository(
        apiKey = key, model = "test-model", systemPrompt = "Responde en español",
        baseUrl = server.url("/"),
        client = OkHttpClient.Builder().callTimeout(timeoutMillis, TimeUnit.MILLISECONDS).build(),
    )
    private val history = listOf(ChatMessage(Author.USER, "Hola \"Gemini\"\n¿Cómo estás?"),
        ChatMessage(Author.ASSISTANT, "Bien"), ChatMessage(Author.USER, "Continúa"))

    @Test fun `request contains key header prompt and ordered history and joins text parts`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"candidates":[{"content":{"parts":[{"text":"privado","thought":true},{"text":"Hola "},{"text":"mundo"}]},"finishReason":"STOP"}]}"""))
        assertEquals("Hola mundo", repository().generateReply(history))
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("POST", request.method)
        assertEquals("/v1beta/models/test-model:generateContent", request.path)
        assertEquals("test-key", request.getHeader("x-goog-api-key"))
        assertFalse(request.path!!.contains("test-key"))
        val json = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("Responde en español", json["systemInstruction"]!!.jsonObject["parts"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
        val contents = json["contents"]!!.jsonArray
        assertEquals(listOf("user", "model", "user"), contents.map { it.jsonObject["role"]!!.jsonPrimitive.content })
        assertEquals(history[0].text, contents[0].jsonObject["parts"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
    }

    @Test fun `missing key does not perform network request`() = runBlocking {
        assertTrue(failure(repository("" )).contains("GEMINI_API_KEY"))
        assertEquals(0, server.requestCount)
    }

    @Test fun `http errors become safe actionable messages`() = runBlocking {
        for ((code, expected) in listOf(400 to "API key", 401 to "API key", 403 to "API key", 404 to "modelo", 429 to "cuota", 503 to "disponible")) {
            server.enqueue(MockResponse().setResponseCode(code).setBody("sensitive-server-body"))
            val error = failure(repository())
            assertTrue(error, error.contains(expected))
            assertFalse(error.contains("sensitive-server-body"))
        }
    }

    @Test fun `blocked empty and malformed responses are handled`() = runBlocking {
        for ((body, expected) in listOf(
            """{"promptFeedback":{"blockReason":"SAFETY"}}""" to "reformularla",
            """{"candidates":[{"finishReason":"SAFETY"}]}""" to "reformularla",
            """{"candidates":[]}""" to "vacía",
            "not-json" to "interpretar",
        )) {
            server.enqueue(MockResponse().setBody(body))
            assertTrue(failure(repository()).contains(expected))
        }
    }

    @Test fun `timeout has a useful error`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertTrue(failure(repository(timeoutMillis = 150)).contains("tardó demasiado"))
    }

    @Test fun `cancelling a coroutine cancels its http call`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val client = OkHttpClient()
        val repo = GeminiChatRepository("test", "test", client = client, baseUrl = server.url("/"))
        val job = launch(Dispatchers.Default) { repo.generateReply(history) }
        withContext(Dispatchers.IO) { assertNotNull(server.takeRequest(2, TimeUnit.SECONDS)) }
        val call = client.dispatcher.runningCalls().single()
        job.cancelAndJoin()
        assertTrue(call.isCanceled())
    }

    private suspend fun failure(repo: GeminiChatRepository): String = try {
        repo.generateReply(history)
        error("Expected ChatException")
    } catch (e: ChatException) { e.message.orEmpty() }
}
