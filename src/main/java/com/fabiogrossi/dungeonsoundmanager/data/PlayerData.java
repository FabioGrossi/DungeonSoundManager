package com.fabiogrossi.dungeonsoundmanager.data;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class PlayerData {

    private final SoundManager plugin;
    private final Player player;

    @Setter
    private SoundData currentPlayingSound;

    @Setter
    private Instant songStartInstant;

    @Setter
    private StateData currentPlayState = null;

    @Setter
    private StateData forcedState = null;

    private BukkitTask transitionTask = null;

    public PlayerData(SoundManager plugin, Player player, SoundData currentPlayingSound) {
        this.plugin = plugin;
        this.player = player;
        this.currentPlayingSound = currentPlayingSound;
    }

    public void changeState(StateData newState) {
        plugin.getLogger().info("[DEBUG-AUDIO] " + player.getName() + " cambia stato: " +
                (this.currentPlayState != null ? this.currentPlayState.getName() : "NULL") +
                " -> " + (newState != null ? newState.getName() : "NULL"));

        if (this.currentPlayState != null && newState != null && this.currentPlayState.getName().equals(newState.getName())) {
            return;
        }

        boolean isDeescalation = this.currentPlayState != null && newState != null && this.currentPlayState.getPriority() > newState.getPriority();

        // Salviamo una referenza allo stato VECCHIO prima di sovrascriverlo, ci serve per l'outro!
        StateData oldState = this.currentPlayState;
        this.currentPlayState = newState;

        if (this.transitionTask != null) {
            this.transitionTask.cancel();
            this.transitionTask = null;
        }

        if (this.currentPlayingSound != null) {
            stopSound(this.currentPlayingSound);
        }

        if (newState != null) {
            if (isDeescalation) {
                // ESECUZIONE DEL MASKING STINGER (OUTRO)
                if (oldState != null && oldState.getOutroStinger() != null) {
                    plugin.getLogger().info("[DEBUG-AUDIO] Esecuzione Outro Stinger: " + oldState.getOutroStinger().getId());
                    player.playSound(player.getLocation(),
                            oldState.getOutroStinger().getId(),
                            SoundCategory.valueOf(oldState.getOutroStinger().getDefaultPlayCategory()),
                            1F, 1F);
                }

                int delaySeconds = plugin.getConfig().getInt("deescalation_delay", 5);
                long delayTicks = delaySeconds * 20L;

                plugin.getLogger().info("[DEBUG-AUDIO] De-escalation rilevata. Attesa di " + delaySeconds + " secondi prima del prossimo brano.");

                this.transitionTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    this.transitionTask = null;

                    if (this.currentPlayState != null && this.currentPlayState.getName().equals(newState.getName())) {
                        playRandomSoundFromState(newState);
                    }
                }, delayTicks);
            } else {
                playRandomSoundFromState(newState);
            }
        }
    }

    public void playRandomSoundFromState(StateData state) {
        Map<SoundData, SoundData.SoundConditions> sounds = state.getSounds();
        if (sounds == null || sounds.isEmpty()) return;

        List<SoundData> validSounds = sounds.keySet().stream()
                .filter(s -> sounds.get(s) == null || sounds.get(s).areVerified(this))
                .toList();

        if (validSounds.isEmpty()) return;

        SoundData selected = validSounds.get(ThreadLocalRandom.current().nextInt(0, validSounds.size()));
        plugin.getLogger().info("[DEBUG-AUDIO] Selezionato e avviato suono: " + selected.getId());
        playSound(selected, 1F, 1F);
    }

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
        if (soundData != null && soundData.getId() != null) {
            player.stopSound(soundData.getId(), SoundCategory.valueOf(soundData.getDefaultPlayCategory()));
        }
        currentPlayingSound = null;
        songStartInstant = null;
    }

    public long getElapsedMilliseconds() {
        if (songStartInstant == null) {
            return 0;
        }
        return ChronoUnit.MILLIS.between(songStartInstant, Instant.now());
    }
}