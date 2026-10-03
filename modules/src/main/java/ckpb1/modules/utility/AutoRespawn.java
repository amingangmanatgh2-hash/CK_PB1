package ckpb1.modules.utility;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

/**
 * Auto Respawn: automatically clicks respawn after dying (simplified
 * Wurst/LiquidBounce "AutoRespawn").
 */
public final class AutoRespawn extends Module {

    public final NumberSetting delay = add(new NumberSetting("Delay",
            "Milliseconds to wait before respawning", 800, 0, 5000, 100, "ms"));

    private long diedAt;

    public AutoRespawn() {
        super("Auto Respawn", "Automatically respawns after you die", Category.UTILITY);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.networkHandler == null) {
            return;
        }
        if (mc.player.isDead()) {
            if (diedAt == 0) {
                diedAt = System.currentTimeMillis();
                return;
            }
            if (System.currentTimeMillis() - diedAt >= delay.get().longValue()) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
                        mc.player, ClientCommandC2SPacket.Mode.PERFORM_RESPAWN));
                diedAt = 0;
            }
        } else {
            diedAt = 0;
        }
    }
}
