package com.fabiogrossi.dungeonsoundmanager.managers;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.PlayerData;
import com.fabiogrossi.dungeonsoundmanager.data.SoundData;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerManager {

    private final SoundManager soundManager;

    @Getter
    private final Map<Player, PlayerData> playerCache = new HashMap<>();

    public PlayerManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public void addPlayer(Player player, SoundData currentPlayingSound) {
        // FIX: Ora passiamo il soundManager alla PlayerData per gestire i delay
        PlayerData playerData = new PlayerData(soundManager, player, currentPlayingSound);
        playerCache.put(player, playerData);
    }

    public PlayerData getPlayer(Player player) {
        return playerCache.get(player);
    }

    public void removePlayer(Player player) {
        playerCache.remove(player);
    }

    public PlayerData getPlayerPlayingSound(SoundData soundData) {
        for (PlayerData playerData : playerCache.values()) {
            Player player = playerData.getPlayer();
            if (player == null || !player.isOnline()) {
                return null;
            }
            if (playerData.getCurrentPlayingSound() != null && playerData.getCurrentPlayingSound().equals(soundData)) {
                return playerData;
            }
        }
        return null;
    }
}