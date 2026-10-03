package ckpb1.modules.movement;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Spider: climb vertical walls like a spider while pressing forward
 * (classic Wurst/LiquidBounce movement module, for test environments).
 */
public final class Spider extends Module {

    public final NumberSetting climbSpeed = add(new NumberSetting("Climb Speed",
            "Upward speed while climbing", 0.25, 0.1, 0.6, 0.01));

    public Spider() {
        super("Spider", "Climb walls while pressing forward (test environments)", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        // only climb while actually pushing into a wall
        if (mc.player.horizontalCollision
                && (mc.player.input.pressingForward || mc.player.input.pressingBack
                || mc.player.input.pressingLeft || mc.player.input.pressingRight)) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, climbSpeed.get(), v.z);
        }
    }
}
