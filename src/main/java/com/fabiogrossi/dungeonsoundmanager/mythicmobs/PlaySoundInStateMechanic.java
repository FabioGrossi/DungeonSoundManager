package com.fabiogrossi.dungeonsoundmanager.mythicmobs;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.PlayerData;
import com.fabiogrossi.dungeonsoundmanager.data.SoundData;
import com.fabiogrossi.dungeonsoundmanager.data.StateData;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PlaySoundInStateMechanic extends SkillMechanic implements ITargetedEntitySkill {

    private final SoundManager soundManager;

    private StateData stateData;
    private float volume = 1;
    private float pitch = 1;
    private boolean ignoreStateAndSoundConditions = true;

    public PlaySoundInStateMechanic(SkillExecutor manager, File file, String line, MythicLineConfig config, int interval, SoundManager soundManager) {
        super(manager, file, line, config, interval);
        this.soundManager = soundManager;

        this.setAsyncSafe(false);
        this.setTargetsCreativePlayers(true);

        String stateName = config.getString(new String[]{"soundcategory", "sc"});
        if (stateName !=  null) {
            StateData state = soundManager.getResourcePackManager().getStateData(stateName);
            if (state == null) {
                soundManager.getLogger().warning("State " + stateName + " has not be found, but was mentioned in metaskill: " + file.getName());
                return;
            }
            stateData = state;
        } else {
            throw new RuntimeException("soundactegory in " + file.getName() + " cannot be null");
        }

        volume = config.getFloat(new String[]{"voume", "v"}, 1F);
        pitch = config.getFloat(new String[]{"pitch", "p"}, 1F);
        ignoreStateAndSoundConditions = config.getBoolean(new String[]{"ignoreconditions", "ic"}, true);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity abstractEntity) {
        if (!abstractEntity.isPlayer()) {
            return SkillResult.SUCCESS;
        }
        Player player = (Player) abstractEntity.getBukkitEntity();
        PlayerData playerData = soundManager.getPlayerManager().getPlayer(player);
        if (!ignoreStateAndSoundConditions && !stateData.getStateConditions().areVerified(playerData)) {
            return SkillResult.SUCCESS;
        }
        Map<SoundData, SoundData.SoundConditions> stateSounds = stateData.getSounds();
        SoundData selectedSound;
        if (!ignoreStateAndSoundConditions) {
            do {
                selectedSound = stateSounds.keySet().stream().toList().get(ThreadLocalRandom.current().nextInt(0, stateSounds.size()));
            } while (!stateSounds.get(selectedSound).areVerified(playerData));
        } else {
            selectedSound = stateSounds.keySet().stream().toList().get(ThreadLocalRandom.current().nextInt(0, stateSounds.size()));
        }
        System.out.println("PlayerData MECHANIC1: " + playerData.toString());
        System.out.println("CURRENTPLY PLAYING: " + playerData.getCurrentPlayingSound().getId());
        if (playerData.getCurrentPlayingSound().getId() != null) {
            System.out.print("Current playing song is not null. stopping it");
            playerData.stopSound(playerData.getCurrentPlayingSound());
        }
        playerData.playSound(selectedSound, volume, pitch);
        System.out.println("PlayerData MECHANIC2: " + playerData.toString());
        return SkillResult.SUCCESS;
    }
}
