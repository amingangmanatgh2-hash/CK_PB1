package ckpb1.modules.bedwars;

import ckpb1.client.core.Category;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.Module;
import ckpb1.client.core.PathFinder;
import ckpb1.client.core.StatusOverlay;
import ckpb1.client.core.WalkController;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import ckpb1.client.render.OverlayRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Bed Destroyer V1: detects (enemy) beds, analyses their defense, plans a
 * path, walks to the bed and breaks it, showing live status on the HUD.
 * Test environments (singleplayer / private BedWars test servers).
 */
public final class BedDestroyerV1 extends Module {

    public final NumberSetting scanRadius = add(new NumberSetting("Scan Radius",
            "Bed detection radius in blocks", 48, 8, 96, 4, "m"));
    public final ModeSetting targetMode = add(new ModeSetting("Target Mode",
            "Which bed is attacked first", "Nearest", "Least Defended"));
    public final BoolSetting skipOwnBed = add(new BoolSetting("Protect Own Bed",
            "Ignore beds near your spawn/join position", true));
    public final NumberSetting ownBedRadius = add(new NumberSetting("Own Bed Radius",
            "Blocks around your join position treated as your base", 12, 4, 40, 1, "m"));
    public final NumberSetting attackRange = add(new NumberSetting("Attack Range",
            "Distance to start breaking the bed", 4.5, 2, 6, 0.1, "m"));
    public final BoolSetting renderTarget = add(new BoolSetting("Render Target",
            "Highlight the target bed", true));
    public final BoolSetting renderPath = add(new BoolSetting("Render Path",
            "Draw the planned path", true));
    public final BoolSetting autoDisable = add(new BoolSetting("Auto Disable",
            "Disable the module when the bed is destroyed", true));

    private enum State {
        SCANNING, PLANNING, MOVING, BREAKING, DONE
    }

    private State state = State.SCANNING;
    private BedScanner.BedInfo target;
    private final WalkController walker = new WalkController("Bed Destroyer V1");
    private BlockPos joinPos;
    private int rescanTimer;

    public BedDestroyerV1() {
        super("Bed Destroyer V1", "Detects enemy beds, walks to them and breaks them (test environments)",
                Category.BEDWARS);
    }

    @Override
    protected void onEnable() {
        state = State.SCANNING;
        target = null;
        walker.stop();
        rescanTimer = 0;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            joinPos = mc.player.getBlockPos();
        }
    }

    @Override
    protected void onDisable() {
        walker.stop();
        StatusOverlay.clear("Bed Destroyer");
    }

    private void chooseTarget(MinecraftClient mc) {
        ClientWorld world = mc.world;
        BlockPos center = mc.player.getBlockPos();
        List<BedScanner.BedInfo> beds = BedScanner.scanBeds(world, center,
                scanRadius.getInt(), 12, 20);
        BedScanner.BedInfo best = null;
        double bestScore = Double.MAX_VALUE;
        for (BedScanner.BedInfo bed : beds) {
            if (skipOwnBed.isOn() && joinPos != null
                    && bed.head.getSquaredDistance(joinPos) < ownBedRadius.get() * ownBedRadius.get()) {
                continue;
            }
            double score = targetMode.is("Least Defended")
                    ? bed.defense * 10 + bed.distance
                    : bed.distance;
            if (score < bestScore) {
                bestScore = score;
                best = bed;
            }
        }
        target = best;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }
        // validate current target
        if (target != null) {
            var bedState = mc.world.getBlockState(target.head);
            if (!(bedState.getBlock() instanceof net.minecraft.block.BedBlock)) {
                // destroyed!
                ChatUtil.message("§aBed Destroyer: target bed destroyed");
                state = State.DONE;
                StatusOverlay.clear("Bed Destroyer");
                if (autoDisable.isOn()) {
                    setEnabled(false, false);
                }
                return;
            }
            target.distance = Math.sqrt(mc.player.getBlockPos().getSquaredDistance(target.head));
        }

        if (state == State.DONE) {
            return;
        }

        if (--rescanTimer <= 0 || target == null) {
            rescanTimer = 40;
            chooseTarget(mc);
            if (target == null) {
                state = State.SCANNING;
                StatusOverlay.set("Bed Destroyer", List.of("§7scanning for beds..."));
                return;
            }
            state = State.PLANNING;
        }

        double dist = Math.sqrt(mc.player.getBlockPos().getSquaredDistance(target.head));
        BlockPos stand = BedScanner.standPosition(mc.world, target.head);

        switch (state) {
            case PLANNING -> {
                if (stand == null) {
                    StatusOverlay.set("Bed Destroyer", List.of(
                            "target: " + target, "§cno reachable standing spot"));
                    state = State.MOVING; // try walking as close as possible
                    return;
                }
                PathFinder.Path path = PathFinder.findPath(mc.world, mc.player.getBlockPos(), stand, 2500);
                walker.setPath(path.waypoints);
                state = State.MOVING;
            }
            case MOVING -> {
                boolean inRange = dist <= attackRange.get() && hasLineOfSight(mc, target.head);
                if (inRange) {
                    walker.stop();
                    mc.player.input.movementForward = 0;
                    state = State.BREAKING;
                } else {
                    walker.tick(mc, 1.0f, true);
                    // re-plan when stuck
                    if (!walker.hasPath() && stand != null) {
                        PathFinder.Path path = PathFinder.findPath(mc.world, mc.player.getBlockPos(), stand, 2500);
                        walker.setPath(path.waypoints);
                    }
                }
            }
            case BREAKING -> {
                if (dist > attackRange.get() + 1) {
                    state = State.PLANNING;
                    return;
                }
                breakBed(mc, target.head);
            }
            default -> {
            }
        }

        List<String> status = new ArrayList<>();
        status.add("state: §f" + state);
        if (target != null) {
            status.add("bed: §f" + target.head.toShortString() + " §7(" + (int) dist + "m)");
            status.add("defense: §f" + defenseLabel(target.defense));
        }
        StatusOverlay.set("Bed Destroyer", status);
    }

    private static String defenseLabel(int level) {
        return switch (level) {
            case 0 -> "open";
            case 1 -> "light";
            case 2 -> "medium";
            default -> "heavy";
        };
    }

    private boolean hasLineOfSight(MinecraftClient mc, BlockPos bed) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(bed);
        var hit = mc.world.raycast(new net.minecraft.util.math.RaycastContext(eye, center,
                net.minecraft.util.math.RaycastContext.ShapeType.OUTLINE,
                net.minecraft.util.math.RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS
                || (hit.getType() == HitResult.Type.BLOCK
                && ((BlockHitResult) hit).getBlockPos().equals(bed)
                || ((BlockHitResult) hit).getBlockPos().equals(bed.down()));
    }

    private void breakBed(MinecraftClient mc, BlockPos bed) {
        Direction side = BedScanner.faceTowards(bed, mc.player.getBlockPos());
        // aim at the bed
        ckpb1.client.core.RotationController.lookAt(Vec3d.ofCenter(bed), 40f);
        if (!mc.interactionManager.attackBlock(bed, side)) {
            mc.interactionManager.updateBlockBreakingProgress(bed, side);
        }
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, Camera camera) {
        if (target == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            return;
        }
        if (renderTarget.isOn()) {
            OverlayRenderer.box(matrices, camera,
                    new net.minecraft.util.math.Box(target.head).expand(0.05), 0xFF40C8FF);
            OverlayRenderer.box(matrices, camera,
                    new net.minecraft.util.math.Box(target.foot).expand(0.05), 0x8000E0FF);
        }
        if (renderPath.isOn() && walker.hasPath()) {
            List<Vec3d> points = new ArrayList<>();
            points.add(mc.player.getPos());
            for (BlockPos wp : walker.remainingPath()) {
                points.add(Vec3d.ofCenter(wp).add(0, 0.5, 0));
            }
            OverlayRenderer.path(matrices, camera, points, 0x80FFE055);
        }
    }
}
