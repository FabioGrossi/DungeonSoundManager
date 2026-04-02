package com.fabiogrossi.dungeonsoundmanager.data;

import com.fabiogrossi.dungeonsoundmanager.utils.WorldUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class SoundData {

    private final String id;
    private final long duration; // Milliseconds
    private final String defaultPlayCategory;

    @Getter
    @Setter
    public static class SoundConditions implements ConfigurationSerializable {

        private boolean nightRequired = false;
        private boolean dayRequired = false;
        private Biome biome = null;
        private boolean undergroundRequired = false;
        private String playState = null;

        public boolean areVerified(PlayerData playerData) {
            Player player = playerData.getPlayer();
            World world = player.getWorld();

            if (nightRequired && (WorldUtils.isDay(world))) {
                return false;
            }
            if (dayRequired && (!WorldUtils.isDay(world))) {
                return false;
            }
            // FIX: Ora controlla se il bioma esiste prima di fare l'equals!
            if (biome != null && !player.getLocation().getBlock().getBiome().equals(biome)) {
                return false;
            }
            // FIX: Ora controlla la variabile undergroundRequired!
            if (undergroundRequired && player.getLocation().getBlock().getY() > 70 && player.getLocation().getBlock().getLightFromSky() > 1) {
                return false;
            }
            // FIX: Ora controlla se lo stato richiesto non è nullo!
            if (playState != null && (playerData.getCurrentPlayState() == null || !playerData.getCurrentPlayState().getName().equalsIgnoreCase(playState))) {
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
        return "SoundData{" +
                "id='" + id + '\'' +
                ", duration=" + duration +
                ", defaultPlayCategory='" + defaultPlayCategory + '\'' +
                '}';
    }
}