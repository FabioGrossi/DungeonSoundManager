package com.fabiogrossi.dungeonsoundmanager.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import co.aikar.commands.annotation.*;
import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.PlayerData;
import com.fabiogrossi.dungeonsoundmanager.data.SoundData;
import com.fabiogrossi.dungeonsoundmanager.data.StateData;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Arrays;

@CommandAlias("sm")
@CommandPermission("dungeonsoundmanager.admin")
public class SoundManagerCommand extends BaseCommand {

    private final SoundManager soundManager;

    public SoundManagerCommand(SoundManager soundManager, CommandCompletions<BukkitCommandCompletionContext> completionContext) {
        this.soundManager = soundManager;
        registerCompletions(completionContext);
    }

    private void registerCompletions(CommandCompletions<BukkitCommandCompletionContext> completionContext) {
        completionContext.registerAsyncCompletion("sound_ids", completion ->
                soundManager.getResourcePackManager().getSoundDataCache().keySet());
        completionContext.registerAsyncCompletion("sound_categories", completion ->
                Arrays.stream(SoundCategory.values()).map(Enum::name).toList());
        completionContext.registerAsyncCompletion("sound_states", completion ->
                soundManager.getResourcePackManager().getStateDataCache().keySet());
    }

    @Subcommand("reload")
    public void onReload(CommandSender sender) {
        sender.sendMessage("§e[DungeonSound] Ricaricamento globale in corso...");
        soundManager.reloadEntirePlugin();
        sender.sendMessage("§a[DungeonSound] Ricaricamento completato!");
    }

    @Subcommand("play")
    @Syntax("<player> <sound> <category> <volume> <pitch>")
    @CommandCompletion("@players @sound_ids @sound_categories")
    public void onPlaySound(CommandSender sender, String targetPlayerName, String soundName, String soundCategoryName, float volume, float pitch) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("Player non trovato!");
            return;
        }
        SoundData soundData = soundManager.getResourcePackManager().getSoundData(soundName);
        if (soundData == null) {
            sender.sendMessage("Suono non trovato");
            return;
        }
        PlayerData playerData = soundManager.getPlayerManager().getPlayer(targetPlayer);
        playerData.playSound(soundData, volume, pitch);
    }

    @Subcommand("stop")
    @Syntax("<player> <sound/category>")
    @CommandCompletion("@players @sound_categories")
    public void onStopSound(CommandSender sender, String targetPlayerName, @Nullable String arg) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("Player non trovato!");
            return;
        }
        PlayerData playerData = soundManager.getPlayerManager().getPlayer(targetPlayer);
        if (arg == null) {
            playerData.stopAllSounds();
            return;
        }
        if (Arrays.stream(SoundCategory.values()).map(Enum::name).toList().contains(arg)) {
            playerData.stopSound(SoundCategory.valueOf(arg));
            return;
        }
        String soundName = arg.replace("-", ".");
        if (soundManager.getResourcePackManager().getSoundDataCache().containsKey(soundName)) {
            playerData.stopSound(soundManager.getResourcePackManager().getSoundData(soundName));
        }
    }

    @Subcommand("setstate")
    @Syntax("<player> <state>")
    @CommandCompletion("@players @sound_states")
    public void onSetState(CommandSender sender, String targetPlayerName, String state) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("Player non trovato!");
            return;
        }
        if (soundManager.getResourcePackManager().getStateDataCache().containsKey(state)) {
            StateData stateData = soundManager.getResourcePackManager().getStateData(state);
            PlayerData playerData = soundManager.getPlayerManager().getPlayer(targetPlayer);

            // FIX: Usa forcedState, altrimenti l'evaluator automatico lo sovrascrive subito
            playerData.setForcedState(stateData);
            sender.sendMessage("§aStato musicale forzato a " + state + " per " + targetPlayerName);
        } else {
            sender.sendMessage("§cStato non trovato!");
        }
    }

    @Subcommand("resetstate")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void onResetState(CommandSender sender, String targetPlayerName) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("Player non trovato!");
            return;
        }
        PlayerData playerData = soundManager.getPlayerManager().getPlayer(targetPlayer);
        playerData.setForcedState(null);
        sender.sendMessage("§aForzatura musicale rimossa per " + targetPlayerName + ". Ora è in modalità Automatica/Dinamica.");
    }
}