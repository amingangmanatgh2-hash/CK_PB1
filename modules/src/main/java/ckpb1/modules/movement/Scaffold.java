package ckpb1.modules.movement;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.RotationController;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Scaffold: automatically places blocks under your feet while you walk, so
 * you can run across gaps and build towers without looking down (simplified
 * version of the classic LiquidBounce/Wurst scaffold, for test environments).
 */
public final class Scaffold extends Module {

    public final ModeSetting mode = add(new ModeSetting("Mode",
            "Scaffold style", "Normal", "Tower"));
    public final NumberSetting placeDelay = add(new NumberSetting("Place Delay",
            "Ticks between placement attempts", 2, 1, 10, 1, "t"));
    public final NumberSetting extension = add(new NumberSetting("Extend",
            "How far ahead of your feet blocks are placed", 0.4, 0.0, 1.2, 0.05, "b"));
    public final BoolSetting keepRotation = add(new BoolSetting("Keep Rotation",
            "Softly aim at the placement point", true));
    public final BoolSetting autoSlot = add(new BoolSetting("Auto Slot",
            "Switch to a block slot automatically", true));
    public final BoolSetting safe = add(new BoolSetting("Safe Place",
            "Only place while standing on a solid block", true));

    private int cooldown;

    public Scaffold() {
        super("Scaffold", "Auto-places blocks beneath you while walking (test environments)",
                Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        cooldown = 0;
    }

    private int blockSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().main.get(i).getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Tries to place a block at {@code pos} by clicking the face of a solid
     * neighbour. Returns true when a placement was sent.
     */
    private boolean placeAt(MinecraftClient mc, BlockPos pos) {
        if (!mc.world.getBlockState(pos).isReplaceable()) {
            return false;
        }
        Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : order) {
            BlockPos support = pos.offset(dir);
            if (mc.world.getBlockState(support).getCollisionShape(mc.world, support).isEmpty()) {
                continue;
            }
            // face centre between support and pos
            Vec3d hitVec = Vec3d.ofCenter(support).add(
                    (pos.getX() - support.getX()) * 0.5,
                    (pos.getY() - support.getY()) * 0.5,
                    (pos.getZ() - support.getZ()) * 0.5);
            Direction side = dir.getOpposite();
            BlockHitResult hit = new BlockHitResult(hitVec, side, support, false);
            if (keepRotation.isOn()) {
                RotationController.lookAt(Vec3d.ofCenter(pos).add(0, 0.4, 0), 45f);
            }
            if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit).isAccepted()) {
                mc.player.swingHand(Hand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) {
            return;
        }
        ClientPlayerEntity player = mc.player;

        int slot = blockSlot(mc);
        if (slot < 0) {
            return; // no blocks in the hotbar
        }
        if (autoSlot.isOn() && player.getInventory().selectedSlot != slot) {
            player.getInventory().selectedSlot = slot;
            player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        Vec3d velocity = player.getVelocity();
        Vec3d feet = player.getPos();

        if (mode.is("Tower") && (player.input.jumping || !player.isOnGround())) {
            // tower: keep placing below while rising
            BlockPos pos = BlockPos.ofFloored(feet.x, feet.y - 0.31, feet.z);
            if (placeAt(mc, pos)) {
                cooldown = Math.max(1, placeDelay.getInt() + ThreadLocalRandom.current().nextInt(-1, 2));
            }
            return;
        }

        if (safe.isOn() && !player.isOnGround()) {
            return; // do not bridge off into the void while airborne
        }

        // normal: place below the (slightly extrapolated) feet position
        Vec3d ahead = feet.add(velocity.x * extension.get(), 0, velocity.z * extension.get());
        BlockPos pos = BlockPos.ofFloored(ahead.x, feet.y - 0.31, ahead.z);
        if (player.isOnGround() || Math.abs(velocity.y) < 0.08) {
            if (placeAt(mc, pos)) {
                cooldown = Math.max(1, placeDelay.getInt() + ThreadLocalRandom.current().nextInt(-1, 2));
            }
        }
    }
}
