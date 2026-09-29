package ckpb1.modules.combat;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.RotationController;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Auto Fireball Defense: detects incoming fireballs and hits them back
 * (deflection). Built for BedWars test environments.
 */
public final class AutoFireballDefense extends Module {

    public final NumberSetting range = add(new NumberSetting("Range",
            "Detection/deflection range in blocks", 8, 2, 16, 0.5, "m"));
    public final NumberSetting cooldown = add(new NumberSetting("Cooldown",
            "Milliseconds between deflection attempts", 300, 100, 1500, 50, "ms"));
    public final BoolSetting onlyIncoming = add(new BoolSetting("Only Incoming",
            "Only react to fireballs flying towards you", true));
    public final BoolSetting rotate = add(new BoolSetting("Rotate",
            "Aim at the fireball before hitting it", true));

    private long nextAttemptAt;

    public AutoFireballDefense() {
        super("Auto Fireball Defense", "Hits incoming fireballs to deflect them (test environments)",
                Category.COMBAT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextAttemptAt) {
            return;
        }
        Vec3d eye = mc.player.getEyePos();
        ExplosiveProjectileEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ExplosiveProjectileEntity fireball)) {
                continue;
            }
            double dist = mc.player.distanceTo(entity);
            if (dist > range.get() || dist >= bestDist) {
                continue;
            }
            if (onlyIncoming.isOn()) {
                Vec3d toPlayer = eye.subtract(fireball.getPos()).normalize();
                Vec3d dir = fireball.getVelocity().normalize();
                if (dir.dotProduct(toPlayer) < 0.7) {
                    continue; // not heading for us
                }
            }
            best = fireball;
            bestDist = dist;
        }
        if (best == null) {
            return;
        }
        if (rotate.isOn()) {
            RotationController.lookAt(best.getPos(), 60f);
        }
        nextAttemptAt = now + cooldown.get().longValue();
        mc.interactionManager.attackEntity(mc.player, best);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
