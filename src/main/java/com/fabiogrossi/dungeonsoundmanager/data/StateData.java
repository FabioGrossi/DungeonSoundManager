package com.fabiogrossi.dungeonsoundmanager.data;

import com.fabiogrossi.dungeonsoundmanager.SoundManager;
import com.fabiogrossi.dungeonsoundmanager.utils.WorldUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
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
    private StateConditions stateConditions;
    private Map<SoundData, SoundData.SoundConditions> sounds;

    @Getter
    @Setter
    public static class StateConditions implements ConfigurationSerializable {

        private boolean nightRequired = false;
        private boolean dayRequired = false;
        private Biome biome = null;
        private boolean undergroundRequired = false;
        private String playState = null;

        public static StateConditions from(ConfigurationSection configurationSection) {
            StateConditions conditions = new StateConditions();
            if (configurationSection.getBoolean("nightRequired")) {
                conditions.setNightRequired(configurationSection.getBoolean("nightRequired"));
            }
            if (configurationSection.getBoolean("dayRequired")) {
                conditions.setDayRequired(configurationSection.getBoolean("dayRequired"));
            }
            if (configurationSection.getString("biome") != null) {
                conditions.setBiome(Biome.valueOf(configurationSection.getString("biome")));
            }
            if (configurationSection.getBoolean("undergroundRequired")) {
                conditions.setUndergroundRequired(configurationSection.getBoolean("undergroundRequired"));
            }
            if (configurationSection.getString("playState") != null) {
                conditions.setPlayState(configurationSection.getString("playState"));
            }
            return conditions;
        }

        public boolean areVerified(PlayerData playerData) {
            Player player = playerData.getPlayer();
            World world = player.getWorld();
            if (nightRequired && (WorldUtils.isDay(world))) {
                System.out.println("false night");
                    return false;
            }
            if (dayRequired && (!WorldUtils.isDay(world))) {
                System.out.println("false day");
                    return false;
            }
            if (biome != null && !player.getLocation().getBlock().getBiome().equals(biome)) {
                System.out.println("false biome");
                return false;
            }
            if (undergroundRequired && player.getLocation().getBlock().getY() > 70 && player.getLocation().getBlock().getLightFromSky() > 1) {
                System.out.println("false undergr");
                return false;
            }
            if (playState != null && !playerData.getCurrentPlayState().getName().equalsIgnoreCase(playState)) {
                System.out.println("false state");
                return false;
            }
            return true;
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> serialized = new HashMap<>();
            serialized.put("nightRequired", nightRequired);
            serialized.put("dayRequired", dayRequired);
            serialized.put("biome", biome);
            serialized.put("undergroundRequired", undergroundRequired);
            serialized.put("playState", playState);
            return serialized;
        }
    }

    @Override
    public String toString() {
        return "StateData{" +
                "name='" + name + '\'' +
                ", assignAutomatically=" + assignAutomatically +
                ", stateConditions=" + stateConditions +
                ", sounds=" + sounds +
                '}';
    }
}
