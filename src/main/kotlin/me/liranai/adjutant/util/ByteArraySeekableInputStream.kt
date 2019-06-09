package me.liranai.adjutant.util

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider
import java.io.ByteArrayOutputStream
import java.io.InputStream


fun byteArrayInputStreamOf(inputStream: InputStream): ByteArraySeekableInputStream {
    val baos = ByteArrayOutputStream()
    inputStream.copyTo(baos)
    val byteArray = baos.toByteArray()
    return ByteArraySeekableInputStream(byteArray)
}

class ByteArraySeekableInputStream(private val input: ByteArray) : SeekableInputStream(input.size.toLong(), 0) {
    private var position = 0
    override fun getPosition() = position.toLong()

    override fun canSeekHard() = true

    override fun seekHard(position: Long) {
        this.position = position.toInt()
    }

    override fun getTrackInfoProviders(): MutableList<AudioTrackInfoProvider> = mutableListOf()

    override fun read(): Int {
        if (position >= input.size)
            return -1
        val result = input[position]
        position++
        return result.toInt()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (position >= input.size)
            return -1
        val start = Math.min(position + off, input.size)
        val end = Math.min(position + off + len, input.size)
        val trueLen = end - start
        input.copyInto(b, 0, start, end)
        position += trueLen
        return trueLen
    }

    override fun skip(n: Long): Long {
        val newPosition = Math.min(input.size, position + n.toInt())
        val amount = newPosition - position
        position += amount
        return amount.toLong()
    }

    override fun available(): Int {
        return Math.max(input.size - position, 0)
    }

    override fun markSupported() = false
}