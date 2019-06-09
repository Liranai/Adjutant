package me.liranai.adjutant

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.TextChannel
import discord4j.core.`object`.entity.VoiceChannel
import me.liranai.adjutant.config.AdjutantConfig
import me.liranai.adjutant.model.AdjutantRecognition
import me.liranai.adjutant.model.GuildMusicManager
import me.liranai.adjutant.model.LavaPlayerAudioProvider
import me.liranai.adjutant.util.sendTempMessage
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class AdjutantDiscordBot(val config: AdjutantConfig) {

    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: MutableMap<Long, GuildMusicManager> = ConcurrentHashMap()



    init {
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
        val adjutant = AdjutantRecognition(config.security.tokens.google)
    }

    @Synchronized
    fun getGuildAudioPlayer(guild: Guild) = musicManagers.computeIfAbsent(guild.id.asLong()) {
        GuildMusicManager(playerManager)
    }


    /**
     * Disconnects the client from the voice channel
     */
    fun disconnectVoice(channel: TextChannel) {
        channel.sendTempMessage("Noot Noot")
        channel.guild.block()!!.client.logout().block()
    }

    /**
     * connects a member to a voice channel
     */
    fun joinVoice(channel: TextChannel, member: Member) {
        // Gets the audio play for the current channel
        val manager = channel.guild.block()?.let { guild ->
            getGuildAudioPlayer(guild)
        }

        val voiceChannel = member.voiceState.block()?.channel?.block()
        if (voiceChannel != null) {
            channel.sendTempMessage("Joining channel: ${voiceChannel.name}")
        }
        if (manager != null) {
            voiceChannel?.join { spec ->
                spec.setProvider(manager.provider)
            }?.block()
        }
    }

    fun loadAndPlay(channel: TextChannel, trackUrl: String) {
        val musicManager = getGuildAudioPlayer(channel.guild.block()!!)

        playerManager.loadItemOrdered(musicManager, trackUrl, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                channel.sendTempMessage("Adding to queue ${track.info.title}")
                play(channel.guild.block(), musicManager, track)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                var firstTrack: AudioTrack? = playlist.selectedTrack

                if (firstTrack == null) {
                    firstTrack = playlist.tracks[0]
                }

                // FIXME what happens if firstTrack is null?
                channel.sendTempMessage("Adding to queue ${firstTrack!!.info.title} (first track of playlist ${playlist.name}")

                play(channel.guild.block(), musicManager, firstTrack)
            }

            override fun noMatches() {
                channel.sendTempMessage("Nothing found by $trackUrl")
            }

            override fun loadFailed(exception: FriendlyException) {
                channel.sendTempMessage("Could not play: ${exception.message}")
            }
        })
    }

    private fun play(guild: Guild?, musicManager: GuildMusicManager, track: AudioTrack?) {
        val manager = getGuildAudioPlayer(guild!!)
        attachToFirstVoiceChannel(guild, manager.provider)
        musicManager.scheduler.queue(track!!)
    }

    fun skipTrack(channel: TextChannel) {
        val musicManager = getGuildAudioPlayer(channel.guild.block()!!)
        musicManager.scheduler.nextTrack()

        channel.sendTempMessage("Skipped to next track.")
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
                voiceChannel.join { spec -> spec.setProvider(provider) }
                    .block() ?: TODO("Error Handling")
            }

        }
    }
}
