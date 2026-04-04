package com.fabiogrossi.dungeonsoundmanager.managers;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class EventManager implements Listener {

    private final SoundManager plugin;

    public EventManager(SoundManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Inizializza il giocatore nella cache appena entra per prevenire NullPointerException
        plugin.getPlayerManager().addPlayer(player, null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData pd = plugin.getPlayerManager().getPlayer(player);

        // Pulizia totale della RAM e dei Task pendenti per evitare Memory Leak
        if (pd != null) {
            pd.stopAllSounds();
            if (pd.getTransitionTask() != null) {
                pd.getTransitionTask().cancel();
            }
        }
        plugin.getPlayerManager().removePlayer(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Zittisce l'audio istantaneamente quando il giocatore muore
        resetPlayerAudioState(event.getEntity(), "Morte");
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Sicurezza extra per resettare l'ambiente al respawn
        resetPlayerAudioState(event.getPlayer(), "Respawn");
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // Supporto Multiverse: resetta sempre l'audio se si cambia dimensione
        resetPlayerAudioState(event.getPlayer(), "Cambio Mondo (" + event.getFrom().getName() + " -> " + event.getPlayer().getWorld().getName() + ")");
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Ignora i micro-teleporti (Enderpearl, Chorus Fruit) per non interrompere la musica
        // Resetta l'audio solo se è un teletrasporto lungo (es. /tpa o /spawn)
        if (event.getFrom().getWorld() == event.getTo().getWorld()) {
            if (event.getFrom().distanceSquared(event.getTo()) >= 2500) { // Distanza ≥ 50 blocchi
                resetPlayerAudioState(event.getPlayer(), "Teletrasporto Lungo");
            }
        }
    }

    /**
     * Metodo Helper: Zittisce la musica forzatamente e azzera gli stati attivi.
     * Al tick successivo, il Regista (Evaluator) ripartirà da zero leggendo il nuovo ambiente.
     */
    private void resetPlayerAudioState(Player player, String reason) {
        PlayerData pd = plugin.getPlayerManager().getPlayer(player);
        if (pd != null) {
            plugin.getLogger().info(String.format("[DEBUG-AUDIO] Reset audio forzato per %s. Motivo: %s", player.getName(), reason));
            pd.stopAllSounds();

            if (pd.getTransitionTask() != null) {
                pd.getTransitionTask().cancel();
            }

            // Svuota forzatamente tutti gli stati attuali e futuri
            pd.setCurrentPlayState(null);
            pd.setForcedState(null);
            pd.setCurrentPlayingSound(null);
        }
    }
}