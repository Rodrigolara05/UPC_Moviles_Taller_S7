package com.example.myupcchat

import com.example.myupcchat.data.ChatException
import com.example.myupcchat.data.ChatRepository
import com.example.myupcchat.model.Author
import com.example.myupcchat.model.ChatMessage
import com.example.myupcchat.ui.chat.ChatViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `empty message and missing key never call repository`() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ChatViewModel(repo)
        vm.updateDraft("  ")
        vm.sendMessage()
        val unconfigured = ChatViewModel(repo, false)
        unconfigured.updateDraft("Hola")
        unconfigured.sendMessage()
        runCurrent()
        assertTrue(repo.calls.isEmpty())
    }

    @Test fun `loading prevents duplicate sends and successful turns preserve context`() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ChatViewModel(repo)
        vm.updateDraft(" Hola ")
        vm.sendMessage()
        vm.sendMessage()
        assertTrue(vm.uiState.value.isLoading)
        assertEquals("", vm.uiState.value.draft)
        runCurrent()
        assertEquals(1, repo.calls.size)
        repo.answer.complete("¡Hola!")
        runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf(Author.USER, Author.ASSISTANT), vm.uiState.value.messages.map { it.author })
        repo.answer = CompletableDeferred()
        vm.updateDraft("¿Qué te dije?")
        vm.sendMessage()
        runCurrent()
        assertEquals(listOf("Hola", "¡Hola!", "¿Qué te dije?"), repo.calls.last().map { it.text })
        repo.answer.complete("Me saludaste.")
        runCurrent()
    }

    @Test fun `failure restores draft and retry does not duplicate the user message`() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ChatViewModel(repo)
        vm.updateDraft("Pregunta")
        vm.sendMessage()
        runCurrent()
        repo.answer.completeExceptionally(ChatException("Sin conexión"))
        runCurrent()
        assertEquals("Pregunta", vm.uiState.value.draft)
        assertEquals("Sin conexión", vm.uiState.value.error)
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
        repo.answer = CompletableDeferred()
        vm.sendMessage()
        runCurrent()
        repo.answer.complete("Respuesta")
        runCurrent()
        assertEquals(2, vm.uiState.value.messages.size)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `new chat cancels request and ignores its result`() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ChatViewModel(repo)
        vm.updateDraft("Pregunta antigua")
        vm.sendMessage()
        runCurrent()
        vm.newChat()
        runCurrent()
        assertTrue(repo.wasCancelled)
        repo.answer.complete("Respuesta antigua")
        runCurrent()
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("", vm.uiState.value.draft)
        assertNull(vm.uiState.value.error)
    }

    private class FakeRepository : ChatRepository {
        val calls = mutableListOf<List<ChatMessage>>()
        var answer = CompletableDeferred<String>()
        var wasCancelled = false
        override suspend fun generateReply(history: List<ChatMessage>): String {
            calls += history
            return try { answer.await() } catch (e: CancellationException) {
                wasCancelled = true
                throw e
            }
        }
    }
}
