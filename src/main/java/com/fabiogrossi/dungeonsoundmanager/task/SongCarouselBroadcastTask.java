package com.fabiogrossi.dungeonsoundmanager.task;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.PlayerData;
import com.fabiogrossi.dungeonsoundmanager.data.SoundData;
import com.fabiogrossi.dungeonsoundmanager.data.StateData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SongCarouselBroadcastTask extends BukkitRunnable {

    private final SoundManager soundManager;

    public SongCarouselBroadcastTask(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    @Override
    public void run() {
        soundManager.getPlayerManager().getPlayerCache().values().forEach(playerData -> {
            System.out.println("1");
            if (playerData.getCurrentPlayingSound() == null) {
                System.out.println("2");
                if (playerData.getCurrentPlayState() == null) {
                    return;
                }
                System.out.println("3");
                StateData currentPlayerState = soundManager.getResourcePackManager().getStateDataCache().get(playerData.getCurrentPlayState().getName());
                if (currentPlayerState == null) {
                    soundManager.getLogger().warning("PlayState " + playerData.getCurrentPlayState() + " doesn't exist. Setting to default");
                    playerData.setCurrentPlayState(null);
                    return;
                }
                System.out.println("4");
                StateData.StateConditions currentPlayerStateConditions = currentPlayerState.getStateConditions();
                if (!currentPlayerStateConditions.areVerified(playerData)) {
                    return;
                }
                System.out.println("Playsound diocan");
                Map<SoundData, SoundData.SoundConditions> soundDataSoundConditionsMap = currentPlayerState.getSounds();
                SoundData soundData = soundDataSoundConditionsMap.keySet().stream().toList().get(ThreadLocalRandom.current().nextInt(0, soundDataSoundConditionsMap.size()));
                playerData.playSound(soundData, 1, 1);
                System.out.println("PlayerData CAROUSEL1: " + playerData.toString());
            }
            if (playerData.getSongStartInstant() != null && (playerData.getElapsedMilliseconds() > playerData.getCurrentPlayingSound().getDuration())) {
                System.out.println("Setting playing sound null");
                playerData.setCurrentPlayingSound(null);
            }
        });
    }

}
