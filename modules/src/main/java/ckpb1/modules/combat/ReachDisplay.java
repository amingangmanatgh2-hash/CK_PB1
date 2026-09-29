package ckpb1.modules.combat;

import ckpb1.client.core.Category;
import ckpb1.client.core.CombatState;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Reach Display: shows the distance of your last hit. */
public final class ReachDisplay extends Module {

    public final NumberSetting timeout = add(new NumberSetting("Timeout",
            "Seconds the last reach stays visible", 2, 1, 5, 0.5, "s"));

    public ReachDisplay() {
        super("Reach Display", "Shows the reach of your last attack", Category.COMBAT);
    }

    @Override
    public void onHudRender(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        long age = System.currentTimeMillis() - CombatState.lastAttackTime;
        if (age > (long) (timeout.get() * 1000)) {
            return;
        }
        double reach = CombatState.lastReach;
        // color by how close to typical max reach
        int color = reach < 3 ? 0xFFE0E0 : reach < 3.5 ? 0x90FF90 : 0x55C8FF;
        String text = String.format("%.2fm", reach);
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        context.drawTextWithShadow(mc.textRenderer, text, cx + 12, cy - 14, color);
    }
}
