package me.liranai.adjutant.model

import com.darkprograms.speech.microphone.Microphone
import com.darkprograms.speech.recognizer.GSpeechDuplex
import com.darkprograms.speech.recognizer.GSpeechResponseListener
import edu.cmu.sphinx.api.Configuration
import edu.cmu.sphinx.api.StreamSpeechRecognizer
import net.sourceforge.javaflacencoder.FLACFileWriter
import java.io.ByteArrayInputStream
import kotlin.concurrent.thread

class AdjutantRecognition(GAPI: String) {
    val mic = Microphone(FLACFileWriter.FLAC)

    val recogniser = StreamSpeechRecognizer(Configuration().apply {
        setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us")
        setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict")
        setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")
    })

    val duplex = GSpeechDuplex(GAPI)

    fun addResponseListener(listener: GSpeechResponseListener) {
        duplex.addResponseListener(listener)
    }

    init {
        duplex.language = "en-GB"

        thread(start = true) {
            println("${Thread.currentThread()} has started.")
            try {
                duplex.recognize(mic.targetDataLine, mic.audioFormat)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun recognise(audio: ByteArray): String {
        recogniser.startRecognition(ByteArrayInputStream(audio))
        var hyp: String = ""
        while (true) {
            val result = recogniser.getResult() ?: break
            println("Hypothesis: ${result.getHypothesis()}")
            hyp = result.hypothesis
        }
        return hyp
    }

}