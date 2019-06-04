import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdjutantDiscordBot {
    private static final Logger log = LoggerFactory.getLogger(AdjutantDiscordBot.class);
    private static final String prefix = ">";

    private ArrayList<Message> selfMessages;

    public static void main(String[] args) {
        DiscordClient client = new DiscordClientBuilder(args[0]).build();
        new AdjutantDiscordBot().registerListeners(client.getEventDispatcher());
        client.login().block();
    }

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    private AdjutantDiscordBot() {
        this.musicManagers = new ConcurrentHashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    private void registerListeners(EventDispatcher eventDispatcher) {
        eventDispatcher.on(MessageCreateEvent.class).subscribe(this::onMessageReceived);
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = guild.getId().asLong();
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        return musicManager;
    }

    private void onMessageReceived(MessageCreateEvent event) {
        selfMessages = new ArrayList<>();
        Message message = event.getMessage();

        message.getContent().ifPresent(it -> {
            MessageChannel channel = message.getChannel().block();

            if (channel instanceof TextChannel) {
                String[] command = it.split(" ", 2);

                if ((prefix+"play").equals(command[0]) && command.length == 2) {
                    loadAndPlay((TextChannel) channel, command[1]);
                } else if ((prefix+"skip").equals(command[0])) {
                    skipTrack((TextChannel) channel);
                } else if ((prefix + "joinme").equals(command[0])) {
                    joinVoice((TextChannel) channel, message.getAuthorAsMember().block());

                } else if ((prefix + "quit").equals(command[0])) {
                    disconnectVoice((TextChannel) channel);
                } else if ((prefix + "pause").equals(command[0])){
                    getGuildAudioPlayer(((TextChannel) channel).getGuild().block()).player.setPaused(!(getGuildAudioPlayer(((TextChannel) channel).getGuild().block()).player.isPaused()));
                } else if (((prefix + "volume").equals(command[0]) || (prefix + "v").equals(command[0])) && command.length > 1){
                    getGuildAudioPlayer(((TextChannel) channel).getGuild().block()).player.setVolume(Integer.valueOf(command[1]));
                }

                if(command[0].startsWith(">")) {
                    message.delete().delaySubscription(Duration.ofMillis(2000)).blockOptional();
                }

                for(Message mes : selfMessages){
                    System.out.println(mes.delete().delaySubscription(Duration.ofMillis(2000)).blockOptional());
                }
            }
        });
    }

    private void disconnectVoice(TextChannel channel){
        sendTempMessageToChannel(channel, "Noot noot");
        channel.getGuild().block().getClient().logout().block();
//        getGuildAudioPlayer(channel.getGuild().block()).player.removeListener(getGuildAudioPlayer(channel.getGuild().block()).scheduler);
//        getGuildAudioPlayer(channel.getGuild().block()).player.checkCleanup(100);
    }

    private void joinVoice(TextChannel channel, Member member) {
        Guild guild = channel.getGuild().block();
        GuildMusicManager manager = getGuildAudioPlayer(guild);

        VoiceChannel voiceChannel = member.getVoiceState().block().getChannel().block();
        sendTempMessageToChannel(channel, "Joining channel: " + voiceChannel.getName());
        voiceChannel.join(spec -> spec.setProvider(manager.provider)).block();
    }

    private void loadAndPlay(TextChannel channel, final String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild().block());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                sendTempMessageToChannel(channel, "Adding to queue " + track.getInfo().title);

                play(channel.getGuild().block(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                sendTempMessageToChannel(channel, "Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")");

                play(channel.getGuild().block(), musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                sendTempMessageToChannel(channel, "Nothing found by " + trackUrl);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                sendTempMessageToChannel(channel, "Could not play: " + exception.getMessage());
            }
        });
    }

    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        GuildMusicManager manager = getGuildAudioPlayer(guild);
        attachToFirstVoiceChannel(guild, manager.provider);
        musicManager.scheduler.queue(track);
    }

    private void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild().block());
        musicManager.scheduler.nextTrack();

        sendTempMessageToChannel(channel, "Skipped to next track.");
    }

    private void sendTempMessageToChannel(TextChannel channel, String message) {
        try {
            selfMessages.add(channel.createMessage(message).block());
        } catch (Exception e) {
            log.warn("Failed to send message {} to {}", message, channel.getName(), e);
        }
    }

    private void sendMessageToChannel(TextChannel channel, String message) {
        try{
            channel.createMessage(message).block();
        } catch (Exception e) {
            log.warn("Failed to send message {} to {}", message, channel.getName(), e);
        }
    }

    private void removeMessageFromChannel(TextChannel channel, String messageIDs) {
        try {
        } catch (Exception e) {
            log.warn("Failed to remove message {} from {}", messageIDs, channel.getName(), e);
        }
    }

    private static void attachToFirstVoiceChannel(Guild guild, LavaPlayerAudioProvider provider) {
        VoiceChannel voiceChannel = guild.getChannels().ofType(VoiceChannel.class).blockFirst();
        boolean inVoiceChannel = guild.getVoiceStates() // Check if any VoiceState for this guild relates to bot
                .any(voiceState -> guild.getClient().getSelfId().map(voiceState.getUserId()::equals).orElse(false))
                .block();

        if (!inVoiceChannel) {
            voiceChannel.join(spec -> spec.setProvider(provider)).block();
        }
    }
}
