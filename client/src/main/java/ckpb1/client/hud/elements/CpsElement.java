package ckpb1.client.hud.elements;

import ckpb1.client.core.CpsTracker;
import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** CPS (clicks per second) HUD element, left/right mouse buttons. */
public final class CpsElement extends HudElement {

    public CpsElement() {
        super("CPS", "Left/right clicks per second", 6, 68);
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String text = "CPS: " + CpsTracker.leftCps() + " | " + CpsTracker.rightCps();
        width = mc.textRenderer.getWidth(text) + 6;
        height = 12;
        drawPanel(context, width, height);
        text(context, text, 3, 2, 0x55C8FF);
    }
}
