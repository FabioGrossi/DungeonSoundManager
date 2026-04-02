package com.fabiogrossi.dungeonsoundmanager;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.fabiogrossi.dungeonsoundmanager.command.SoundManagerCommand;
import com.fabiogrossi.dungeonsoundmanager.managers.PlayerManager;
import com.fabiogrossi.dungeonsoundmanager.managers.ResourcePackManager;
import com.fabiogrossi.dungeonsoundmanager.task.SongCarouselBroadcastTask;
import com.fabiogrossi.dungeonsoundmanager.task.StateEvaluatorTask;
import io.lumine.mythic.bukkit.MythicBukkit;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

@Getter
public class SoundManager extends JavaPlugin {

    private ResourcePackManager resourcePackManager;
    private ProtocolManager protocolManager;
    private PlayerManager playerManager;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            protocolManager = ProtocolLibrary.getProtocolManager();

            String tempDir = getConfig().getString("resource_pack_temp_directory", "temp");
            String packName = getConfig().getString("resource_pack", "Skyrim.zip");

            resourcePackManager = new ResourcePackManager(this);
            resourcePackManager.processResourcePack(
                    getDataFolder().toPath().resolve(tempDir),
                    getDataFolder().toPath().resolve(packName)
            );

            PaperCommandManager paperCommandManager = new PaperCommandManager(this);
            CommandCompletions<BukkitCommandCompletionContext> completionContext = paperCommandManager.getCommandCompletions();
            registerCommands(paperCommandManager, completionContext);

            playerManager = new PlayerManager(this);

            new SongCarouselBroadcastTask(this).runTaskTimer(this, 0, 20 * 5);
            new StateEvaluatorTask(this).runTaskTimer(this, 0, 20);

            validateConfig();

        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Impossibile caricare la Resource Pack!", e);
        }
    }

    @SuppressWarnings("resource")
    public void validateConfig() {
        getLogger().info("[VALIDAZIONE] Controllo integrità configurazioni...");
        ConfigurationSection mobStates = getConfig().getConfigurationSection("mob_states");
        if (mobStates != null) {
            for (String mobName : mobStates.getKeys(false)) {
                if (MythicBukkit.inst().getMobManager().getMythicMob(mobName).isEmpty()) {
                    getLogger().warning(String.format("[ATTENZIONE] Il mob '%s' in config.yml non esiste in MythicMobs!", mobName));
                }
            }
        }
    }

    public void reloadEntirePlugin() {
        getLogger().info("Inizio ricaricamento completo del DungeonSoundManager...");
        Bukkit.getScheduler().cancelTasks(this);

        if (playerManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                com.fabiogrossi.dungeonsoundmanager.data.PlayerData pd = playerManager.getPlayer(player);
                if (pd != null && pd.getCurrentPlayingSound() != null) {
                    pd.stopSound(pd.getCurrentPlayingSound());
                }
            }
            playerManager.getPlayerCache().clear();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerManager.addPlayer(player, null);
            }
        }

        reloadConfig();

        if (resourcePackManager != null) {
            resourcePackManager.getSoundDataCache().clear();
            resourcePackManager.getStateDataCache().clear();

            try {
                String tempDir = getConfig().getString("resource_pack_temp_directory", "temp");
                String packName = getConfig().getString("resource_pack", "Skyrim.zip");
                Path tempPath = getDataFolder().toPath().resolve(tempDir);
                Path packPath = getDataFolder().toPath().resolve(packName);

                resourcePackManager.processResourcePack(tempPath, packPath);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Errore critico durante l'estrazione della Resource Pack", e);
                return;
            }
        }

        new SongCarouselBroadcastTask(this).runTaskTimer(this, 0, 20 * 5);
        new StateEvaluatorTask(this).runTaskTimer(this, 0, 20);

        validateConfig();
        getLogger().info("Ricaricamento completato con successo!");
    }

    public void registerCommands(PaperCommandManager paperCommandManager, CommandCompletions<BukkitCommandCompletionContext> completionContext) {
        paperCommandManager.registerCommand(new SoundManagerCommand(this, completionContext));
    }

    @Override
    public void onDisable() {
        // Nessuna logica di spegnimento necessaria al momento
    }
}