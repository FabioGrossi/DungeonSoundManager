package com.fabiogrossi.dungeonsoundmanager.task;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.PlayerData;
import com.fabiogrossi.dungeonsoundmanager.data.StateData;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class StateEvaluatorTask extends BukkitRunnable {

    private final SoundManager soundManager;

    public StateEvaluatorTask(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            evaluatePlayerState(player);
        }
    }

    private void evaluatePlayerState(Player player) {
        PlayerData pd = soundManager.getPlayerManager().getPlayer(player);
        if (pd == null) return;

        StateData bestState = pd.getForcedState();
        if (bestState == null) {
            bestState = calculateBestAutomaticState(player, pd);
        }

        if (bestState != pd.getCurrentPlayState()) {
            pd.changeState(bestState);
        }
    }

    private StateData calculateBestAutomaticState(Player player, PlayerData pd) {
        StateData bestState = null;
        int highestPriority = -1;
        int highestConditionCount = -1;

        // 1. Valuta stati ambientali
        for (StateData state : soundManager.getResourcePackManager().getStateDataCache().values()) {
            if (state.isAssignAutomatically() && state.getStateConditions().areVerified(pd)) {
                int currentConditions = state.getStateConditions().getSpecificConditionCount();
                if (state.getPriority() > highestPriority) {
                    highestPriority = state.getPriority();
                    highestConditionCount = currentConditions;
                    bestState = state;
                } else if (state.getPriority() == highestPriority && currentConditions > highestConditionCount) {
                    highestConditionCount = currentConditions;
                    bestState = state;
                }
            }
        }

        // 2. Valuta combattimento MythicMobs
        for (Entity entity : player.getNearbyEntities(40, 40, 40)) {
            StateData combatState = getCombatStateFromEntity(entity, player);
            if (combatState != null && combatState.getPriority() > highestPriority) {
                highestPriority = combatState.getPriority();
                bestState = combatState;
            }
        }

        return bestState;
    }

    @SuppressWarnings("resource")
    private StateData getCombatStateFromEntity(Entity entity, Player player) {
        ActiveMob am = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).orElse(null);
        if (am != null && isPlayerInCombatWithMob(am, player)) {
            String mobStateName = soundManager.getConfig().getString("mob_states." + am.getType().getInternalName());
            if (mobStateName != null) {
                return soundManager.getResourcePackManager().getStateData(mobStateName);
            }
        }
        return null;
    }

    private boolean isPlayerInCombatWithMob(ActiveMob am, Player player) {
        if (am.hasThreatTable()) {
            return am.getThreatTable().getAllThreatTargets().stream()
                    .anyMatch(target -> target.getBukkitEntity().getUniqueId().equals(player.getUniqueId()));
        }
        return am.getEntity().getTarget() != null && am.getEntity().getTarget().getUniqueId().equals(player.getUniqueId());
    }
}