package ckpb1.client.hud.elements;

import ckpb1.client.core.CombatState;
import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Target HUD: the current target of Kill Farm / Combat Assistant with a
 * health bar, distance and combo counter.
 */
public final class TargetHudElement extends HudElement {

    public TargetHudElement() {
        super("Target HUD", "Current combat target (Kill Farm / Combat Assistant)", 250, 40);
        width = 140;
        height = 44;
    }

    @Override
    public void render(DrawContext context) {
        if (!CombatState.hasTarget()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = CombatState.targetName;
        float health = CombatState.targetHealth;
        float max = Math.max(1f, CombatState.targetMaxHealth);
        double dist = CombatState.targetDistance;

        width = 140;
        height = 44;
        drawPanel(context, width, height);
        context.drawTextWithShadow(mc.textRenderer, "§bTarget", x, y + 3, 0xFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, name, x, y + 14, 0xFFE0E0);
        // health bar
        float pct = Math.max(0f, Math.min(1f, health / max));
        int barW = width - 6;
        context.fill(x + 3, y + 25, x + 3 + barW, y + 31, 0xFF202028);
        int hpColor = pct > 0.5f ? 0xFF55FF55 : pct > 0.25f ? 0xFFFFE055 : 0xFFFF5555;
        context.fill(x + 3, y + 25, x + 3 + (int) (barW * pct), y + 31, hpColor);
        String info = (int) Math.ceil(health) + "/" + (int) Math.ceil(max) + " HP   "
                + String.format("%.1fm", dist) + "   Combo " + CombatState.combo();
        context.drawTextWithShadow(mc.textRenderer, info, x + 3, y + 34, 0xC8C8C8);
    }
}
