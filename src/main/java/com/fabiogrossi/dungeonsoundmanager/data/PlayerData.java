package com.fabiogrossi.dungeonsoundmanager.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Getter
public class PlayerData {

    private final Player player;

    @Setter
    private SoundData currentPlayingSound;

    @Setter
    private Instant songStartInstant;

    @Setter
    private StateData currentPlayState = null;

    public void playSound(SoundData soundData, float volume, float pitch) {
        player.playSound(player.getLocation(), soundData.getId(), SoundCategory.valueOf(soundData.getDefaultPlayCategory()), volume, pitch);
        currentPlayingSound = soundData;
        songStartInstant = Instant.now();
    }

    public void stopSound(SoundCategory soundCategory) {
        player.stopSound(soundCategory);
        currentPlayingSound = null;
        songStartInstant = null;
    }

    public void stopAllSounds() {
        player.stopAllSounds();
        currentPlayingSound = null;
        songStartInstant = null;
    }

    public void stopSound(SoundData soundData) {
        player.stopSound(soundData.getId());
        currentPlayingSound = null;
        songStartInstant = null;
    }

    public PlayerData(Player player, SoundData currentPlayingSound) {
        this.player = player;
        this.currentPlayingSound = currentPlayingSound;
    }

    public long getSoundMillisecondsLasting() {
        Instant terminateInstant = songStartInstant.plusMillis(currentPlayingSound.getDuration());
        return ChronoUnit.MILLIS.between(Instant.now(), terminateInstant);
    }

    public long getElapsedMilliseconds() {
        if (songStartInstant == null) {
            return 0;
        }
        return ChronoUnit.MILLIS.between(songStartInstant, Instant.now());
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "player=" + player +
                ", currentPlayingSound=" + currentPlayingSound +
                ", songStartInstant=" + songStartInstant +
                ", currentPlayState=" + currentPlayState +
                '}';
    }
}
