package com.fabiogrossi.dungeonsoundmanager.task;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.PlayerData;
import com.fabiogrossi.dungeonsoundmanager.data.StateData;
import org.bukkit.Bukkit;
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

        StateData targetState;

        if (pd.getForcedState() != null) {
            // Stato forzato manualmente (es. via comando) — ha la precedenza assoluta
            targetState = pd.getForcedState();
        } else {
            targetState = calculateBestState(player, pd);
        }

        // Cambia stato solo se è effettivamente diverso da quello corrente
        if (targetState != pd.getCurrentPlayState()) {
            pd.changeState(targetState);
        }
    }

    /**
     * Calcola lo stato migliore da riprodurre:
     *
     * 1. Se il player ha mob in combattimento attivi (tracciati da EventManager via eventi),
     *    prende lo stato di combattimento con priorità più alta.
     *    Se questo supera il miglior stato ambientale, vince.
     *
     * 2. Altrimenti, valuta tutti gli stati automatici (assignAutomatically=true)
     *    le cui condizioni siano soddisfatte, e prende quello con priorità più alta.
     *    A parità di priorità, vince quello con più condizioni specifiche (più preciso).
     *
     * Non si fa più polling delle entità vicine: il combattimento viene
     * registrato/rimosso tramite eventi in EventManager (danno subito, morte mob).
     */
    private StateData calculateBestState(Player player, PlayerData pd) {
        StateData bestAmbient = calculateBestAmbientState(pd);
        StateData bestCombat = pd.getBestCombatState(); // event-driven, sempre aggiornato

        if (bestCombat == null) {
            return bestAmbient;
        }
        if (bestAmbient == null) {
            return bestCombat;
        }
        return bestCombat.getPriority() >= bestAmbient.getPriority() ? bestCombat : bestAmbient;
    }

    private StateData calculateBestAmbientState(PlayerData pd) {
        StateData best = null;
        int highestPriority = -1;
        int highestConditionCount = -1;

        for (StateData state : soundManager.getResourcePackManager().getStateDataCache().values()) {
            if (!state.isAssignAutomatically()) continue;
            if (!state.getStateConditions().areVerified(pd)) continue;

            int conditionCount = state.getStateConditions().getSpecificConditionCount();

            if (state.getPriority() > highestPriority
                    || (state.getPriority() == highestPriority && conditionCount > highestConditionCount)) {
                highestPriority = state.getPriority();
                highestConditionCount = conditionCount;
                best = state;
            }
        }

        return best;
    }
}
