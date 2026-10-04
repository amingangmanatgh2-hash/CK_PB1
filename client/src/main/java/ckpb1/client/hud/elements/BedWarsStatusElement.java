package ckpb1.client.hud.elements;

import ckpb1.client.hud.HudElement;
import ckpb1.modules.automation.BedWarsAutomation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** BedWars match status HUD element (driven by BedWars Automation). */
public final class BedWarsStatusElement extends HudElement {

    public BedWarsStatusElement() {
        super("BedWars Status", "BedWars Automation match status (stage, resources, targets)", 250, 120);
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String[] lines = BedWarsAutomation.statusLines();
        if (lines == null || lines.length == 0) {
            return;
        }
        int widest = 60;
        for (String line : lines) {
            widest = Math.max(widest, mc.textRenderer.getWidth(line));
        }
        width = widest + 6;
        height = lines.length * 11 + 4;
        drawPanel(context, width, height);
        int ly = 2;
        for (String line : lines) {
            text(context, line, 3, ly, 0xFFE8C8);
            ly += 11;
        }
    }
}
