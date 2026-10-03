package ckpb1.modules.movement;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;

/**
 * Auto Sprint: keeps you sprinting whenever you move forward (simplified
 * LiquidBounce "Sprint" module).
 */
public final class AutoSprint extends Module {

    public AutoSprint() {
        super("Auto Sprint", "Automatically sprint while moving forward", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.player.input.pressingForward
                && !mc.player.isSneaking()
                && !mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
    }
}
