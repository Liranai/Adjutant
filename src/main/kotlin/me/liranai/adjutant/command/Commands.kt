package me.liranai.adjutant.command

import com.github.elizabethlfransen.discord.command.CommandDetails
import com.github.elizabethlfransen.discord.command.CommandExecutor
import com.github.elizabethlfransen.discord.command.SimpleCommandManager
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.TextChannel
import kotlinx.io.InputStream
import me.liranai.adjutant.AdjutantDiscordBot
import me.liranai.adjutant.util.sendTempMessage
import java.io.File

class Commands(private val bot: AdjutantDiscordBot) {
    private val Guild.audioPlayer
        get() = bot.getGuildAudioPlayer(this)

    private fun ping(channel: TextChannel) {
        channel.sendTempMessage("Pong!")
    }

    private fun play(channel: TextChannel, url: String) {
        bot.loadAndPlay(channel, url)
    }

    private fun skip(channel: TextChannel) {
        bot.skipTrack(channel)
    }

    private fun joinMe(message: Message, channel: TextChannel) {
        message.authorAsMember.subscribe { author ->
            bot.joinVoice(channel, author)
        }
    }

    private fun quit(channel: TextChannel) {
        bot.disconnectVoice(channel)
    }

    private fun pause(channel: TextChannel) {
        channel.guild.map { it.audioPlayer.player }.subscribe {
            it.isPaused = !it.isPaused
        }
    }

    private fun volume(channel: TextChannel, volume: Int) {
        channel.guild.map { it.audioPlayer.player }.subscribe {
            it.volume = volume
        }
    }

    private fun assertArgs(details: CommandDetails, num: Int, execute: CommandExecutor) {
        if (details.args.size < num) {
            details.channel.sendTempMessage("Missing arguments")
        } else {
            execute(details)
        }
    }


    fun build(prefix: String): SimpleCommandManager {
        return SimpleCommandManager.Builder()
            .prefix(prefix)
            .withCommand("ping") { this.ping(it.channel) }
            .withCommand("play") { details ->
                assertArgs(details, 1) {
                    play(it.channel, it.args[0])
                }
            }
            .withCommand("skip") { skip(it.channel) }
            .withCommand("joinme") { joinMe(it.message, it.channel) }
            .withCommand("quit") { quit(it.channel) }
            .withCommand("pause") { pause(it.channel) }
            .withCommand("volume", "v") { details ->
                assertArgs(details, 1) {
                    volume(it.channel, it.args[0].toInt())
                }
            }
            .build()
    }
}