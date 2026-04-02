package com.fabiogrossi.dungeonsoundmanager.managers;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.SoundData;
import com.fabiogrossi.dungeonsoundmanager.data.StateData;
import com.fabiogrossi.dungeonsoundmanager.utils.SoundUtils;
import com.fabiogrossi.dungeonsoundmanager.utils.ZipUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class ResourcePackManager {

    private JsonObject soundsJson;
    private final SoundManager soundManager;

    @Getter
    private final Map<String, SoundData> soundDataCache = new HashMap<>();

    @Getter
    private final Map<String, StateData> stateDataCache = new HashMap<>();

    public ResourcePackManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public void processResourcePack(Path destinationFolder, Path resourcePackFile) throws IOException {
        if (Files.exists(destinationFolder)) {
            try (Stream<Path> walk = Files.walk(destinationFolder)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
        ZipUtils.unzip(resourcePackFile, destinationFolder);
        Path soundsJsonFile = destinationFolder.resolve("assets/minecraft/sounds.json");
        if (!Files.exists(soundsJsonFile)) {
            throw new IOException("Resource pack does not contain sounds.json");
        }
        soundsJson = new Gson().fromJson(new FileReader(soundsJsonFile.toFile()), JsonObject.class);
        Path resourcePackSoundsFolder = destinationFolder.resolve("assets/minecraft/sounds");
        if (!Files.exists(resourcePackSoundsFolder)) {
            throw new IOException("Resource pack does not contain any sound");
        }
        soundsJson.asMap().forEach((internalSoundID, jsonElement) -> {
            JsonObject soundJsonObject = (JsonObject) jsonElement;
            String defaultPlayCategory = soundJsonObject.get("category").getAsString().toUpperCase(Locale.ROOT);
            JsonArray soundFilesList = soundJsonObject.getAsJsonArray("sounds");
            if (soundFilesList.size() > 1) {
                return;
            }
            Path soundOggFile = resourcePackSoundsFolder.resolve(soundFilesList.get(0).getAsString() + ".ogg");
            if (!Files.exists(soundOggFile)) {
                soundManager.getLogger().warning("Sound file " + soundOggFile + " does not exist");
                return;
            }
            try {
                long soundDuration = SoundUtils.calculateDurationOgg(soundOggFile.toFile());
                SoundData soundData = new SoundData(internalSoundID, soundDuration, defaultPlayCategory);
                soundDataCache.put(internalSoundID, soundData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        ConfigurationSection soundsConfig = soundManager.getConfig().getConfigurationSection("player_states");
        if (soundsConfig == null) {
            throw new RuntimeException("Unable to find a player_states configuration section. Please regenerate the config file");
        }
        soundsConfig.getKeys(false).forEach(stateName -> {
            ConfigurationSection stateConditionSection = soundsConfig.getConfigurationSection(stateName + ".conditions");
            if (stateConditionSection == null) return;

            StateData.StateConditions stateConditions = StateData.StateConditions.from(stateConditionSection);
            boolean assignAutomatically = soundsConfig.getBoolean(stateName + ".assignAutomatically", false);
            int priority = soundsConfig.getInt(stateName + ".priority", 0);

            // LETTURA DELL'OUTRO STINGER
            String stingerConfigName = soundsConfig.getString(stateName + ".outro_stinger");
            SoundData outroStinger = null;
            if (stingerConfigName != null) {
                outroStinger = getSoundData(stingerConfigName.replace("-", "."));
            }

            ConfigurationSection soundsMap = soundsConfig.getConfigurationSection(stateName + ".sounds");
            if (soundsMap == null) {
                soundManager.getLogger().warning("The state "+ stateName + " has no sounds section. Ignoring it");
                return;
            }

            Map<SoundData, SoundData.SoundConditions> stateSoundData = new HashMap<>();
            soundsMap.getKeys(false).forEach(soundNameRaw -> {
                String soundName = soundNameRaw.replace("-", ".");
                SoundData selectedSound = getSoundData(soundName);
                if (selectedSound == null) {
                    soundManager.getLogger().warning("Sound " + soundName + " doesn't exist");
                    return;
                }

                SoundData.SoundConditions soundConditions = soundsMap.getSerializable(soundName + ".conditions", SoundData.SoundConditions.class);
                if (soundConditions == null) {
                    soundConditions = new SoundData.SoundConditions();
                }
                stateSoundData.put(selectedSound, soundConditions);
            });

            // Inserito l'outroStinger nel costruttore
            StateData stateData = new StateData(stateName, assignAutomatically, priority, outroStinger, stateConditions, stateSoundData);
            stateDataCache.put(stateName, stateData);
        });
    }

    public SoundData getSoundData(String soundID) {
        return soundDataCache.get(soundID);
    }

    public StateData getStateData(String stateName) {
        return stateDataCache.get(stateName);
    }
}