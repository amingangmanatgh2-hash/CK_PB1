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
import net.minecraft.block.BedBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bed Destroyer V2 (advanced): scans all beds, shows them in a selectable
 * list (`.ck beds`), supports priority modes, target lock, path planning,
 * defense detection and - for test environments only - teleport & instant
 * destroy.
 */
public final class BedDestroyerV2 extends Module {

    public final NumberSetting scanRadius = add(new NumberSetting("Scan Radius",
            "Bed detection radius in blocks", 64, 8, 128, 4, "m"));
    public final ModeSetting priority = add(new ModeSetting("Priority",
            "Bed ordering used by the auto selector", "Distance", "Defense", "Manual"));
    public final BoolSetting targetLock = add(new BoolSetting("Target Lock",
            "Keep the selected bed until it is destroyed", true));
    public final NumberSetting attackRange = add(new NumberSetting("Attack Range",
            "Distance to start breaking the bed", 4.5, 2, 6, 0.1, "m"));
    public final BoolSetting renderBeds = add(new BoolSetting("Render Beds",
            "Highlight all detected beds", true));
    public final BoolSetting renderPath = add(new BoolSetting("Render Path",
            "Draw the planned path to the selected bed", true));
    public final BoolSetting testTeleport = add(new BoolSetting("Test Teleport",
            "TEST ONLY: teleport to the selected bed when activated", false));
    public final BoolSetting testInstantDestroy = add(new BoolSetting("Test Instant Destroy",
            "TEST ONLY (singleplayer): instantly remove the selected bed", false));

    private final List<BedScanner.BedInfo> beds = new ArrayList<>();
    private BedScanner.BedInfo selected;
    private final WalkController walker = new WalkController("Bed Destroyer V2");
    private int rescanTimer;

    public BedDestroyerV2() {
        super("Bed Destroyer V2",
                "Advanced bed targeting: bed list, priority, target lock, path planning, test teleport/instant destroy",
                Category.BEDWARS);
    }

    public List<BedScanner.BedInfo> beds() {
        return beds;
    }

    public BedScanner.BedInfo selectedBed() {
        return selected;
    }

    /** Selects a bed from the list screen (manual selection). */
    public void selectBed(BedScanner.BedInfo bed) {
        this.selected = bed;
        ChatUtil.message("§7Bed Destroyer V2 target: §f" + bed);
    }

    /** Test-environment action from the bed list screen: teleport to the bed. */
    public void requestTestTeleport(BedScanner.BedInfo bed) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || bed == null) {
            return;
        }
        ChatUtil.message("§etest teleport to " + bed.head.toShortString());
        teleportNear(mc, bed.head);
    }

    /** Test-environment action from the bed list screen: instant destroy. */
    public void requestTestInstantDestroy() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || selected == null) {
            return;
        }
        if (!mc.isInSingleplayer()) {
            ChatUtil.message("§cInstant destroy is singleplayer/test only - breaking normally instead");
            return;
        }
        instantDestroy(mc);
    }

    @Override
    protected void onEnable() {
        beds.clear();
        selected = null;
        walker.stop();
        rescanTimer = 0;
    }

    @Override
    protected void onDisable() {
        walker.stop();
        StatusOverlay.clear("Bed Destroyer V2");
    }

    private void rescan(MinecraftClient mc) {
        beds.clear();
        beds.addAll(BedScanner.scanBeds(mc.world, mc.player.getBlockPos(),
                scanRadius.getInt(), 16, 25));
        beds.sort(Comparator.comparingDouble(b -> b.distance));
        if (selected != null) {
            // refresh or drop the selection
            BedScanner.BedInfo still = beds.stream()
                    .filter(b -> b.head.equals(selected.head)).findFirst().orElse(null);
            selected = still;
        }
    }

    /** Auto-picks a bed by priority mode when no manual selection exists. */
    private void autoSelect(MinecraftClient mc) {
        if (beds.isEmpty() || (selected != null && targetLock.isOn())) {
            return;
        }
        switch (priority.get()) {
            case "Defense" -> beds.stream()
                    .min(Comparator.comparingInt(b -> b.defense)).ifPresent(b -> selected = b);
            case "Manual" -> {
                if (selected == null && !beds.isEmpty()) {
                    selected = beds.get(0);
                }
            }
            default -> selected = beds.get(0);
        }
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }
        if (--rescanTimer <= 0) {
            rescanTimer = 30;
            rescan(mc);
        }
        if (beds.isEmpty()) {
            StatusOverlay.set("Bed Destroyer V2", List.of("§7scanning... no beds found yet"));
            return;
        }
        // target destroyed?
        if (selected != null && !(mc.world.getBlockState(selected.head).getBlock() instanceof BedBlock)) {
            ChatUtil.message("§aBed Destroyer V2: bed destroyed");
            selected = null;
            walker.stop();
        }
        autoSelect(mc);
        if (selected == null) {
            StatusOverlay.set("Bed Destroyer V2", List.of(
                    "beds: " + beds.size(), "§7no target - open the list with .ck beds"));
            return;
        }

        double dist = Math.sqrt(mc.player.getBlockPos().getSquaredDistance(selected.head));
        BlockPos stand = BedScanner.standPosition(mc.world, selected.head);

        // path plan
        if (!walker.hasPath() && dist > attackRange.get() && stand != null) {
            PathFinder.Path path = PathFinder.findPath(mc.world, mc.player.getBlockPos(), stand, 3000);
            walker.setPath(path.waypoints);
        }

        if (dist > attackRange.get()) {
            if (testTeleport.isOn()) {
                teleportNear(mc, stand != null ? stand : selected.head);
            } else {
                walker.tick(mc, 1.0f, true);
            }
        } else {
            walker.stop();
            if (testInstantDestroy.isOn() && mc.isInSingleplayer() && mc.getServer() != null) {
                instantDestroy(mc);
            } else {
                breakBed(mc, selected.head);
            }
        }

        List<String> status = new ArrayList<>();
        status.add("target: §f" + selected.head.toShortString() + " §7(" + (int) dist + "m)");
        status.add("defense: §f" + selected.defense + " §7| beds: §f" + beds.size());
        status.add(walker.hasPath() ? "moving (path " + walker.remainingPath().size() + ")"
                : (dist <= attackRange.get() ? "breaking" : "no path"));
        StatusOverlay.set("Bed Destroyer V2", status);
    }

    private void teleportNear(MinecraftClient mc, BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos).add(0, 0.2, 0);
        if (mc.isInSingleplayer() && mc.getServer() != null) {
            var serverPlayer = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
            if (serverPlayer != null) {
                serverPlayer.networkHandler.requestTeleport(target.x, target.y, target.z,
                        mc.player.getYaw(), mc.player.getPitch());
                return;
            }
        }
        mc.player.setPosition(target.x, target.y, target.z);
    }

    private void instantDestroy(MinecraftClient mc) {
        var server = mc.getServer();
        if (server == null) {
            return;
        }
        for (var world : server.getWorlds()) {
            if (world != null && world.getRegistryKey().equals(mc.world.getRegistryKey())) {
                world.removeBlock(selected.head, false);
                world.removeBlock(selected.foot, false);
                ChatUtil.message("§aBed Destroyer V2: bed removed (test instant destroy)");
                return;
            }
        }
    }

    private void breakBed(MinecraftClient mc, BlockPos bed) {
        Direction side = BedScanner.faceTowards(bed, mc.player.getBlockPos());
        ckpb1.client.core.RotationController.lookAt(Vec3d.ofCenter(bed), 40f);
        if (!mc.interactionManager.attackBlock(bed, side)) {
            mc.interactionManager.updateBlockBreakingProgress(bed, side);
        }
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, Camera camera) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || !renderBeds.isOn()) {
            return;
        }
        for (BedScanner.BedInfo bed : beds) {
            boolean isTarget = bed == selected;
            int color = isTarget ? 0xFF40FF90 : bed.defense >= 2 ? 0xFFFFE055 : 0x80FF9060;
            OverlayRenderer.box(matrices, camera, new Box(bed.head).expand(0.05), color);
        }
        if (renderPath.isOn() && walker.hasPath()) {
            List<Vec3d> points = new ArrayList<>();
            points.add(mc.player.getPos());
            for (BlockPos wp : walker.remainingPath()) {
                points.add(Vec3d.ofCenter(wp).add(0, 0.5, 0));
            }
            OverlayRenderer.path(matrices, camera, points, 0x8040FFC8);
        }
    }
}
