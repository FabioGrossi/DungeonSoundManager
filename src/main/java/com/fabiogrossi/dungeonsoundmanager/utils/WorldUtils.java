package com.fabiogrossi.dungeonsoundmanager.utils;

import org.bukkit.World;

public class WorldUtils {

    public static boolean isDay(World world) {
        long time = world.getTime();
        return time < 12300 || time > 23850;
    }

}
