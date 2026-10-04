package ckpb1.modules.movement;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Movement Assistant: sprint/jump helpers, edge safety stop and an optional
 * velocity boost for singleplayer / private test environments.
 */
public final class MovementAssistant extends Module {

    public final BoolSetting autoSprint = add(new BoolSetting("Auto Sprint",
            "Always sprint while moving forward", true));
    public final BoolSetting autoJump = add(new BoolSetting("Auto Jump",
            "Jump automatically when blocked by a wall", false));
    public final BoolSetting stopAtEdge = add(new BoolSetting("Stop At Edge",
            "Stop moving when about to fall off an edge (safe walk)", false));
    public final NumberSetting boost = add(new NumberSetting("Speed Boost",
            "Movement velocity multiplier (1.0 = vanilla). Test environments only",
            1.0, 1.0, 2.0, 0.05, "x"));

    public MovementAssistant() {
        super("Movement Assistant", "Sprint/jump/safe-walk helpers and speed boost (test environments)",
                Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        var player = mc.player;
        if (autoSprint.isOn() && player.input.movementForward > 0 && !player.isSneaking()) {
            player.setSprinting(true);
        }
        if (autoJump.isOn() && player.horizontalCollision && player.isOnGround()
                && player.input.movementForward > 0) {
            player.input.jumping = true;
        }
        if (stopAtEdge.isOn() && player.isOnGround()) {
            Vec3d vel = player.getVelocity();
            if (Math.abs(vel.x) > 0.08 || Math.abs(vel.z) > 0.08) {
                Vec3d ahead = player.getPos().add(vel.x * 3, -1, vel.z * 3);
                if (mc.world.getBlockState(player.getBlockPos().add(
                        (int) Math.floor(ahead.x - player.getX()),
                        -1,
                        (int) Math.floor(ahead.z - player.getZ()))).isAir()) {
                    player.setVelocity(vel.x * 0.2, vel.y, vel.z * 0.2);
                }
            }
        }
        if (boost.get() > 1.01 && player.isOnGround()
                && (Math.abs(player.input.movementForward) > 0 || Math.abs(player.input.movementSideways) > 0)) {
            Vec3d vel = player.getVelocity();
            double scale = boost.get() - 1.0;
            player.setVelocity(vel.x + vel.x * scale * 0.25, vel.y, vel.z + vel.z * scale * 0.25);
        }
    }
}
