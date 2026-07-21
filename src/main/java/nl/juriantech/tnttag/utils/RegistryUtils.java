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
        Material material = Registry.MATERIAL.get(key(configuredValue));
        if (material == null) {
            throw new IllegalArgumentException("Unknown material: " + configuredValue);
        }
        return material;
    }

    public static Sound sound(String configuredValue) {
        if (configuredValue == null || configuredValue.isBlank()) {
            throw new IllegalArgumentException("Registry value cannot be blank");
        }

        String normalized = configuredValue.trim().toLowerCase(Locale.ROOT);
        NamespacedKey directKey = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : normalized.contains(".") ? NamespacedKey.minecraft(normalized) : null;
        Sound sound = directKey == null ? null : Registry.SOUND_EVENT.get(directKey);

        if (sound == null) {
            // Preserve compatibility with the former enum-style values. Dots in a sound key became
            // underscores in enum constants, while underscores already present in key segments stayed
            // underscores, so blindly replacing every underscore with a dot is not reversible.
            String legacyName = normalized.startsWith("minecraft:")
                    ? normalized.substring("minecraft:".length())
                    : normalized;
            NamespacedKey legacyKey = Registry.SOUND_EVENT.keyStream()
                    .filter(key -> key.getNamespace().equals(NamespacedKey.MINECRAFT))
                    .filter(key -> key.getKey().replace('.', '_').equals(legacyName))
                    .findFirst()
                    .orElse(null);
            sound = legacyKey == null ? null : Registry.SOUND_EVENT.get(legacyKey);
        }

        if (sound == null) {
            throw new IllegalArgumentException("Unknown sound: " + configuredValue);
        }
        return sound;
    }

    public static PotionEffectType potionEffect(String configuredValue) {
        PotionEffectType effect = Registry.MOB_EFFECT.get(key(configuredValue));
        if (effect == null) {
            throw new IllegalArgumentException("Unknown potion effect: " + configuredValue);
        }
        return effect;
    }

    private static NamespacedKey key(String configuredValue) {
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
        normalized = namespace + ":" + value;

        NamespacedKey key = NamespacedKey.fromString(normalized);
        if (key == null) {
            throw new IllegalArgumentException("Invalid registry key: " + configuredValue);
        }
        return key;
    }
}
