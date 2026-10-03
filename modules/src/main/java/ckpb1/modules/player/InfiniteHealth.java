package ckpb1.modules.player;

import ckpb1.client.core.Category;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Infinite Health (Test God Mode) - singleplayer only. Keeps the player at
 * maximum health on both the client and the integrated server.
 */
public final class InfiniteHealth extends Module {

    public InfiniteHealth() {
        super("Infinite Health", "Never lose health (singleplayer only)", Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null && !mc.isInSingleplayer()) {
            ChatUtil.message("§cInfinite Health works in singleplayer only - disabling");
            setEnabled(false, false);
        }
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!mc.isInSingleplayer()) {
            setEnabled(false, true);
            return;
        }
        mc.player.setHealth(mc.player.getMaxHealth());
        if (mc.getServer() != null) {
            ServerPlayerEntity serverPlayer = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
            if (serverPlayer != null) {
                serverPlayer.setHealth(serverPlayer.getMaxHealth());
            }
        }
    }
}
