package nl.juriantech.tnttag.objects;

import nl.juriantech.tnttag.Tnttag;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public void setWins(int wins) {
        Tnttag.playerdatafile.set(uuid + ".wins", wins);
        markDirty();
    }

    public Integer getWins() {
        Integer wins = Tnttag.playerdatafile.getInt(uuid + ".wins");
        return wins == null ? 0 : wins;
    }

    public void setTimesTagged(int timesTagged) {
        Tnttag.playerdatafile.set(uuid + ".timestagged", timesTagged);
        markDirty();
    }

    public Integer getTimesTagged() {
        Integer timesTagged = Tnttag.playerdatafile.getInt(uuid + ".timestagged");
        return timesTagged == null ? 0 : timesTagged;
    }

    public void setTags(int tags) {
        Tnttag.playerdatafile.set(uuid + ".tags", tags);
        markDirty();
    }

    public Integer getTags() {
        Integer tags = Tnttag.playerdatafile.getInt(uuid + ".tags");
        return tags == null ? 0 : tags;
    }

    public void setWinstreak(int winstreak) {
        Tnttag.playerdatafile.set(uuid + ".winstreak", winstreak);
        markDirty();
    }

    public Integer getWinstreak() {
        Integer winstreak = Tnttag.playerdatafile.getInt(uuid + ".winstreak");
        return winstreak == null ? 0 : winstreak;
    }

    private void markDirty() {
        Tnttag plugin = Tnttag.getInstance();
        if (plugin != null && plugin.getPlayerDataManager() != null) {
            plugin.getPlayerDataManager().markDirty();
        }
    }
}
