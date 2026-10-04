package ckpb1.client.core;

/** CK_PB1 module categories. CK_PB1 itself is the client name, never a category. */
public enum Category {
    COMBAT("Combat", "PvP helpers for private/test environments"),
    MOVEMENT("Movement", "Movement helpers"),
    PLAYER("Player", "Player & god-mode style helpers (singleplayer / test)"),
    RENDER("Render", "Rendering & visual modules"),
    WORLD("World", "World scanning & helpers"),
    UTILITY("Utility", "General utilities"),
    BEDWARS("BedWars", "BedWars helpers for test environments"),
    AUTOMATION("Automation", "Automated routines for test environments"),
    HUD("HUD", "HUD elements - drag them in the HUD editor");

    public final String label;
    public final String hint;

    Category(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }
}
