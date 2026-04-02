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
            PlayerData pd = soundManager.getPlayerManager().getPlayer(player);
            if (pd == null) continue;

            StateData bestState = null;
            int highestPriority = -1;
            int highestConditionCount = -1;

            // 1. STATI AMBIENTALI (Valuta Priorità e Specificità)
            for (StateData state : soundManager.getResourcePackManager().getStateDataCache().values()) {
                if (state.isAssignAutomatically() && state.getStateConditions().areVerified(pd)) {
                    int currentConditions = state.getStateConditions().getSpecificConditionCount();

                    if (state.getPriority() > highestPriority) {
                        highestPriority = state.getPriority();
                        highestConditionCount = currentConditions;
                        bestState = state;
                    } else if (state.getPriority() == highestPriority) {
                        // Se la priorità è uguale, vince lo stato che richiede più requisiti (es: notte + pioggia vince su solo notte)
                        if (currentConditions > highestConditionCount) {
                            highestConditionCount = currentConditions;
                            bestState = state;
                        }
                    }
                }
            }

            // 2. STATI DI COMBATTIMENTO (MythicMobs)
            for (Entity entity : player.getNearbyEntities(40, 40, 40)) {
                ActiveMob am = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId()).orElse(null);
                if (am != null) {
                    boolean inCombat = false;
                    if (am.hasThreatTable()) {
                        inCombat = am.getThreatTable().getAllThreatTargets().stream()
                                .anyMatch(target -> target.getBukkitEntity().getUniqueId().equals(player.getUniqueId()));
                    }
                    if (!inCombat && am.getEntity().getTarget() != null) {
                        inCombat = am.getEntity().getTarget().getUniqueId().equals(player.getUniqueId());
                    }

                    if (inCombat) {
                        String mobStateName = soundManager.getConfig().getString("mob_states." + am.getType().getInternalName());
                        if (mobStateName != null) {
                            StateData combatState = soundManager.getResourcePackManager().getStateData(mobStateName);
                            if (combatState != null && combatState.getPriority() > highestPriority) {
                                highestPriority = combatState.getPriority();
                                bestState = combatState;
                            }
                        }
                    }
                }
            }

            // 3. FORZATURA MANUALE
            if (pd.getForcedState() != null) {
                bestState = pd.getForcedState();
            }

            if (bestState != pd.getCurrentPlayState()) {
                pd.changeState(bestState);
            }
        }
    }
}