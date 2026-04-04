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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class PlayerData {

    private final SoundManager plugin;
    private final Player player;

    @Setter private SoundData currentPlayingSound;
    @Setter private Instant songStartInstant;
    @Setter private StateData currentPlayState = null;
    @Setter private StateData forcedState = null;

    private BukkitTask transitionTask = null;

    /**
     * Mob attualmente in combattimento col player.
     * Key: UUID del mob, Value: StateData della categoria musicale associata al mob.
     * Popolato da EventManager su danno, svuotato su morte/despawn del mob.
     */
    private final Map<UUID, StateData> activeCombatMobs = new HashMap<>();

    public PlayerData(SoundManager plugin, Player player, SoundData currentPlayingSound) {
        this.plugin = plugin;
        this.player = player;
        this.currentPlayingSound = currentPlayingSound;
    }

    // -------------------------------------------------------------------------
    // Gestione combattimento (event-driven)
    // -------------------------------------------------------------------------

    public void registerCombatMob(UUID mobUUID, StateData combatState) {
        activeCombatMobs.put(mobUUID, combatState);
    }

    public void removeCombatMob(UUID mobUUID) {
        activeCombatMobs.remove(mobUUID);
    }

    public void clearCombatMobs() {
        activeCombatMobs.clear();
    }

    /**
     * Calcola lo stato di combattimento attivo con priorità più alta tra tutti i mob
     * che stanno attualmente combattendo col player.
     * Restituisce null se nessun mob è in combattimento.
     */
    public StateData getBestCombatState() {
        return activeCombatMobs.values().stream()
                .max(java.util.Comparator.comparingInt(StateData::getPriority))
                .orElse(null);
    }

    public boolean hasCombatMobs() {
        return !activeCombatMobs.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Gestione stato
    // -------------------------------------------------------------------------

    public void changeState(StateData newState) {
        if (this.currentPlayState != null && newState != null
                && this.currentPlayState.getName().equals(newState.getName())) {
            return;
        }

        plugin.getLogger().info(String.format("[DEBUG-AUDIO] %s cambia stato: %s -> %s",
                player.getName(),
                (this.currentPlayState != null ? this.currentPlayState.getName() : "NULL"),
                (newState != null ? newState.getName() : "NULL")));

        boolean isDeescalation = this.currentPlayState != null && newState != null
                && this.currentPlayState.getPriority() > newState.getPriority();
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
                executeDeescalationDelay(newState, oldState);
            } else {
                playRandomSoundFromState(newState);
            }
        }
    }

    private void executeDeescalationDelay(StateData newState, StateData oldState) {
        if (oldState.getOutroStinger() != null) {
            plugin.getLogger().info(String.format("[DEBUG-AUDIO] Outro Stinger: %s", oldState.getOutroStinger().getId()));
            player.playSound(player.getLocation(), oldState.getOutroStinger().getId(),
                    SoundCategory.valueOf(oldState.getOutroStinger().getDefaultPlayCategory()), 1F, 1F);
        }

        int delaySeconds = plugin.getConfig().getInt("deescalation_delay", 5);
        long delayTicks = delaySeconds * 20L;

        plugin.getLogger().info(String.format("[DEBUG-AUDIO] De-escalation. Attesa %ds.", delaySeconds));

        this.transitionTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.transitionTask = null;
            if (this.currentPlayState != null && this.currentPlayState.getName().equals(newState.getName())) {
                playRandomSoundFromState(newState);
            }
        }, delayTicks);
    }

    public void playRandomSoundFromState(StateData state) {
        Map<SoundData, SoundData.SoundConditions> sounds = state.getSounds();
        if (sounds == null || sounds.isEmpty()) return;

        List<SoundData> validSounds = sounds.keySet().stream()
                .filter(s -> sounds.get(s) == null || sounds.get(s).areVerified(this))
                .toList();

        if (validSounds.isEmpty()) return;

        SoundData selected = validSounds.get(ThreadLocalRandom.current().nextInt(validSounds.size()));
        plugin.getLogger().info(String.format("[DEBUG-AUDIO] Suono avviato: %s", selected.getId()));
        playSound(selected, 1F, 1F);
    }

    // -------------------------------------------------------------------------
    // Audio
    // -------------------------------------------------------------------------

    public void playSound(SoundData soundData, float volume, float pitch) {
        player.playSound(player.getLocation(), soundData.getId(),
                SoundCategory.valueOf(soundData.getDefaultPlayCategory()), volume, pitch);
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
        if (songStartInstant == null) return 0;
        return ChronoUnit.MILLIS.between(songStartInstant, Instant.now());
    }
}
