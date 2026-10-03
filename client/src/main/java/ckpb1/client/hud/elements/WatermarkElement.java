package ckpb1.client.hud.elements;

import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** CK_PB1 watermark: client name + version + FPS. */
public final class WatermarkElement extends HudElement {

    public WatermarkElement() {
        super("Watermark", "Shows the CK_PB1 client name, version and FPS", 6, 6);
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String main = "§bCK_PB1 §f" + ckpb1.common.CKPB1.VERSION;
        String sub = "§7" + mc.getCurrentFps() + " fps";
        int w1 = mc.textRenderer.getWidth("CK_PB1 " + ckpb1.common.CKPB1.VERSION);
        int w2 = mc.textRenderer.getWidth(mc.getCurrentFps() + " fps");
        width = Math.max(w1, w2) + 6;
        height = 24;
        drawPanel(context, width, height);
        context.drawTextWithShadow(mc.textRenderer, main, x, y + 3, 0xFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, sub, x, y + 14, 0xB0B0B0);
    }
}
