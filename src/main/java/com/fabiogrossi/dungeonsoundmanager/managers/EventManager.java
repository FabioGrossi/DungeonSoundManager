package com.fabiogrossi.dungeonsoundmanager.managers;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.mythicmobs.PlaySoundInStateMechanic;
import io.lumine.mythic.api.skills.SkillManager;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.lang.reflect.Field;

public class EventManager implements Listener {

    private final SoundManager soundManager;

    public EventManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        soundManager.getPlayerManager().addPlayer(event.getPlayer(), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        soundManager.getPlayerManager().removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onMythicMechanicLoad(MythicMechanicLoadEvent event) {
        if (event.getMechanicName().equalsIgnoreCase("playstatesound")) {
            SkillExecutor skillExecutor = MythicBukkit.inst().getSkillManager();
            File file = new File(event.getContainer().getFilePath());

            SkillMechanic skillMechanic = new PlaySoundInStateMechanic(skillExecutor, file, event.getConfig().getLine(), event.getConfig(), event.getContainer().getTimerInterval(), soundManager);
            event.register(skillMechanic);
        }
    }

}
