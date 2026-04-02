package com.fabiogrossi.dungeonsoundmanager.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import co.aikar.commands.annotation.*;
import com.comphenix.protocol.ProtocolManager;
import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.PlayerData;
import com.fabiogrossi.dungeonsoundmanager.data.SoundData;
import com.fabiogrossi.dungeonsoundmanager.data.StateData;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Arrays;

@CommandAlias("sm")
@CommandPermission("dungeonsoundmanager.playsound")
public class SoundManagerCommand extends BaseCommand {

    private final SoundManager soundManager;

    private final ProtocolManager protocolManager;

    public SoundManagerCommand(SoundManager soundManager, CommandCompletions<BukkitCommandCompletionContext> completionContext) {
        this.soundManager = soundManager;
        this.protocolManager = soundManager.getProtocolManager();
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

    @Subcommand("play")
    @Syntax("<player> <sound> <category> <volume> <pitch>")
    @CommandCompletion("@players @sound_ids @sound_categories")
    public void onPlaySound(Player sender, String targetPlayerName, String soundName, String soundCategoryName, float volume, float pitch) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("Player not found!");
            return;
        }
        SoundData soundData = soundManager.getResourcePackManager().getSoundData(soundName);
        if (soundData == null) {
            sender.sendMessage("Sound not found");
            return;
        }
        PlayerData playerData = soundManager.getPlayerManager().getPlayer(targetPlayer);
        playerData.playSound(soundData, volume, pitch);
    }

    @Subcommand("stop")
    @Syntax("<player> <sound/category>")
    @CommandCompletion("@players @sound_categories")
    public void onStopSound(Player sender, String targetPlayerName, @Nullable String arg) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("Player not found!");
            return;
        }
        PlayerData playerData = soundManager.getPlayerManager().getPlayer(targetPlayer);
        if (arg == null) {
            playerData.stopAllSounds();
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
    public void onSetState(Player sender, String targetPlayerName, String state) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            sender.sendMessage("Player not found!");
            return;
        }
        if (state == null) {
            sender.sendMessage("State not specified");
            return;
        }
        if (soundManager.getResourcePackManager().getStateDataCache().containsKey(state)) {
            StateData stateData = soundManager.getResourcePackManager().getStateDataCache().get(state);
            if (stateData == null) {
                sender.sendMessage("State not found");
                return;
            }
            PlayerData playerData = soundManager.getPlayerManager().getPlayer(targetPlayer);
            playerData.setCurrentPlayState(stateData);
        }
    }

}
