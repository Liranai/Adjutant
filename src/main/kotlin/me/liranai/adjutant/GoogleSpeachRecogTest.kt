package me.liranai.adjutant

import com.darkprograms.speech.microphone.Microphone
import com.darkprograms.speech.recognizer.GSpeechDuplex
import com.darkprograms.speech.recognizer.GSpeechResponseListener
import com.darkprograms.speech.recognizer.GoogleResponse
import net.sourceforge.javaflacencoder.FLACFileWriter
import java.io.IOException
import javax.swing.*

class TryGoogleSpeechRecognitionSimple : GSpeechResponseListener {

    override fun onResponse(paramGoogleResponse: GoogleResponse) {
        // TODO Auto-generated method stub

    }

    companion object {

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val mic = Microphone(FLACFileWriter.FLAC)
            // You have to make your own GOOGLE_API_KEY
            val duplex = GSpeechDuplex("AIzaSyDfL6x8jRB_4Gq2gX0TBcG-zmNutDLsX-8")

            duplex.language = "en"

            val frame = JFrame("Jarvis Speech API DEMO")
            frame.defaultCloseOperation = 3
            val response = JTextArea()
            response.isEditable = false
            response.wrapStyleWord = true
            response.lineWrap = true

            val record = JButton("Record")
            val stop = JButton("Stop")
            stop.isEnabled = false

            record.addActionListener {
                Thread {
                    try {
                        duplex.recognize(mic.targetDataLine, mic.audioFormat)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }


                }.start()
                record.isEnabled = false
                stop.isEnabled = true
            }
            stop.addActionListener {
                mic.close()
                duplex.stopSpeechRecognition()
                record.isEnabled = true
                stop.isEnabled = false
            }
            val infoText = JLabel(
                "<html><div style=\"text-align: center;\">Just hit record and watch your voice be translated into text.\n<br>Only English is supported by this demo, but the full API supports dozens of languages.<center></html>",

                0
            )
            frame.contentPane.add(infoText)
            infoText.alignmentX = 0.5f
            val scroll = JScrollPane(response)
            frame.contentPane.layout = BoxLayout(frame.contentPane, 1)
            frame.contentPane.add(scroll)
            val recordBar = JPanel()
            frame.contentPane.add(recordBar)
            recordBar.layout = BoxLayout(recordBar, 0)
            recordBar.add(record)
            recordBar.add(stop)
            frame.isVisible = true
            frame.pack()
            frame.setSize(500, 500)
            frame.setLocationRelativeTo(null)

            duplex.addResponseListener(object : GSpeechResponseListener {
                internal var old_text = ""

                override fun onResponse(gr: GoogleResponse) {
                    var output = ""
                    output = gr.response
                    if (gr.response == null) {
                        this.old_text = response.text
                        if (this.old_text.contains("(")) {
                            this.old_text = this.old_text.substring(0, this.old_text.indexOf('('))
                        }
                        println("Paragraph Line Added")
                        this.old_text = response.text + "\n"
                        this.old_text = this.old_text.replace(")", "").replace("( ", "")
                        response.text = this.old_text
                        return
                    }
                    if (output.contains("(")) {
                        output = output.substring(0, output.indexOf('('))
                    }
                    if (!gr.otherPossibleResponses.isEmpty()) {
                        output = output + " (" + gr.otherPossibleResponses[0] as String + ")"
                    }
                    println(output)
                    response.text = ""
                    response.append(this.old_text)
                    response.append(output)
                }
            })
        }
    }
}