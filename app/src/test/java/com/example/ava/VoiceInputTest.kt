package com.example.ava

import com.example.ava.esphome.microphone.Microphone
import com.example.ava.esphome.voiceassistant.AudioResult
import com.example.ava.esphome.voiceassistant.VoiceInputImpl
import com.example.ava.esphome.wakeword.WakeWord
import com.example.ava.settings.SettingState
import com.example.ava.stubs.StubMicrophone
import com.example.ava.stubs.StubWakeWord
import com.example.ava.stubs.stubSettingState
import com.example.ava.wakewords.models.WakeWordWithId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoiceInputTest {
    fun createVoiceInput(
        microphone: Microphone = StubMicrophone(),
        wakeWord: WakeWord = StubWakeWord(),
        availableWakeWords: List<WakeWordWithId> = emptyList(),
        availableStopWords: List<WakeWordWithId> = emptyList(),
        activeWakeWords: List<String> = availableWakeWords.map { it.id },
        activeStopWords: List<String> = availableStopWords.map { it.id },
        muted: SettingState<Boolean> = stubSettingState(false),
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) = VoiceInputImpl(
        microphone = flowOf(microphone),
        wakeWord = wakeWord,
        availableWakeWords = { availableWakeWords },
        availableStopWords = { availableStopWords },
        activeWakeWords = stubSettingState(activeWakeWords),
        activeStopWords = stubSettingState(activeStopWords),
        muted = muted,
        dispatcher = dispatcher
    )

    @Test
    fun should_not_open_microphone_when_not_streaming() = runTest {
        var microphoneStarted = false
        val voiceInput = createVoiceInput(
            microphone = object : StubMicrophone() {
                override fun start() {
                    microphoneStarted = true
                }
            }
        )

        val job = voiceInput.start().launchIn(this)
        withContext(Dispatchers.Default) { delay(200) }
        job.cancelAndJoin()

        assertFalse(microphoneStarted)
    }

    @Test
    fun should_emit_audio_when_streaming() = runTest {
        val voiceInput = createVoiceInput()
        voiceInput.isStreaming = true

        val result = voiceInput.start().first()

        assertTrue(result is AudioResult.Audio)
    }

    @Test
    fun should_close_microphone_when_streaming_stops() = runTest {
        val started = CompletableDeferred<Unit>()
        val microphoneStopped = CompletableDeferred<Unit>()
        val voiceInput = createVoiceInput(
            microphone = object : StubMicrophone() {
                override fun start() {
                    started.complete(Unit)
                }

                override fun stop() {
                    microphoneStopped.complete(Unit)
                }
            }
        )

        voiceInput.start().launchIn(backgroundScope)
        voiceInput.isStreaming = true
        started.await()
        voiceInput.isStreaming = false

        microphoneStopped.await()
    }

    @Test
    fun should_close_microphone_when_muted() = runTest {
        val started = CompletableDeferred<Unit>()
        val microphoneStopped = CompletableDeferred<Unit>()
        val voiceInput = createVoiceInput(
            microphone = object : StubMicrophone() {
                override fun start() {
                    started.complete(Unit)
                }

                override fun stop() {
                    microphoneStopped.complete(Unit)
                }
            }
        )

        voiceInput.start().launchIn(backgroundScope)
        voiceInput.isStreaming = true
        started.await()
        voiceInput.muted.set(true)

        microphoneStopped.await()
    }

    @Test
    fun should_close_microphone_when_flow_cancelled() = runTest {
        val started = CompletableDeferred<Unit>()
        var microphoneStopped = false
        val voiceInput = createVoiceInput(
            microphone = object : StubMicrophone() {
                override fun start() {
                    started.complete(Unit)
                }

                override fun stop() {
                    microphoneStopped = true
                }
            }
        )
        voiceInput.isStreaming = true

        val job = voiceInput.start().launchIn(this)
        started.await()
        job.cancelAndJoin()

        assertTrue(microphoneStopped)
    }
}
