package nl.juriantech.tnttag.gui.menu;

import java.util.Map;

/**
 * Named menu slots kept separate from menu behavior so layouts can later be loaded from configuration.
 */
public record MenuLayout(int rows, Map<String, Integer> slots) {

    public MenuLayout {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Menu rows must be between 1 and 6.");
        }
        slots = Map.copyOf(slots);
        int size = rows * 9;
        for (Map.Entry<String, Integer> entry : slots.entrySet()) {
            if (entry.getValue() < 0 || entry.getValue() >= size) {
                throw new IllegalArgumentException("Menu slot '" + entry.getKey() + "' is outside the inventory.");
            }
        }
    }

    public int size() {
        return rows * 9;
    }

    public int slot(String name) {
        Integer slot = slots.get(name);
        if (slot == null) {
            throw new IllegalArgumentException("Unknown menu slot: " + name);
        }
        return slot;
    }
}
