package ckpb1.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Smooth client-side rotation helper used by combat/bedwars modules
 * (private test environments).
 */
public final class RotationController {

    private RotationController() {
    }

    /** Rotates the player towards a world position. */
    public static void lookAt(Vec3d targetPos, float maxStepDegrees) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        Vec3d eye = mc.player.getEyePos();
        double dx = targetPos.x - eye.x;
        double dy = targetPos.y - eye.y;
        double dz = targetPos.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (MathHelper.atan2(dz, dx) * 57.2957763671875) - 90.0f;
        float targetPitch = (float) (-(MathHelper.atan2(dy, horizontal) * 57.2957763671875));

        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        float yawDelta = MathHelper.wrapDegrees(targetYaw - yaw);
        float pitchDelta = MathHelper.wrapDegrees(targetPitch - pitch);

        float clamped = Math.min(maxStepDegrees, Math.abs(yawDelta));
        yaw += Math.signum(yawDelta) * clamped;
        float clampedPitch = Math.min(maxStepDegrees, Math.abs(pitchDelta));
        pitch += Math.signum(pitchDelta) * clampedPitch;

        mc.player.setYaw(yaw);
        mc.player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
    }

    /** Instantly aims at an entity (feet-to-head center). */
    public static void lookAt(Entity target, float maxStepDegrees) {
        lookAt(target.getPos().add(0, target.getHeight() * 0.85, 0), maxStepDegrees);
    }

    /** Angle (degrees) between the player look direction and a world position. */
    public static float angleTo(Vec3d targetPos) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return 999;
        }
        Vec3d eye = mc.player.getEyePos();
        double dx = targetPos.x - eye.x;
        double dy = targetPos.y - eye.y;
        double dz = targetPos.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (MathHelper.atan2(dz, dx) * 57.2957763671875) - 90.0f;
        float targetPitch = (float) (-(MathHelper.atan2(dy, horizontal) * 57.2957763671875));
        float yawDelta = Math.abs(MathHelper.wrapDegrees(targetYaw - mc.player.getYaw()));
        float pitchDelta = Math.abs(MathHelper.wrapDegrees(targetPitch - mc.player.getPitch()));
        return Math.max(yawDelta, pitchDelta);
    }
}
