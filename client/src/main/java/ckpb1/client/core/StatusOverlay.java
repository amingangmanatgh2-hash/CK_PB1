package ckpb1.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.ColorHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-center status panels that CK_PB1 modules (Bed Destroyer, BedWars
 * Automation, Kill Farm, ...) push their live status into. The HUD manager
 * renders the collected sections once per frame.
 */
public final class StatusOverlay {

    private static final Map<String, List<String>> SECTIONS = new LinkedHashMap<>();

    private StatusOverlay() {
    }

    /** Publishes (or clears, when lines is empty) the status lines of a module. */
    public static void set(String key, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            SECTIONS.remove(key);
        } else {
            SECTIONS.put(key, new ArrayList<>(lines));
        }
    }

    public static void clear(String key) {
        SECTIONS.remove(key);
    }

    public static void clearAll() {
        SECTIONS.clear();
    }

    /** Renders all active sections centered at the top of the screen. */
    public static void render(DrawContext context) {
        if (SECTIONS.isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow().getScaledWidth();
        int y = 6;
        int panelWidth = 150;
        int x = screenWidth / 2 - panelWidth / 2;
        for (Map.Entry<String, List<String>> e : SECTIONS.entrySet()) {
            int lines = e.getValue().size();
            int h = 12 + lines * 10;
            // panel
            context.fill(x, y, x + panelWidth, y + h, 0x90101020);
            context.drawBorder(x, y, panelWidth, h, ColorHelper.Argb.getArgb(200, 60, 190, 255));
            // title (section key)
            context.drawTextWithShadow(mc.textRenderer, "§b" + e.getKey(), x + 5, y + 3, 0xFFFFFF);
            int ly = y + 13;
            for (String line : e.getValue()) {
                context.drawTextWithShadow(mc.textRenderer, line, x + 5, ly, 0xE0E0E0);
                ly += 10;
            }
            y += h + 4;
            if (y > 120) {
                break; // never flood the screen
            }
        }
    }
}
