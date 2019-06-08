package me.liranai.adjutant.model

import com.darkprograms.speech.microphone.Microphone
import edu.cmu.sphinx.api.Configuration
import net.sourceforge.javaflacencoder.FLACFileWriter

class AdjutantRecognition() {
    val mic = Microphone(FLACFileWriter.FLAC)
    val configuration = Configuration()

    init {
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us")
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict")
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")


    }

}