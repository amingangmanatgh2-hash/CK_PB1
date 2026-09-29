package ckpb1.modules.util;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto Clicker: simulated left clicks with randomized CPS. Modes: Always, or
 * only while the attack button is held. Test environments.
 */
public final class AutoClicker extends Module {

    public final NumberSetting cpsMin = add(new NumberSetting("CPS Min",
            "Minimum clicks per second", 8, 1, 20, 1));
    public final NumberSetting cpsMax = add(new NumberSetting("CPS Max",
            "Maximum clicks per second", 12, 1, 20, 1));
    public final ModeSetting mode = add(new ModeSetting("Mode",
            "Always clicks, or only while you hold the attack button",
            "Hold Attack", "Always"));
    public final BoolSetting requireTarget = add(new BoolSetting("Require Target",
            "Only click when the crosshair is over an entity", false));

    private long nextClickAt;

    public AutoClicker() {
        super("Auto Clicker", "Clicks automatically with randomized CPS (test environments)", Category.UTILITY);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }
        if (mode.is("Hold Attack") && !mc.options.attackKey.isPressed()) {
            return;
        }
        if (requireTarget.isOn()) {
            if (!(mc.crosshairTarget instanceof Entity hitEntity) || !(hitEntity instanceof net.minecraft.entity.LivingEntity)) {
                return;
            }
        }
        long now = System.currentTimeMillis();
        if (now < nextClickAt) {
            return;
        }
        int lo = Math.max(1, Math.min(cpsMin.getInt(), cpsMax.getInt()));
        int hi = Math.max(lo, cpsMax.getInt());
        double cps = ThreadLocalRandom.current().nextDouble(lo, hi + 0.01);
        nextClickAt = now + (long) (1000.0 / cps);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (mc.crosshairTarget instanceof Entity hitEntity) {
            mc.interactionManager.attackEntity(mc.player, hitEntity);
        }
    }
}
