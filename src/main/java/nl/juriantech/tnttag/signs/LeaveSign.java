package nl.juriantech.tnttag.signs;

import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.objects.SimpleLocation;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class LeaveSign implements SignInterface {

    private final Location loc;
    private final List<String> signLines;

    public LeaveSign(Location loc) {
        this.loc = loc;
        this.signLines = Tnttag.customizationfile.getStringList("leave-sign.lines");
    }

    @Override
    public void onClick(Player player) {
        player.performCommand("tnttag leave");
    }

    @Override
    public void update() {
        if (loc == null || !(loc.getBlock().getState() instanceof Sign sign) || signLines.size() < 4) return;
        for (int i = 0; i < 4; i++) {
            sign.getSide(Side.FRONT).line(i, ChatUtils.component(signLines.get(i)));
        }
        sign.update(true);
    }

    @Override
    public String toString() {
        return SimpleLocation.fromLocation(loc).toString();
    }

    public static LeaveSign fromString(String str) {
        Location location = Objects.requireNonNull(SimpleLocation.fromString(str)).toLocation();
        return new LeaveSign(location);
    }

    @Override
    public Location getLoc() {
        return loc;
    }
}
