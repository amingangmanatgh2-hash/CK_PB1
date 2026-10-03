package ckpb1.modules.bedwars;

import ckpb1.client.core.Category;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.Module;
import ckpb1.client.core.StatusOverlay;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import ckpb1.client.render.OverlayRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Bridge Assistant (test environments): automatic bridging with edge
 * detection, configurable style and speed, emergency stop (keybind toggle)
 * and a path preview. The player keeps control of the camera - the module
 * only drives movement input and block placement.
 */
public final class BridgeAssistant extends Module {

    public final ModeSetting mode = add(new ModeSetting("Bridge Type",
            "Bridging style", "God Bridge (0 CPS)", "God Bridge", "Breezily", "Ninja"));
    public final NumberSetting placeDelay = add(new NumberSetting("Place Delay",
            "Ticks between placement attempts", 3, 1, 10, 1, "t"));
    public final NumberSetting speed = add(new NumberSetting("Walk Speed",
            "Backward walking speed multiplier", 1.0, 0.2, 1.0, 0.05, "x"));
    public final BoolSetting requireBlocks = add(new BoolSetting("Require Blocks",
            "Stop automatically when no blocks are held", true));
    public final BoolSetting renderPreview = add(new BoolSetting("Path Preview",
            "Draw the upcoming bridge line", true));

    private int placeTimer;
    private int strafePhase; // for Breezily alternation
    private int pauseTicks; // god-bridge edge safety pause

    public BridgeAssistant() {
        super("Bridge Assistant", "Automatic bridging with edge detection (test environments)",
                Category.BEDWARS);
    }

    @Override
    protected void onEnable() {
        placeTimer = 0;
        strafePhase = 0;
        pauseTicks = 0;
        ChatUtil.message("§7Bridging §aon §7- press §f" + ckpb1.client.core.ModuleManager.keyName(getKeyCode())
                + " §7again for emergency stop");
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.input.movementForward = 0;
            mc.player.input.movementSideways = 0;
            mc.player.input.sneaking = false;
        }
        StatusOverlay.clear("Bridge");
    }

    private boolean holdingBlocks(MinecraftClient mc) {
        return mc.player.getMainHandStack().getItem() instanceof BlockItem
                || mc.player.getOffHandStack().getItem() instanceof BlockItem;
    }

    private Hand blockHand(MinecraftClient mc) {
        if (mc.player.getMainHandStack().getItem() instanceof BlockItem) {
            return Hand.MAIN_HAND;
        }
        return Hand.OFF_HAND;
    }

    /** Horizontal direction the player is moving (backwards from the look direction). */
    private Direction travelDirection(ClientPlayerEntity player) {
        float yaw = player.getYaw();
        // opposite of look
        double dx = -Math.sin(Math.toRadians(yaw));
        double dz = Math.cos(Math.toRadians(yaw));
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) {
            return;
        }
        ClientPlayerEntity player = mc.player;

        if (requireBlocks.isOn() && !holdingBlocks(mc)) {
            ChatUtil.message("§cBridge Assistant: no blocks in hand - emergency stop");
            setEnabled(false, false);
            return;
        }

        Direction travel = travelDirection(player);
        BlockPos feet = player.getBlockPos();
        BlockPos underFeet = feet.down();
        BlockPos gap = underFeet.offset(travel); // ground block under the next cell

        boolean gapIsAir = mc.world.getBlockState(gap).isAir()
                || mc.world.getBlockState(gap).getCollisionShape(mc.world, gap).isEmpty();
        boolean supported = !mc.world.getBlockState(underFeet).getCollisionShape(mc.world, underFeet).isEmpty();

        // movement input: walk backwards (bridging direction), sneak in safe modes
        boolean zeroCps = mode.is("God Bridge (0 CPS)");
        if (pauseTicks > 0 && !zeroCps) {
            pauseTicks--;
            player.input.movementForward = 0;
            player.input.sneaking = true;
        } else {
            player.input.movementForward = -1.0f * speed.get().floatValue();
        }
        switch (mode.get()) {
            case "Breezily" -> {
                player.input.sneaking = true;
                player.input.movementSideways = strafePhase % 2 == 0 ? 0.6f : -0.6f;
            }
            case "Ninja" -> {
                player.input.sneaking = true;
                player.input.movementSideways = 0;
            }
            default -> { // God Bridge & God Bridge (0 CPS): no sneak, full speed
                player.input.sneaking = false;
                player.input.movementSideways = 0;
            }
        }

        // place blocks
        int effectiveDelay = zeroCps ? Math.max(1, placeDelay.getInt() - 1) : placeDelay.getInt();
        if (--placeTimer <= 0 && gapIsAir && supported) {
            placeTimer = effectiveDelay;
            // place against the block under our feet, on the face towards the gap
            Vec3d hitVec = Vec3d.ofCenter(underFeet).add(travel.getVector().getX() * 0.5,
                    0, travel.getVector().getZ() * 0.5);
            BlockHitResult hit = new BlockHitResult(hitVec, travel, underFeet, false);
            if (mc.interactionManager.interactBlock(player, blockHand(mc), hit).isAccepted()) {
                player.swingHand(blockHand(mc));
                strafePhase++;
                if (mode.is("God Bridge")) {
                    pauseTicks = 2; // brief stop so the new block catches us
                }
                // God Bridge (0 CPS): no pause at all - blocks are placed at
                // full walking speed without a single click from the player
            }
        }

        int remaining = countBlocks(mc);
        StatusOverlay.set("Bridge", List.of(
                "mode: §f" + mode.get(),
                "blocks left: §f" + remaining,
                "§7keybind toggle = emergency stop"));
    }

    private int countBlocks(MinecraftClient mc) {
        int count = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            var stack = inv.main.get(i);
            if (stack.getItem() instanceof BlockItem) {
                count += stack.getCount();
            }
        }
        if (mc.player.getOffHandStack().getItem() instanceof BlockItem) {
            count += mc.player.getOffHandStack().getCount();
        }
        return count;
    }

    @Override
    public void onWorldRender(MatrixStack matrices, Camera camera) {
        if (!renderPreview.isOn()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        Direction travel = travelDirection(mc.player);
        BlockPos start = mc.player.getBlockPos().down();
        List<Vec3d> points = new ArrayList<>();
        points.add(Vec3d.ofCenter(start).add(0, 0.5, 0));
        for (int i = 1; i <= 8; i++) {
            points.add(Vec3d.ofCenter(start.offset(travel, i)).add(0, 0.5, 0));
        }
        OverlayRenderer.path(matrices, camera, points, 0x80FF9055);
    }
}
