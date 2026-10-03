package ckpb1.client.hud.elements;

import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** FPS HUD element. */
public final class FpsElement extends HudElement {

    public FpsElement() {
        super("FPS", "Current frames per second", 6, 36);
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int fps = mc.getCurrentFps();
        String text = "FPS: " + fps;
        width = mc.textRenderer.getWidth(text) + 6;
        height = 12;
        drawPanel(context, width, height);
        int color = fps >= 120 ? 0x55FF55 : fps >= 60 ? 0xFFE055 : 0xFF5555;
        text(context, text, 3, 2, color);
    }
}
