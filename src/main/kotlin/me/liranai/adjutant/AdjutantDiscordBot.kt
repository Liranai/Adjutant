package me.liranai.adjutant

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.`object`.entity.*
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.message.MessageCreateEvent
import me.liranai.adjutant.config.AdjutantConfig
import me.liranai.adjutant.model.GuildMusicManager
import me.liranai.adjutant.model.LavaPlayerAudioProvider
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class AdjutantDiscordBot(private val config: AdjutantConfig) {

    private val selfMessages: MutableList<Message> = mutableListOf()

    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: MutableMap<Long, GuildMusicManager> = ConcurrentHashMap()

    init {
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    fun registerListeners(eventDispatcher: EventDispatcher) {
        eventDispatcher.on(MessageCreateEvent::class.java).subscribe{ event -> this.onMessageReceived(event) }
    }

    @Synchronized
    private fun getGuildAudioPlayer(guild: Guild): GuildMusicManager {
        val guildId = guild.id.asLong()
        var musicManager: GuildMusicManager? = musicManagers[guildId]

        if (musicManager == null) {
            musicManager = GuildMusicManager(playerManager)
            musicManagers[guildId] = musicManager
        }

        return musicManager
    }

    private fun onMessageReceived(event: MessageCreateEvent) {
        selfMessages.clear()
        val message = event.message

        message.content.ifPresent {
            val channel = message.channel.block()

            if (channel is TextChannel) {
                val command = it.split(" ".toRegex(), 2)

                if (config.prefix + "play" == command[0] && command.size == 2) {
                    loadAndPlay(channel, command[1])
                } else if (config.prefix + "skip" == command[0]) {
                    skipTrack(channel)
                } else if (config.prefix + "joinme" == command[0]) {
                    message.authorAsMember.block()?.let { author ->
                            joinVoice(channel, author)
                    }
                } else if (config.prefix + "quit" == command[0]) {
                    disconnectVoice(channel)
                } else if (config.prefix + "pause" == command[0]) {
                    getGuildAudioPlayer(channel.guild.block()!!).player.isPaused = !getGuildAudioPlayer(channel.guild.block()!!).player.isPaused
                } else if ((config.prefix + "volume" == command[0] || config.prefix + "v" == command[0]) && command.size > 1) {
                    getGuildAudioPlayer(channel.guild.block()!!).player.volume = Integer.valueOf(command[1])
                }

                if (command[0].startsWith(config.prefix)) {
                    message.delete().delaySubscription(Duration.ofMillis(2000)).blockOptional()
                }

                for (mes in selfMessages) {
                    println(mes.delete().delaySubscription(Duration.ofMillis(2000)).blockOptional())
                }
            }
        }
    }

    private fun disconnectVoice(channel: TextChannel) {
        sendTempMessageToChannel(channel, "Noot noot")
        channel.guild.block()!!.client.logout().block()
    }

    private fun joinVoice(channel: TextChannel, member: Member) {
        val manager = channel.guild.block()?.let { guild ->
            getGuildAudioPlayer(guild)
        }

        val voiceChannel = member.voiceState.block()?.channel?.block()
        if(voiceChannel != null) {
            sendTempMessageToChannel(channel, "Joining channel: " + voiceChannel.name)
        }
        if(manager != null) {
            voiceChannel?.join {
                    spec -> spec.setProvider(manager.provider)
            }?.block()
        }
    }

    private fun loadAndPlay(channel: TextChannel, trackUrl: String) {
        val musicManager = getGuildAudioPlayer(channel.guild.block()!!)

        playerManager.loadItemOrdered(musicManager, trackUrl, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                sendTempMessageToChannel(channel, "Adding to queue " + track.info.title)

                play(channel.guild.block(), musicManager, track)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                var firstTrack: AudioTrack? = playlist.selectedTrack

                if (firstTrack == null) {
                    firstTrack = playlist.tracks[0]
                }

                sendTempMessageToChannel(channel, "Adding to queue " + firstTrack!!.info.title + " (first track of playlist " + playlist.name + ")")

                play(channel.guild.block(), musicManager, firstTrack)
            }

            override fun noMatches() {
                sendTempMessageToChannel(channel, "Nothing found by $trackUrl")
            }

            override fun loadFailed(exception: FriendlyException) {
                sendTempMessageToChannel(channel, "Could not play: " + exception.message)
            }
        })
    }

    private fun play(guild: Guild?, musicManager: GuildMusicManager, track: AudioTrack?) {
        val manager = getGuildAudioPlayer(guild!!)
        attachToFirstVoiceChannel(guild, manager.provider)
        musicManager.scheduler.queue(track!!)
    }

    private fun skipTrack(channel: TextChannel) {
        val musicManager = getGuildAudioPlayer(channel.guild.block()!!)
        musicManager.scheduler.nextTrack()

        sendTempMessageToChannel(channel, "Skipped to next track.")
    }

    private fun sendTempMessageToChannel(channel: TextChannel, message: String) {
        try {
            channel.createMessage(message).block()?.let { tempMess ->
                selfMessages.add(tempMess)
            }
        } catch (e: Exception) {
            log.warn("Failed to send message {} to {}", message, channel.name, e)
        }

    }

    private fun sendMessageToChannel(channel: TextChannel, message: String) {
        try {
            channel.createMessage(message).block()
        } catch (e: Exception) {
            log.warn("Failed to send message {} to {}", message, channel.name, e)
        }

    }

    private fun removeMessageFromChannel(channel: TextChannel, messageIDs: String) {
        try {
        } catch (e: Exception) {
            log.warn("Failed to remove message {} from {}", messageIDs, channel.name, e)
        }

    }

    companion object {
        private val log = LoggerFactory.getLogger(AdjutantDiscordBot::class.java)

        fun attachToFirstVoiceChannel(guild: Guild, provider: LavaPlayerAudioProvider) {
            val voiceChannel = guild.channels.ofType(VoiceChannel::class.java).blockFirst() ?: TODO("Error Handling")
            val inVoiceChannel = guild.voiceStates
                .any { voiceState ->
                    guild.client.selfId
                        .map(voiceState.userId::equals)
                        .orElse(false)
                }.block() ?: TODO("Error Handling")

            if (!inVoiceChannel) {
                voiceChannel.join{spec -> spec.setProvider(provider)}
                    .block() ?: TODO("Error Handling")
            }

        }
    }
}
