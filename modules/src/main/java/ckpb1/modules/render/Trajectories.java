package ckpb1.modules.render;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.render.OverlayRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Trajectories: predicts and draws the flight path of the projectile you are
 * holding (bow, snowball, egg, ender pearl, splash potion, XP bottle) with a
 * landing marker (simplified Wurst "Trajectories").
 */
public final class Trajectories extends Module {

    private static final int PATH_COLOR = 0x9040C8FF;
    private static final int LAND_COLOR = 0x90FF5060;

    public Trajectories() {
        super("Trajectories", "Predicts the flight path of your held projectile", Category.RENDER);
    }

    private static final class Shot {
        final Vec3d velocity;
        final double gravity;
        final double drag;

        Shot(Vec3d velocity, double gravity, double drag) {
            this.velocity = velocity;
            this.gravity = gravity;
            this.drag = drag;
        }
    }

    /** Returns the simulated launch parameters for the held item, or null. */
    private Shot shot(MinecraftClient mc) {
        ItemStack stack = mc.player.getMainHandStack();
        Item item = stack.getItem();
        Vec3d direction = mc.player.getRotationVec(1.0F);

        if (item == Items.BOW) {
            float progress;
            if (mc.player.isUsingItem()) {
                int usedTicks = 72000 - mc.player.getItemUseTimeLeft();
                progress = net.minecraft.item.BowItem.getPullProgress(Math.min(20, usedTicks));
            } else {
                progress = 1.0f; // preview a fully drawn bow
            }
            return new Shot(direction.multiply(progress * 3.0), 0.05, 0.99);
        }
        if (item == Items.SNOWBALL || item == Items.EGG || item == Items.ENDER_PEARL
                || item == Items.EXPERIENCE_BOTTLE) {
            return new Shot(direction.multiply(1.5), 0.03, 0.99);
        }
        if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            return new Shot(direction.multiply(0.5), 0.05, 0.99);
        }
        return null;
    }

    @Override
    public void onWorldRender(MatrixStack matrices, Camera camera) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        Shot shot = shot(mc);
        if (shot == null) {
            return;
        }

        Vec3d pos = mc.player.getEyePos();
        Vec3d vel = shot.velocity;
        List<Vec3d> points = new ArrayList<>();
        points.add(pos);
        BlockPos.Mutable landing = null;

        for (int step = 0; step < 300; step++) {
            vel = new Vec3d(vel.x * shot.drag, vel.y * shot.drag - shot.gravity, vel.z * shot.drag);
            Vec3d next = pos.add(vel);
            // stop at the first solid block on the segment
            BlockHitResult hit = mc.world.raycast(new RaycastContext(pos, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                points.add(hit.getPos());
                landing = hit.getBlockPos().toMutable();
                break;
            }
            pos = next;
            points.add(pos);
            if (pos.y < mc.world.getBottomY() - 8) {
                break;
            }
        }

        OverlayRenderer.path(matrices, camera, points, PATH_COLOR);
        if (landing != null) {
            OverlayRenderer.box(matrices, camera, new Box(landing), LAND_COLOR);
        }
    }
}
