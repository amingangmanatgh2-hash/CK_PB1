package ckpb1.modules.world;

import ckpb1.client.core.Category;
import ckpb1.client.core.BlockScanner;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.Module;
import ckpb1.client.core.PathFinder;
import ckpb1.client.core.StatusOverlay;
import ckpb1.client.core.WalkController;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ListSetting;
import ckpb1.client.core.setting.NumberSetting;
import ckpb1.client.render.OverlayRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resource Assistant (test environments): finds iron/gold/diamond/emerald
 * sources in the world, shows the nearest target + direction on the HUD and
 * can walk there automatically.
 */
public final class ResourceAssistant extends Module {

    public final ListSetting resources = add(new ListSetting("Resources",
            "Blocks to look for", List.of(
            "iron_ore", "gold_ore", "diamond_ore", "emerald_ore",
            "iron_block", "gold_block", "diamond_block", "emerald_block",
            "deepslate_iron_ore", "deepslate_gold_ore", "deepslate_diamond_ore", "deepslate_emerald_ore")));
    public final NumberSetting scanRadius = add(new NumberSetting("Scan Radius",
            "Search radius in blocks", 32, 8, 64, 2, "m"));
    public final BoolSetting autoWalk = add(new BoolSetting("Auto Walk",
            "Walk to the nearest resource automatically", false));
    public final BoolSetting renderTarget = add(new BoolSetting("Render Target",
            "Highlight the nearest resource block", true));
    public final BoolSetting renderPath = add(new BoolSetting("Render Path",
            "Draw the walking path", true));

    private final WalkController walker = new WalkController("Resource Assistant");
    private BlockPos target;
    private int rescanTimer;

    public ResourceAssistant() {
        super("Resource Assistant", "Finds iron/gold/diamond/emerald sources and paths to them (test environments)",
                Category.WORLD);
    }

    private static Block parseBlock(String id) {
        return switch (id.toLowerCase()) {
            case "iron_ore" -> Blocks.IRON_ORE;
            case "gold_ore" -> Blocks.GOLD_ORE;
            case "diamond_ore" -> Blocks.DIAMOND_ORE;
            case "emerald_ore" -> Blocks.EMERALD_ORE;
            case "iron_block" -> Blocks.IRON_BLOCK;
            case "gold_block" -> Blocks.GOLD_BLOCK;
            case "diamond_block" -> Blocks.DIAMOND_BLOCK;
            case "emerald_block" -> Blocks.EMERALD_BLOCK;
            case "deepslate_iron_ore" -> Blocks.DEEPSLATE_IRON_ORE;
            case "deepslate_gold_ore" -> Blocks.DEEPSLATE_GOLD_ORE;
            case "deepslate_diamond_ore" -> Blocks.DEEPSLATE_DIAMOND_ORE;
            case "deepslate_emerald_ore" -> Blocks.DEEPSLATE_EMERALD_ORE;
            default -> null;
        };
    }

    @Override
    protected void onEnable() {
        walker.stop();
        target = null;
        rescanTimer = 0;
        ChatUtil.message("§7Resource Assistant scanning for: §f" + String.join(", ", resources.get()));
    }

    @Override
    protected void onDisable() {
        walker.stop();
        StatusOverlay.clear("Resources");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (--rescanTimer <= 0) {
            rescanTimer = 30;
            List<Block> wanted = new ArrayList<>();
            for (String id : resources.get()) {
                Block block = parseBlock(id);
                if (block != null) {
                    wanted.add(block);
                }
            }
            List<BlockPos> found = BlockScanner.scan(mc.world, mc.player.getBlockPos(),
                    scanRadius.getInt(), 16, wanted.isEmpty() ? (pos, state) -> false
                            : (pos, state) -> wanted.contains(state.getBlock()),
                    15);
            target = found.stream()
                    .min(Comparator.comparingDouble(p -> p.getSquaredDistance(mc.player.getBlockPos())))
                    .orElse(null);
            if (target != null && autoWalk.isOn() && !walker.hasPath()) {
                PathFinder.Path path = PathFinder.findPath(mc.world, mc.player.getBlockPos(),
                        target, 2500);
                walker.setPath(path.waypoints);
            }
        }

        if (target == null) {
            StatusOverlay.set("Resources", List.of("§7no resources found in range"));
            return;
        }
        if (mc.world.getBlockState(target).isAir()) {
            target = null;
            rescanTimer = 0;
            return;
        }
        double dist = Math.sqrt(mc.player.getBlockPos().getSquaredDistance(target));
        String dir = arrowTowards(mc, target);
        StatusOverlay.set("Resources", List.of(
                "target: §f" + mc.world.getBlockState(target).getBlock().getName().getString(),
                String.format("distance: §f%.0fm §7%s", dist, dir),
                walker.hasPath() ? "walking..." : "use Auto Walk to path there"));

        if (autoWalk.isOn()) {
            if (dist <= 2.5) {
                walker.stop();
                ChatUtil.message("§aResource Assistant: reached the target");
                setEnabled(false, false);
                return;
            }
            walker.tick(mc, 1.0f, true);
            if (!walker.hasPath()) {
                PathFinder.Path path = PathFinder.findPath(mc.world, mc.player.getBlockPos(), target, 2500);
                walker.setPath(path.waypoints);
            }
        }
    }

    private String arrowTowards(MinecraftClient mc, BlockPos to) {
        double dx = to.getX() - mc.player.getX();
        double dz = to.getZ() - mc.player.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx)); // 0 = east, 90 = south
        String[] arrows = {"→", "↘", "↓", "↙", "←", "↖", "↑", "↗"};
        int idx = ((int) Math.round(angle / 45.0) + 8) % 8;
        return arrows[idx];
    }

    @Override
    public void onWorldRender(MatrixStack matrices, Camera camera) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (renderTarget.isOn() && target != null && !mc.world.getBlockState(target).isAir()) {
            OverlayRenderer.box(matrices, camera, new Box(target).expand(0.05), 0xFF55E0A0);
        }
        if (renderPath.isOn() && walker.hasPath()) {
            List<Vec3d> points = new ArrayList<>();
            points.add(mc.player.getPos());
            for (BlockPos wp : walker.remainingPath()) {
                points.add(Vec3d.ofCenter(wp).add(0, 0.5, 0));
            }
            OverlayRenderer.path(matrices, camera, points, 0x8055E0A0);
        }
    }
}
