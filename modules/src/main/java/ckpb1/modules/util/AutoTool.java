package ckpb1.modules.util;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/**
 * Auto Tool: switches the hotbar to the fastest tool for the block being
 * mined.
 */
public final class AutoTool extends Module {

    public AutoTool() {
        super("Auto Tool", "Selects the best hotbar tool for the block you are mining", Category.UTILITY);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }
        if (!mc.interactionManager.isBreakingBlock()) {
            return;
        }
        if (!(mc.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult hit)) {
            return;
        }
        var state = mc.world.getBlockState(hit.getBlockPos());
        if (state.isAir()) {
            return;
        }
        int bestSlot = -1;
        float bestSpeed = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().main.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        int current = mc.player.getInventory().selectedSlot;
        if (bestSlot >= 0 && bestSlot != current) {
            float currentSpeed = mc.player.getMainHandStack().getMiningSpeedMultiplier(state);
            if (bestSpeed > currentSpeed) {
                mc.player.getInventory().selectedSlot = bestSlot;
                mc.player.networkHandler.sendPacket(
                        new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(bestSlot));
            }
        }
    }
}
