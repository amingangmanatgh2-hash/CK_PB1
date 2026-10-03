package ckpb1.modules.render;

import ckpb1.client.core.BlockScanner;
import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.NumberSetting;
import ckpb1.client.render.OverlayRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * ESP: highlights players, mobs, dropped items and chests with colored
 * boxes (simplified LiquidBounce/Wurst ESP). Chest positions are cached and
 * rescanned every two seconds to keep the frame rate stable on weak PCs.
 */
public final class Esp extends Module {

    public final BoolSetting players = add(new BoolSetting("Players",
            "Highlight players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs",
            "Highlight living mobs", false));
    public final BoolSetting items = add(new BoolSetting("Items",
            "Highlight dropped items", false));
    public final BoolSetting chests = add(new BoolSetting("Chests",
            "Highlight chests & ender chests", true));
    public final NumberSetting radius = add(new NumberSetting("Radius",
            "Scan radius for chests in blocks", 32, 8, 64, 1, "m"));

    private static final int COLOR_PLAYERS = 0x9040C8FF;
    private static final int COLOR_MOBS = 0x90FF6E6E;
    private static final int COLOR_ITEMS = 0x90FFC860;
    private static final int COLOR_CHESTS = 0x90FF9055;

    private final List<BlockPos> chestCache = new ArrayList<>();
    private long nextChestScan;

    public Esp() {
        super("ESP", "Highlights players, mobs, items and chests through walls", Category.RENDER);
    }

    private void rescanChests(MinecraftClient mc) {
        chestCache.clear();
        chestCache.addAll(BlockScanner.scan(mc.world, mc.player.getBlockPos(),
                radius.getInt(), radius.getInt(),
                (pos, state) -> {
                    Block block = state.getBlock();
                    return block instanceof ChestBlock || block instanceof EnderChestBlock;
                }, 40));
        nextChestScan = System.currentTimeMillis() + 2000;
    }

    @Override
    public void onWorldRender(MatrixStack matrices, Camera camera) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (chests.isOn() && System.currentTimeMillis() >= nextChestScan) {
            rescanChests(mc);
        }

        // entities (cheap: iterate the already-loaded client entity list)
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive()) {
                continue;
            }
            if (entity instanceof PlayerEntity && players.isOn()) {
                OverlayRenderer.box(matrices, camera, entity.getBoundingBox(), COLOR_PLAYERS);
            } else if (entity instanceof LivingEntity && mobs.isOn()) {
                OverlayRenderer.box(matrices, camera, entity.getBoundingBox(), COLOR_MOBS);
            } else if (entity instanceof ItemEntity && items.isOn()) {
                OverlayRenderer.box(matrices, camera, entity.getBoundingBox(), COLOR_ITEMS);
            }
        }

        // chests from cache
        if (chests.isOn()) {
            for (BlockPos pos : chestCache) {
                OverlayRenderer.box(matrices, camera, new Box(pos), COLOR_CHESTS);
            }
        }
    }
}
