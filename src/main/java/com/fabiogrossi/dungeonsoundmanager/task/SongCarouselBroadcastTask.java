package com.fabiogrossi.dungeonsoundmanager.task;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import org.bukkit.scheduler.BukkitRunnable;

public class SongCarouselBroadcastTask extends BukkitRunnable {

    private final SoundManager soundManager;

    public SongCarouselBroadcastTask(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    @Override
    public void run() {
        soundManager.getPlayerManager().getPlayerCache().values().forEach(playerData -> {
            if (playerData.getCurrentPlayState() == null) return;

            // FIX: Se il giocatore è in pausa silenziosa di "de-escalation", il Carousel non deve intervenire!
            if (playerData.getTransitionTask() != null) return;

            // Se la canzone è finita (o non c'è), ne avvia un'altra dello stato attuale
            if (playerData.getCurrentPlayingSound() == null ||
                    (playerData.getSongStartInstant() != null && playerData.getElapsedMilliseconds() >= playerData.getCurrentPlayingSound().getDuration())) {

                if (playerData.getCurrentPlayingSound() != null) {
                    playerData.setCurrentPlayingSound(null);
                }

                playerData.playRandomSoundFromState(playerData.getCurrentPlayState());
            }
        });
    }
}