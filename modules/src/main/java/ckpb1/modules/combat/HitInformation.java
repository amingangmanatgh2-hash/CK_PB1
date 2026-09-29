package ckpb1.modules.combat;

import ckpb1.client.core.Category;
import ckpb1.client.core.CombatState;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;

import java.util.Collection;

/**
 * Hit Information: panel with details of the last hit (target, estimated
 * damage, remaining health). Damage is an estimate based on the held item
 * and the target's armor points.
 */
public final class HitInformation extends Module {

    public HitInformation() {
        super("Hit Information", "Last hit details: target, estimated damage, health", Category.COMBAT);
    }

    private double estimateDamage(MinecraftClient mc, LivingEntity target) {
        double base = 1.0; // fist
        ItemStack held = mc.player.getMainHandStack();
        if (!held.isEmpty()) {
            var modifiers = held.getAttributeModifiers(EquipmentSlot.MAINHAND)
                    .get(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            for (EntityAttributeModifier mod : modifiers) {
                base += mod.getValue();
            }
        }
        // vanilla-style armor reduction (approximation, ignores enchants)
        double armor = target.getArmor();
        double reduction = Math.min(0.8, armor * 0.03);
        return base * (1.0 - reduction);
    }

    @Override
    public void onHudRender(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        long age = System.currentTimeMillis() - CombatState.lastAttackTime;
        if (age > 4000 || CombatState.lastHitTarget.isEmpty()) {
            return;
        }
        String target = CombatState.lastHitTarget;
        float hp = CombatState.lastHitTargetHealth;
        LivingEntity entity = CombatState.lastHitEntity;
        String dmg = entity != null && entity.isAlive()
                ? String.format("  ~%.1f dmg", estimateDamage(mc, entity))
                : "";
        String lines = target + "  " + (int) Math.ceil(hp) + " HP" + dmg;
        int y = mc.getWindow().getScaledHeight() / 2 + 20;
        int cx = mc.getWindow().getScaledWidth() / 2;
        int w = mc.textRenderer.getWidth(lines) + 8;
        context.fill(cx - w / 2, y, cx + w / 2, y + 14, 0x80101020);
        context.drawBorder(cx - w / 2, y, w, 14, 0x5040C8FF);
        context.drawTextWithShadow(mc.textRenderer, "§f" + lines, cx - w / 2 + 4, y + 3, 0xFFFFFF);
    }
}
