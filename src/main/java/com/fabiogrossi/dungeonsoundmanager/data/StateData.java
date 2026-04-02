package com.fabiogrossi.dungeonsoundmanager.data;

import com.fabiogrossi.dungeonsoundmanager.utils.WorldUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class StateData {

    private final String name;
    private final boolean assignAutomatically;
    private final int priority;
    private final SoundData outroStinger;
    private StateConditions stateConditions;
    private Map<SoundData, SoundData.SoundConditions> sounds;

    @Getter
    @Setter
    public static class StateConditions implements ConfigurationSerializable {

        private boolean nightRequired = false;
        private boolean dayRequired = false;
        private boolean stormRequired = false;
        private boolean rainRequired = false;
        private boolean clearWeatherRequired = false;
        private boolean inWaterRequired = false;
        private boolean sneakingRequired = false;
        private Integer minY = null;
        private Integer maxY = null;
        private Double minHealth = null;
        private Double maxHealth = null;
        private String worldName = null;
        private Biome biome = null;
        private boolean undergroundRequired = false;
        private String playState = null;

        public static StateConditions from(ConfigurationSection section) {
            StateConditions conditions = new StateConditions();
            if (section.contains("nightRequired")) conditions.setNightRequired(section.getBoolean("nightRequired"));
            if (section.contains("dayRequired")) conditions.setDayRequired(section.getBoolean("dayRequired"));
            if (section.contains("stormRequired")) conditions.setStormRequired(section.getBoolean("stormRequired"));
            if (section.contains("rainRequired")) conditions.setRainRequired(section.getBoolean("rainRequired"));
            if (section.contains("clearWeatherRequired")) conditions.setClearWeatherRequired(section.getBoolean("clearWeatherRequired"));
            if (section.contains("inWaterRequired")) conditions.setInWaterRequired(section.getBoolean("inWaterRequired"));
            if (section.contains("sneakingRequired")) conditions.setSneakingRequired(section.getBoolean("sneakingRequired"));
            if (section.contains("minY")) conditions.setMinY(section.getInt("minY"));
            if (section.contains("maxY")) conditions.setMaxY(section.getInt("maxY"));
            if (section.contains("minHealth")) conditions.setMinHealth(section.getDouble("minHealth"));
            if (section.contains("maxHealth")) conditions.setMaxHealth(section.getDouble("maxHealth"));
            if (section.getString("worldName") != null) conditions.setWorldName(section.getString("worldName"));
            if (section.getString("biome") != null) conditions.setBiome(Biome.valueOf(section.getString("biome")));
            if (section.contains("undergroundRequired")) conditions.setUndergroundRequired(section.getBoolean("undergroundRequired"));
            if (section.getString("playState") != null) conditions.setPlayState(section.getString("playState"));
            return conditions;
        }

        public boolean areVerified(PlayerData playerData) {
            Player p = playerData.getPlayer();
            World w = p.getWorld();

            if (nightRequired && WorldUtils.isDay(w)) return false;
            if (dayRequired && !WorldUtils.isDay(w)) return false;
            if (stormRequired && !w.hasStorm()) return false;
            if (rainRequired && (!w.hasStorm() || !w.isThundering())) return false;
            if (clearWeatherRequired && w.hasStorm()) return false;
            if (inWaterRequired && !p.isInWater()) return false;
            if (sneakingRequired && !p.isSneaking()) return false;
            if (minY != null && p.getLocation().getY() < minY) return false;
            if (maxY != null && p.getLocation().getY() > maxY) return false;
            if (minHealth != null && p.getHealth() < minHealth) return false;
            if (maxHealth != null && p.getHealth() > maxHealth) return false;
            if (worldName != null && !w.getName().equalsIgnoreCase(worldName)) return false;
            if (biome != null && !p.getLocation().getBlock().getBiome().equals(biome)) return false;
            if (undergroundRequired && p.getLocation().getBlock().getY() > 70 && p.getLocation().getBlock().getLightFromSky() > 1) return false;
            if (playState != null && (playerData.getCurrentPlayState() == null || !playerData.getCurrentPlayState().getName().equalsIgnoreCase(playState))) return false;

            return true;
        }

        // Metodo per calcolare quante condizioni specifiche sono richieste
        public int getSpecificConditionCount() {
            int count = 0;
            if (nightRequired) count++;
            if (dayRequired) count++;
            if (stormRequired) count++;
            if (rainRequired) count++;
            if (clearWeatherRequired) count++;
            if (inWaterRequired) count++;
            if (sneakingRequired) count++;
            if (minY != null) count++;
            if (maxY != null) count++;
            if (minHealth != null) count++;
            if (maxHealth != null) count++;
            if (worldName != null) count++;
            if (biome != null) count++;
            if (undergroundRequired) count++;
            if (playState != null) count++;
            return count;
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> serialized = new HashMap<>();
            // La serializzazione standard (opzionale ometterla se usiamo il from manuale)
            return serialized;
        }
    }
}