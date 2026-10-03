package ckpb1.client.hud.elements;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.ModuleManager;
import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ColorHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Module List (array list): shows every enabled module, sorted by name
 * width, with a soft cyan-to-violet gradient - the classic array list known
 * from LiquidBounce & friends.
 */
public final class ModuleListElement extends HudElement {

    public ModuleListElement() {
        super("Module List", "List of enabled modules (array list)", 6, 40);
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ModuleManager modules = ckpb1.client.CKPB1Client.modules();
        if (modules == null) {
            width = 60;
            height = 12;
            return;
        }
        List<Module> enabled = new ArrayList<>();
        for (Module m : modules.all()) {
            // HUD elements themselves are not listed - that would be noise
            if (m.isEnabled() && m.getCategory() != Category.HUD) {
                enabled.add(m);
            }
        }
        if (enabled.isEmpty()) {
            width = 54;
            height = 11;
            context.drawTextWithShadow(mc.textRenderer, "§7(no modules on)", x, y, 0xFFFFFF);
            return;
        }
        enabled.sort(Comparator.comparingInt(m -> -mc.textRenderer.getWidth(m.getName())));
        width = 10 + enabled.stream().mapToInt(m -> mc.textRenderer.getWidth(m.getName())).max().orElse(40);
        height = enabled.size() * 11;

        int i = 0;
        for (Module m : enabled) {
            float hue = 0.52f + 0.18f * (i / (float) enabled.size());
            int color = java.awt.Color.HSBtoRGB(hue, 0.75f, 1.0f) | 0xFF000000;
            int rowY = y + i * 11;
            context.fill(x, rowY, x + width, rowY + 11, 0x90101420);
            context.fill(x, rowY, x + 2, rowY + 11, color);
            context.drawTextWithShadow(mc.textRenderer, m.getName(), x + 5, rowY + 2, color);
            i++;
        }
        // subtle border around the whole list
        context.drawBorder(x, y, width, height, ColorHelper.Argb.getArgb(120, 70, 200, 255));
    }
}
