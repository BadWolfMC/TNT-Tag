package nl.juriantech.tnttag.utils;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

public final class ParticleUtils {

    private ParticleUtils() {
    }

    /** Spawns one modern celebration firework instead of iterating through air blocks and creating duplicates. */
    public static void firework(Location location, int power) {
        World world = location.getWorld();
        if (world == null) return;

        world.spawn(location.clone().add(0.0, 0.25, 0.0), Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.clearEffects();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BURST)
                    .withColor(Color.RED, Color.ORANGE)
                    .withFade(Color.YELLOW)
                    .trail(true)
                    .flicker(true)
                    .build());
            meta.setPower(Math.max(0, Math.min(power, 4)));
            firework.setFireworkMeta(meta);
        });
    }
}
