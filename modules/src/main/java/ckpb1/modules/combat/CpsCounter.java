package ckpb1.modules.combat;

import ckpb1.client.core.Category;
import ckpb1.client.core.CpsTracker;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** CPS Counter: shows left/right clicks per second near the crosshair. */
public final class CpsCounter extends Module {

    public CpsCounter() {
        super("CPS Counter", "Live clicks-per-second readout next to the crosshair", Category.COMBAT);
    }

    @Override
    public void onHudRender(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        String text = "§7CPS §f" + CpsTracker.leftCps() + "§7/§f" + CpsTracker.rightCps();
        int w = mc.textRenderer.getWidth(text);
        context.drawTextWithShadow(mc.textRenderer, text, cx + 12, cy + 6, 0xFFFFFF);
    }
}
