package me.liranai.adjutant

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

/**
 * Holder for both the player and a track scheduler for one guild.
 */
class GuildMusicManager (manager: AudioPlayerManager) {
/**
 * Creates a player and a track scheduler.
 * @param manager Audio player manager to use for creating the player.
 */
    /**
     * Audio player for the guild.
     */
    val player: AudioPlayer = manager.createPlayer()
    /** Track scheduler for the player. */
    val scheduler: TrackScheduler = TrackScheduler(player)

    val provider: LavaPlayerAudioProvider = LavaPlayerAudioProvider(player);

    init {
        player.addListener(scheduler)
    }
}
