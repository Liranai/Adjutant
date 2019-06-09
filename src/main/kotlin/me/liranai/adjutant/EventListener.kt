package me.liranai.adjutant

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.TextChannel
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.message.MessageCreateEvent
import java.time.Duration
import javax.swing.Timer

class EventListener(private val bot: AdjutantDiscordBot, private val prefix: String) {
    private var old_text: String = ""
    var timer = Timer(2000) {
        println("PING")
        println("TO SAY: $old_text")
    }

    private val Guild.audioPlayer
        get() = bot.getGuildAudioPlayer(this)

    /**
     * registers the events for the listener
     */
    fun registerEvents(eventDispatcher: EventDispatcher) {
        eventDispatcher.on(MessageCreateEvent::class.java).subscribe(this::onMessageReceived)
    }

    private fun onMessageReceived(event: MessageCreateEvent) {
        timer.start()
        val message = event.message

        message.content.ifPresent {
            val channel = message.channel.block()

            if (channel is TextChannel) {
                val command = it.split(" ".toRegex(), 2)
                var handled = true
                if (prefix + "play" == command[0] && command.size == 2) {
                    bot.loadAndPlay(channel, command[1])
                } else if (prefix + "skip" == command[0]) {
                    bot.skipTrack(channel)
                } else if (prefix + "joinme" == command[0]) {
                    message.authorAsMember.block()?.let { author -> bot.joinVoice(channel, author) }
                } else if (prefix + "quit" == command[0]) {
                    bot.disconnectVoice(channel)
                } else if (prefix + "pause" == command[0]) {
                    channel.guild.block()?.audioPlayer?.player?.apply {
                        isPaused = !isPaused
                    }
                } else if ((prefix + "volume" == command[0] || prefix + "v" == command[0]) && command.size > 1) {
                    channel.guild.block()?.audioPlayer?.player?.volume = command[1].toInt()
                } else {
                    handled = false
                }
                if (handled) {
                    message.delete().delaySubscription(Duration.ofMillis(2000)).blockOptional()
                }
            }
        }
    }


}