package ckpb1.modules.combat;

import ckpb1.client.core.Category;
import ckpb1.client.core.CombatState;
import ckpb1.client.core.Module;
import ckpb1.client.core.RotationController;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Legit Aura: 1.8.9-style smooth kill aura for private/test environments.
 *
 * <p>Humanized behaviour: eased rotations with per-tick jitter, gaussian CPS
 * with occasional micro-pauses, an FOV cone, a wall check and an attack gate
 * that only swings once the crosshair is actually close to the target hitbox,
 * so the movement looks like natural aiming instead of snapping.</p>
 */
public final class LegitAura extends Module {

    private final AttackTiming timing = new AttackTiming();
    private Entity currentTarget;
    private long switchAllowedAt;
    private long pauseUntil;

    public final NumberSetting range = add(new NumberSetting("Range",
            "Attack range in blocks (1.8 style is ~3.0)", 3.0, 2.0, 4.5, 0.05, "m"));
    public final NumberSetting cpsMin = add(new NumberSetting("CPS Min",
            "Minimum clicks per second", 9, 4, 20, 1));
    public final NumberSetting cpsMax = add(new NumberSetting("CPS Max",
            "Maximum clicks per second", 13, 4, 20, 1));
    public final NumberSetting rotSpeed = add(new NumberSetting("Rotation Speed",
            "Max degrees turned per tick (lower = smoother)", 22, 5, 60, 1));
    public final NumberSetting fov = add(new NumberSetting("FOV",
            "Only aim at targets inside this view cone", 120, 30, 180, 5, "\u00b0"));
    public final NumberSetting crosshairAngle = add(new NumberSetting("Swing Angle",
            "Only swing when the crosshair is this close to the target", 15, 3, 60, 1, "\u00b0"));
    public final NumberSetting switchDelay = add(new NumberSetting("Switch Delay",
            "Milliseconds before locking a new target", 350, 0, 1500, 50, "ms"));
    public final BoolSetting players = add(new BoolSetting("Players",
            "Attack players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs",
            "Attack hostile mobs", false));
    public final BoolSetting wallCheck = add(new BoolSetting("Wall Check",
            "Never attack through walls", true));
    public final BoolSetting humanize = add(new BoolSetting("Humanize",
            "Micro-pauses and jitter so aiming looks natural", true));

    public LegitAura() {
        super("Legit Aura", "1.8.9-style smooth kill aura with humanized rotations (test environments)",
                Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
    }

    /** True when the entity passes the wall check from the player's eyes. */
    private boolean visible(MinecraftClient mc, Entity target) {
        if (!wallCheck.isOn()) {
            return true;
        }
        Vec3d eye = mc.player.getEyePos();
        Box box = target.getBoundingBox();
        Vec3d center = box.getCenter();
        // sample eyes/chest/feet so partially exposed targets still count
        Vec3d[] points = {
                center,
                new Vec3d(box.minX, box.minY + box.getLengthY() * 0.15, box.minZ).add(
                        (box.maxX - box.minX) / 2, 0, (box.maxZ - box.minZ) / 2)
        };
        for (Vec3d to : points) {
            BlockHitResult hit = mc.world.raycast(new RaycastContext(eye, to,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    private boolean validTarget(MinecraftClient mc, Entity entity) {
        if (entity == mc.player || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        boolean isPlayer = entity instanceof PlayerEntity;
        if (isPlayer && !players.isOn()) {
            return false;
        }
        if (!isPlayer && !mobs.isOn()) {
            return false;
        }
        double dist = mc.player.distanceTo(entity);
        if (dist > range.get()) {
            return false;
        }
        if (RotationController.angleTo(entity.getPos().add(0, entity.getHeight() * 0.85, 0)) > fov.get() / 2.0) {
            return false;
        }
        return visible(mc, entity);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) {
            return;
        }

        // keep or drop the current target
        if (currentTarget != null && !validTarget(mc, currentTarget)) {
            currentTarget = null;
        }

        // pick the closest valid entity (stable switching, not target jitter)
        if (currentTarget == null) {
            Entity best = null;
            double bestDist = Double.MAX_VALUE;
            for (Entity entity : mc.world.getEntities()) {
                if (!validTarget(mc, entity)) {
                    continue;
                }
                double dist = mc.player.distanceTo(entity);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = entity;
                }
            }
            if (best != null && best != currentTarget) {
                long now = System.currentTimeMillis();
                if (now >= switchAllowedAt) {
                    currentTarget = best;
                    switchAllowedAt = now + (long) switchDelay.get();
                }
            }
        }

        if (currentTarget == null) {
            return;
        }
        Entity target = currentTarget;

        // smooth rotation towards the upper chest with per-tick jitter
        double jitter = humanize.isOn() ? ThreadLocalRandom.current().nextDouble(0.75, 1.3) : 1.0;
        float step = (float) Math.max(3.0, rotSpeed.get() * jitter);
        Vec3d aimPoint = target.getPos().add(0, target.getHeight() * 0.75, 0);
        RotationController.lookAt(aimPoint, step);

        // occasional human micro-pause (skips swinging for a few ms)
        if (humanize.isOn() && ThreadLocalRandom.current().nextInt(400) == 0) {
            pauseUntil = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(120, 420);
        }
        if (System.currentTimeMillis() < pauseUntil) {
            return;
        }

        double dist = mc.player.distanceTo(target);
        CombatAssistant.publishTarget(target, dist);

        // swing gate: only when the crosshair actually points at the target
        if (RotationController.angleTo(aimPoint) > crosshairAngle.get()) {
            return;
        }
        if (!timing.ready()) {
            return;
        }
        timing.spend(cpsMin.getInt(), cpsMax.getInt());
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
