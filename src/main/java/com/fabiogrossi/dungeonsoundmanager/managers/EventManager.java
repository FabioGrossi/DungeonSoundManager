package com.fabiogrossi.dungeonsoundmanager.managers;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.PlayerData;
import com.fabiogrossi.dungeonsoundmanager.data.StateData;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventManager implements Listener {

    private final SoundManager plugin;

    /**
     * Pending removal tasks: quando un mob perde il target non lo rimuoviamo
     * subito (MythicMobs è buggato e può sparare falsi lose-target).
     * Aspettiamo il delay configurato per mob; se nel frattempo il mob
     * riacquisisce il target, cancelliamo il task.
     *
     * Key: UUID del mob
     * Value: task di rimozione in attesa
     */
    private final Map<UUID, BukkitTask> pendingRemovals = new HashMap<>();

    public EventManager(SoundManager plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Player lifecycle
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPlayerManager().addPlayer(event.getPlayer(), null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData pd = plugin.getPlayerManager().getPlayer(player);
        if (pd != null) {
            pd.stopAllSounds();
            if (pd.getTransitionTask() != null) pd.getTransitionTask().cancel();
        }
        plugin.getPlayerManager().removePlayer(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        resetPlayerAudioState(event.getEntity(), "Morte");
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        resetPlayerAudioState(event.getPlayer(), "Respawn");
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        resetPlayerAudioState(event.getPlayer(),
                "Cambio Mondo (" + event.getFrom().getName() + " -> " + event.getPlayer().getWorld().getName() + ")");
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() == event.getTo().getWorld()) {
            if (event.getFrom().distanceSquared(event.getTo()) >= 2500) {
                resetPlayerAudioState(event.getPlayer(), "Teletrasporto Lungo");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Rilevamento combattimento
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("resource")
    public void onMobTargetPlayer(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;

        UUID mobUUID = event.getEntity().getUniqueId();

        ActiveMob am = MythicBukkit.inst().getMobManager()
                .getActiveMob(mobUUID).orElse(null);
        if (am == null) return;

        String mobType = am.getType().getInternalName();
        String mobStateName = plugin.getConfig().getString("mob_states." + mobType + ".state");
        if (mobStateName == null) return;

        StateData combatState = plugin.getResourcePackManager().getStateData(mobStateName);
        if (combatState == null) {
            plugin.getLogger().warning(String.format(
                    "[COMBAT] Il mob '%s' è mappato a '%s' ma quello stato non esiste nel config!",
                    mobType, mobStateName));
            return;
        }

        PlayerData pd = plugin.getPlayerManager().getPlayer(player);
        if (pd == null) return;

        // Annulla eventuale rimozione pendente: il mob ha riacquisito il target
        BukkitTask pending = pendingRemovals.remove(mobUUID);
        if (pending != null) {
            pending.cancel();
            plugin.getLogger().info(String.format(
                    "[COMBAT] '%s' ha riacquisito il target su %s — rimozione annullata",
                    mobType, player.getName()));
        }

        if (!pd.getActiveCombatMobs().containsKey(mobUUID)) {
            plugin.getLogger().info(String.format(
                    "[COMBAT] %s agganciato da '%s' → stato '%s'",
                    player.getName(), mobType, mobStateName));
        }

        pd.registerCombatMob(mobUUID, combatState);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("resource")
    public void onMobLoseTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() != null) return;

        UUID mobUUID = event.getEntity().getUniqueId();

        // Se già c'è un task pendente per questo mob, non schedularne un altro
        if (pendingRemovals.containsKey(mobUUID)) return;

        // Controlla se il mob è effettivamente tracciato da qualche player
        boolean tracked = plugin.getPlayerManager().getPlayerCache().values().stream()
                .anyMatch(pd -> pd.getActiveCombatMobs().containsKey(mobUUID));
        if (!tracked) return;

        ActiveMob am = MythicBukkit.inst().getMobManager()
                .getActiveMob(mobUUID).orElse(null);
        if (am == null) return;

        String mobType = am.getType().getInternalName();

        // Legge il delay configurato per questo specifico mob (in secondi)
        int delaySeconds = plugin.getConfig().getInt(
                "mob_states." + mobType + ".lose_target_delay", 5);
        long delayTicks = delaySeconds * 20L;

        plugin.getLogger().info(String.format(
                "[COMBAT] '%s' ha perso il target. Rimozione tra %ds (configurabile in mob_states.%s.lose_target_delay)",
                mobType, delaySeconds, mobType));

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingRemovals.remove(mobUUID);
            removeMobFromAllPlayers(mobUUID, "perdita target confermata dopo " + delaySeconds + "s");
        }, delayTicks);

        pendingRemovals.put(mobUUID, task);
    }

    // -------------------------------------------------------------------------
    // Rimozione mob (morte e despawn) — immediata, nessun delay
    // -------------------------------------------------------------------------

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        UUID mobUUID = event.getMob().getUniqueId();
        cancelPendingRemoval(mobUUID); // rimozione immediata, cancella eventuale delay
        removeMobFromAllPlayers(mobUUID, "morte");
    }

    @EventHandler
    public void onMythicMobDespawn(MythicMobDespawnEvent event) {
        UUID mobUUID = event.getMob().getUniqueId();
        cancelPendingRemoval(mobUUID);
        removeMobFromAllPlayers(mobUUID, "despawn");
    }

    private void cancelPendingRemoval(UUID mobUUID) {
        BukkitTask pending = pendingRemovals.remove(mobUUID);
        if (pending != null) pending.cancel();
    }

    private void removeMobFromAllPlayers(UUID mobUUID, String reason) {
        plugin.getPlayerManager().getPlayerCache().values().forEach(pd -> {
            if (pd.getActiveCombatMobs().containsKey(mobUUID)) {
                plugin.getLogger().info(String.format(
                        "[COMBAT] Mob rimosso da %s (%s). Combat mob rimanenti: %d",
                        pd.getPlayer().getName(), reason, pd.getActiveCombatMobs().size() - 1));
                pd.removeCombatMob(mobUUID);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Reset audio
    // -------------------------------------------------------------------------

    private void resetPlayerAudioState(Player player, String reason) {
        PlayerData pd = plugin.getPlayerManager().getPlayer(player);
        if (pd != null) {
            plugin.getLogger().info(String.format(
                    "[DEBUG-AUDIO] Reset audio per %s. Motivo: %s", player.getName(), reason));
            pd.stopAllSounds();
            if (pd.getTransitionTask() != null) pd.getTransitionTask().cancel();
            pd.setCurrentPlayState(null);
            pd.setForcedState(null);
            pd.setCurrentPlayingSound(null);
            pd.clearCombatMobs();
        }
    }
}
