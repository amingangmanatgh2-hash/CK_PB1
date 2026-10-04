package ckpb1.modules.combat;

import ckpb1.client.core.Category;
import ckpb1.client.core.CombatState;
import ckpb1.client.core.Module;
import ckpb1.client.core.RotationController;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;

/**
 * Combat Assistant: attacks the target chosen by Target Selector within a
 * configured range and CPS. Intended for singleplayer and private test
 * servers where such helpers are allowed.
 */
public final class CombatAssistant extends Module {

    private final TargetSelector selector;
    private final AttackTiming timing = new AttackTiming();

    public final NumberSetting range = add(new NumberSetting("Range",
            "Attack range in blocks", 4.0, 1.0, 6.0, 0.1, "m"));
    public final NumberSetting cpsMin = add(new NumberSetting("CPS Min",
            "Minimum clicks per second", 8, 1, 20, 1));
    public final NumberSetting cpsMax = add(new NumberSetting("CPS Max",
            "Maximum clicks per second", 12, 1, 20, 1));
    public final BoolSetting rotate = add(new BoolSetting("Rotate",
            "Smoothly aim at the target", true));
    public final NumberSetting rotationSpeed = add(new NumberSetting("Rotation Speed",
            "Degrees per tick while rotating", 30, 5, 90, 5));
    public final BoolSetting onlyOnClick = add(new BoolSetting("Only While Attacking",
            "Attack only while the attack button is held", false));

    public CombatAssistant(TargetSelector selector) {
        super("Combat Assistant", "Automatic attacks on the selected target (test environments)",
                Category.COMBAT);
        this.selector = selector;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) {
            return;
        }
        if (onlyOnClick.isOn() && !mc.options.attackKey.isPressed()) {
            return;
        }
        Entity target = selector.findTarget(mc, range.get());
        if (target == null) {
            CombatState.targetLastSeen = System.currentTimeMillis();
            return;
        }
        double dist = mc.player.distanceTo(target);
        if (dist > range.get()) {
            return;
        }
        if (rotate.isOn()) {
            RotationController.lookAt(target, rotationSpeed.get().floatValue());
        }
        publishTarget(target, dist);
        if (timing.ready()) {
            timing.spend(cpsMin.getInt(), cpsMax.getInt());
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    static void publishTarget(Entity target, double dist) {
        CombatState.targetName = target.getName().getString();
        CombatState.targetDistance = dist;
        CombatState.targetLastSeen = System.currentTimeMillis();
        if (target instanceof net.minecraft.entity.LivingEntity living) {
            CombatState.targetHealth = living.getHealth();
            CombatState.targetMaxHealth = living.getMaxHealth();
        }
    }
}
