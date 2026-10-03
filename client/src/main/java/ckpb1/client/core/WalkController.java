package ckpb1.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Drives the local player along a waypoint path by overriding the vanilla
 * movement input (client-side input, works in singleplayer and vanilla
 * private test servers). Used by Bed Destroyer / Resource Assistant /
 * BedWars Automation.
 */
public final class WalkController {

    private List<BlockPos> path = List.of();
    private int index;
    private final String owner;

    public WalkController(String owner) {
        this.owner = owner;
    }

    public void setPath(List<BlockPos> waypoints) {
        this.path = waypoints == null ? List.of() : waypoints;
        this.index = 0;
    }

    public void stop() {
        this.path = List.of();
        this.index = 0;
    }

    public boolean hasPath() {
        return index < path.size();
    }

    public BlockPos currentWaypoint() {
        return hasPath() ? path.get(index) : null;
    }

    /** Remaining waypoints (for path preview rendering). */
    public List<BlockPos> remainingPath() {
        if (!hasPath()) {
            return List.of();
        }
        return path.subList(index, path.size());
    }

    /** Steers the player towards the current waypoint each tick. */
    public void tick(MinecraftClient mc, float speed, boolean allowJump) {
        if (!hasPath() || mc.player == null) {
            return;
        }
        ClientPlayerEntity player = mc.player;
        BlockPos wp = path.get(index);
        Vec3d target = Vec3d.ofCenter(wp).add(0, 0.5, 0);
        Vec3d feet = player.getPos();

        // advance waypoint when reached horizontally
        double dx = target.x - feet.x;
        double dz = target.z - feet.z;
        if (dx * dx + dz * dz < 0.8 * 0.8 && Math.abs(wp.getY() + 1 - feet.y) < 1.6) {
            index++;
            if (!hasPath()) {
                return;
            }
            wp = path.get(index);
            target = Vec3d.ofCenter(wp).add(0, 0.5, 0);
            dx = target.x - feet.x;
            dz = target.z - feet.z;
        }

        // steer yaw towards waypoint
        float desiredYaw = (float) (MathHelper.atan2(dz, dx) * 57.2957763671875) - 90.0f;
        player.setYaw(MathHelper.wrapDegrees(desiredYaw));

        // walk forward (with optional speed scaling)
        player.input.movementForward = 1.0f * MathHelper.clamp(speed, 0.2f, 1.0f);
        player.input.movementSideways = 0;

        // jump when the next block is higher or we are stuck against a block
        if (allowJump && (wp.getY() > player.getBlockPos().getY() || player.horizontalCollision)
                && player.isOnGround()) {
            player.input.jumping = true;
        }
    }

    public String owner() {
        return owner;
    }
}
