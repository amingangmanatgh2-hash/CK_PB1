package ckpb1.modules.combat;

import ckpb1.client.core.Category;
import ckpb1.client.core.CombatState;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Combo Information: consecutive hits on a target without taking damage. */
public final class ComboInformation extends Module {

    public ComboInformation() {
        super("Combo Information", "Shows your current hit combo near the crosshair", Category.COMBAT);
    }

    @Override
    public void onHudRender(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        int combo = CombatState.combo();
        if (combo < 2) {
            return;
        }
        String text = combo + " combo";
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int w = mc.textRenderer.getWidth(text);
        context.drawTextWithShadow(mc.textRenderer, "§e" + text, cx - w / 2, cy + 22, 0xFFFFFF);
    }
}
