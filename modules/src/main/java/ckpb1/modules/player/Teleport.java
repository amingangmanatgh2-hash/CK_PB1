package ckpb1.modules.player;

import ckpb1.client.core.Category;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Teleport (Test God Mode): teleports to configured coordinates or forward
 * in the look direction. Keybind (default J) triggers the selected mode;
 * `.ck tp x y z` and `.ck forward <m>` work from chat.
 */
public final class Teleport extends Module {

    public final ModeSetting mode = add(new ModeSetting("Mode",
            "What the keybind teleports to", "Forward", "Coords"));
    public final NumberSetting x = add(new NumberSetting("X", "Target X coordinate", 0, -100000, 100000, 1));
    public final NumberSetting y = add(new NumberSetting("Y", "Target Y coordinate", 64, -64, 384, 1));
    public final NumberSetting z = add(new NumberSetting("Z", "Target Z coordinate", 0, -100000, 100000, 1));
    public final NumberSetting forwardDistance = add(new NumberSetting("Forward Distance",
            "Blocks to teleport forward", 10, 1, 100, 1, "m"));

    public Teleport() {
        super("Teleport", "Teleport to coordinates or forward (singleplayer / test servers)", Category.PLAYER);
    }

    /** Momentary module: enabling performs the teleport and disables again. */
    @Override
    protected void onEnable() {
        if (mode.is("Coords")) {
            teleportTo(x.get(), y.get(), z.get());
        } else {
            teleportForward(forwardDistance.get());
        }
        setEnabled(false, false);
    }

    /** Teleports forward in the look direction (used by the keybind and .ck forward). */
    public void teleportForward(double distance) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d horizontal = new Vec3d(look.x, 0, look.z).normalize();
        if (horizontal.lengthSquared() < 1e-4) {
            horizontal = new Vec3d(0, 0, 1);
        }
        Vec3d target = mc.player.getPos().add(horizontal.multiply(distance));
        teleportTo(target.x, mc.player.getY(), target.z);
    }

    /** Teleports to the given coordinates. */
    public void teleportTo(double tx, double ty, double tz) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.isInSingleplayer() && mc.getServer() != null) {
            ServerPlayerEntity serverPlayer = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
            if (serverPlayer != null) {
                serverPlayer.networkHandler.requestTeleport(tx, ty, tz,
                        mc.player.getYaw(), mc.player.getPitch());
                ChatUtil.message(String.format("§ateleported to %.0f / %.0f / %.0f", tx, ty, tz));
                return;
            }
        }
        // multiplayer test servers: client-side position update
        mc.player.setPosition(tx, ty, tz);
        mc.player.setVelocity(Vec3d.ZERO);
        ChatUtil.message(String.format("§ateleported to %.0f / %.0f / %.0f §7(client-side)", tx, ty, tz));
    }
}
