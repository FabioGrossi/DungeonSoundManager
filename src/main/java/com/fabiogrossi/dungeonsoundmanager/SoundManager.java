package com.fabiogrossi.dungeonsoundmanager;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.ProtocolManager;
import com.fabiogrossi.dungeonsoundmanager.command.SoundManagerCommand;
import com.fabiogrossi.dungeonsoundmanager.managers.EventManager;
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

@Getter
public class SoundManager extends JavaPlugin {

    private ResourcePackManager resourcePackManager;
    private ProtocolManager protocolManager;
    private PlayerManager playerManager;

    @Override
    public void onEnable() {
        try {
            // Salva il config preservando i commenti senza mai sovrascriverlo brutalmente
            saveDefaultConfig();

            resourcePackManager = new ResourcePackManager(this);
            resourcePackManager.processResourcePack(
                    getDataFolder().toPath().resolve(getConfig().getString("resource_pack_temp_directory")),
                    getDataFolder().toPath().resolve(getConfig().getString("resource_pack"))
            );

            // RIMOSSO IL SAVE_CONFIG() DISTRUTTIVO CHE CANCELLAVA I COMMENTI

            PaperCommandManager paperCommandManager = new PaperCommandManager(this);
            CommandCompletions<BukkitCommandCompletionContext> completionContext = paperCommandManager.getCommandCompletions();
            registerCommands(paperCommandManager, completionContext);

            Bukkit.getPluginManager().registerEvents(new EventManager(this), this);

            playerManager = new PlayerManager(this);

            new SongCarouselBroadcastTask(this).runTaskTimer(this, 0, 20 * 5);
            new StateEvaluatorTask(this).runTaskTimer(this, 0, 20);

            // Avvia la validazione per avvisarti di errori
            validateConfig();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void validateConfig() {
        getLogger().info("[VALIDAZIONE] Controllo integrità configurazioni...");
        ConfigurationSection mobStates = getConfig().getConfigurationSection("mob_states");
        if (mobStates != null) {
            for (String mobName : mobStates.getKeys(false)) {
                if (MythicBukkit.inst().getMobManager().getMythicMob(mobName).isEmpty()) {
                    getLogger().warning("[ATTENZIONE] Il mob '" + mobName + "' in config.yml non esiste in MythicMobs! La musica non partirà.");
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
        }

        try {
            resourcePackManager.processResourcePack(
                    getDataFolder().toPath().resolve(getConfig().getString("resource_pack_temp_directory")),
                    getDataFolder().toPath().resolve(getConfig().getString("resource_pack"))
            );
        } catch (IOException e) {
            getLogger().severe("Errore critico durante l'estrazione della Resource Pack: " + e.getMessage());
            e.printStackTrace();
            return;
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
    public void onDisable() {}
}