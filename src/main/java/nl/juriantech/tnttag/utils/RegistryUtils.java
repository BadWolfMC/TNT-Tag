package nl.juriantech.tnttag.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

public final class RegistryUtils {

    private RegistryUtils() {
    }

    public static Material material(String configuredValue) {
        Material material = Registry.MATERIAL.get(key(configuredValue, false));
        if (material == null) {
            throw new IllegalArgumentException("Unknown material: " + configuredValue);
        }
        return material;
    }

    public static Sound sound(String configuredValue) {
        Sound sound = Registry.SOUND_EVENT.get(key(configuredValue, true));
        if (sound == null) {
            throw new IllegalArgumentException("Unknown sound: " + configuredValue);
        }
        return sound;
    }

    public static PotionEffectType potionEffect(String configuredValue) {
        PotionEffectType effect = Registry.MOB_EFFECT.get(key(configuredValue, false));
        if (effect == null) {
            throw new IllegalArgumentException("Unknown potion effect: " + configuredValue);
        }
        return effect;
    }

    private static NamespacedKey key(String configuredValue, boolean sound) {
        if (configuredValue == null || configuredValue.isBlank()) {
            throw new IllegalArgumentException("Registry value cannot be blank");
        }

        String normalized = configuredValue.trim().toLowerCase(Locale.ROOT);
        String namespace = "minecraft";
        String value = normalized;
        int separator = normalized.indexOf(':');
        if (separator >= 0) {
            namespace = normalized.substring(0, separator);
            value = normalized.substring(separator + 1);
        }
        if (sound) value = value.replace('_', '.');
        normalized = namespace + ":" + value;

        NamespacedKey key = NamespacedKey.fromString(normalized);
        if (key == null) {
            throw new IllegalArgumentException("Invalid registry key: " + configuredValue);
        }
        return key;
    }
}
