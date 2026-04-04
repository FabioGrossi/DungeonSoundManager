package com.fabiogrossi.dungeonsoundmanager.managers;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.data.SoundData;
import com.fabiogrossi.dungeonsoundmanager.data.StateData;
import com.fabiogrossi.dungeonsoundmanager.utils.SoundUtils;
import com.fabiogrossi.dungeonsoundmanager.utils.ZipUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
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
                        .forEach(file -> {
                            if (!file.delete()) {
                                soundManager.getLogger().warning("Impossibile cancellare il file temporaneo: " + file.getName());
                            }
                        });
            }
        }
        ZipUtils.unzip(resourcePackFile, destinationFolder);

        Path soundsJsonFile = destinationFolder.resolve("assets/minecraft/sounds.json");
        Path resourcePackSoundsFolder = destinationFolder.resolve("assets/minecraft/sounds");

        loadSoundsCache(soundsJsonFile, resourcePackSoundsFolder);
        loadStatesCache();
    }

    private void loadSoundsCache(Path soundsJsonFile, Path resourcePackSoundsFolder) throws IOException {
        if (!Files.exists(soundsJsonFile)) {
            throw new IOException("Resource pack does not contain sounds.json");
        }
        if (!Files.exists(resourcePackSoundsFolder)) {
            throw new IOException("Resource pack does not contain any sound");
        }

        JsonObject soundsJson = new Gson().fromJson(new FileReader(soundsJsonFile.toFile()), JsonObject.class);

        soundsJson.asMap().forEach((internalSoundID, jsonElement) -> {
            JsonObject soundJsonObject = (JsonObject) jsonElement;

            // FIX ANTI-CRASH: Controllo se la categoria esiste. Se non c'è, usa "MASTER"
            String defaultPlayCategory = "MASTER";
            if (soundJsonObject.has("category") && !soundJsonObject.get("category").isJsonNull()) {
                defaultPlayCategory = soundJsonObject.get("category").getAsString().toUpperCase(Locale.ROOT);
            }

            if (!soundJsonObject.has("sounds")) return;

            JsonArray soundFilesList = soundJsonObject.getAsJsonArray("sounds");
            if (soundFilesList.isEmpty() || soundFilesList.size() > 1) return;

            JsonElement firstSound = soundFilesList.get(0);
            String soundFileName = firstSound.isJsonObject() ? firstSound.getAsJsonObject().get("name").getAsString() : firstSound.getAsString();

            Path soundOggFile = resourcePackSoundsFolder.resolve(soundFileName + ".ogg");
            if (!Files.exists(soundOggFile)) {
                soundManager.getLogger().warning(String.format("Sound file %s does not exist", soundOggFile));
                return;
            }
            try {
                long soundDuration = SoundUtils.calculateDurationOgg(soundOggFile.toFile());
                SoundData soundData = new SoundData(internalSoundID, soundDuration, defaultPlayCategory);
                soundDataCache.put(internalSoundID, soundData);
            } catch (IOException e) {
                soundManager.getLogger().severe("Errore nella lettura dell'header ogg: " + e.getMessage());
            }
        });
    }

    private void loadStatesCache() {
        ConfigurationSection soundsConfig = soundManager.getConfig().getConfigurationSection("player_states");
        if (soundsConfig == null) {
            throw new IllegalStateException("Impossibile trovare la sezione player_states nel config.yml");
        }

        soundsConfig.getKeys(false).forEach(stateName -> {
            ConfigurationSection stateConditionSection = soundsConfig.getConfigurationSection(stateName + ".conditions");
            if (stateConditionSection == null) return;

            StateData.StateConditions stateConditions = StateData.StateConditions.from(stateConditionSection);
            boolean assignAutomatically = soundsConfig.getBoolean(stateName + ".assignAutomatically", false);
            int priority = soundsConfig.getInt(stateName + ".priority", 0);

            String stingerConfigName = soundsConfig.getString(stateName + ".outro_stinger");
            SoundData outroStinger = (stingerConfigName != null) ? getSoundData(stingerConfigName.replace("-", ".")) : null;

            ConfigurationSection soundsMap = soundsConfig.getConfigurationSection(stateName + ".sounds");
            if (soundsMap == null) {
                soundManager.getLogger().warning(String.format("Lo stato %s non ha suoni. Viene ignorato.", stateName));
                return;
            }

            Map<SoundData, SoundData.SoundConditions> stateSoundData = new HashMap<>();
            soundsMap.getKeys(false).forEach(soundNameRaw -> {
                String soundName = soundNameRaw.replace("-", ".");
                SoundData selectedSound = getSoundData(soundName);
                if (selectedSound == null) {
                    soundManager.getLogger().warning(String.format("Il suono %s non esiste.", soundName));
                    return;
                }

                SoundData.SoundConditions soundConditions = soundsMap.getSerializable(soundName + ".conditions", SoundData.SoundConditions.class);
                if (soundConditions == null) soundConditions = new SoundData.SoundConditions();

                stateSoundData.put(selectedSound, soundConditions);
            });

            StateData stateData = new StateData(stateName, assignAutomatically, priority, outroStinger, stateConditions, stateSoundData);
            stateDataCache.put(stateName, stateData);
        });
    }

    /**
     * Verifica se un suono è stato caricato con successo dal sounds.json.
     * Questa funzione viene chiamata dal SoundManager durante la validazione.
     */
    public boolean hasSound(String soundName) {
        if (soundDataCache == null || soundDataCache.isEmpty()) return false;
        return soundDataCache.containsKey(soundName);
    }

    public SoundData getSoundData(String soundID) {
        return soundDataCache.get(soundID);
    }

    public StateData getStateData(String stateName) {
        return stateDataCache.get(stateName);
    }
}