package ckpb1.client.hud.elements;

import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/** Armor status HUD element: worn armor + main hand with durability. */
public final class ArmorStatusElement extends HudElement {

    public ArmorStatusElement() {
        super("Armor Status", "Worn armor and held item with durability", 6, 220);
        width = 22;
        height = 5 * 20;
    }

    private void slot(DrawContext context, ItemStack stack, int y) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (stack == null || stack.isEmpty()) {
            context.fill(x, y, x + 16, y + 16, 0x50202028);
            return;
        }
        context.drawItem(stack, x, y);
        // durability bar
        if (stack.isDamageable()) {
            int max = stack.getMaxDamage();
            int dmg = stack.getDamage();
            float pct = 1.0f - (float) dmg / max;
            int w = (int) (16 * pct);
            int color = pct > 0.5f ? 0xFF55FF55 : pct > 0.2f ? 0xFFFFE055 : 0xFFFF5555;
            context.fill(x, y + 17, x + 16, y + 18, 0xFF202020);
            context.fill(x, y + 17, x + w, y + 18, color);
            String pctText = Math.round(pct * 100) + "%";
            context.drawTextWithShadow(mc.textRenderer, pctText, x + 19, y + 5, 0xE0E0E0);
        }
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        var inventory = mc.player.getInventory();
        // armor: index 0 = boots ... 3 = helmet -> render helmet first
        slot(context, inventory.armor.get(3), y);
        slot(context, inventory.armor.get(2), y + 20);
        slot(context, inventory.armor.get(1), y + 40);
        slot(context, inventory.armor.get(0), y + 60);
        slot(context, mc.player.getMainHandStack(), y + 80);
    }
}
