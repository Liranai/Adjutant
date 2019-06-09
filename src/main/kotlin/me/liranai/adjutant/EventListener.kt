package me.liranai.adjutant

import com.github.elizabethlfransen.discord.command.CommandManager
import com.github.elizabethlfransen.discord.command.event.CommandPostExecutedEvent
import discord4j.core.`object`.entity.Guild
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.message.MessageCreateEvent
import me.liranai.adjutant.command.Commands
import java.time.Duration
import javax.swing.Timer

class EventListener(private val bot: AdjutantDiscordBot, prefix: String) {
    private var old_text: String = ""
    private val commandManager: CommandManager = Commands(bot).build(prefix)
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
        eventDispatcher.on(CommandPostExecutedEvent::class.java).subscribe(this::onPostCommand)

    }

    private fun onMessageReceived(event: MessageCreateEvent) {
        timer.start()
        commandManager.handle(event)
    }

    private fun onPostCommand(event: CommandPostExecutedEvent) {
        event.details.message.delete().delaySubscription(Duration.ofSeconds(2)).subscribe()
    }


}