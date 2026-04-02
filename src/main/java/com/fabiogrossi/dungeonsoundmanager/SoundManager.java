package com.fabiogrossi.dungeonsoundmanager;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.fabiogrossi.dungeonsoundmanager.command.SoundManagerCommand;
import com.fabiogrossi.dungeonsoundmanager.managers.EventManager;
import com.fabiogrossi.dungeonsoundmanager.managers.PlayerManager;
import com.fabiogrossi.dungeonsoundmanager.managers.ResourcePackManager;
import com.fabiogrossi.dungeonsoundmanager.task.SongCarouselBroadcastTask;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class SoundManager extends JavaPlugin {

    private ResourcePackManager resourcePackManager;

    private ProtocolManager protocolManager;

    private PlayerManager playerManager;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();

            resourcePackManager = new ResourcePackManager(this);
            resourcePackManager.processResourcePack(getDataFolder().toPath().resolve(getConfig().getString("resource_pack_temp_directory")),
                    getDataFolder().toPath().resolve(getConfig().getString("resource_pack")));

            List<String> availableSongs = new ArrayList<>();
            resourcePackManager.getSoundDataCache().keySet().forEach(soundName -> {
                availableSongs.add(soundName.replace(".", "-"));
            });
            getConfig().set("available_songs", availableSongs);
            saveConfig();

            PaperCommandManager paperCommandManager = new PaperCommandManager(this);
            CommandCompletions<BukkitCommandCompletionContext> completionContext = paperCommandManager.getCommandCompletions();
            registerCommands(paperCommandManager, completionContext);

            Bukkit.getPluginManager().registerEvents(new EventManager(this), this);

            playerManager = new PlayerManager(this);

            new SongCarouselBroadcastTask(this).runTaskTimer(this, 0, 20 * 10);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerCommands(PaperCommandManager paperCommandManager, CommandCompletions<BukkitCommandCompletionContext> completionContext) {
        paperCommandManager.registerCommand(new SoundManagerCommand(this, completionContext));
    }

    @Override
    public void onDisable() {

    }
}
