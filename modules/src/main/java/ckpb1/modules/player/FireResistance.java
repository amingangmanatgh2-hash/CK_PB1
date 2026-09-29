package ckpb1.modules.player;

import ckpb1.client.core.Category;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Fire Resistance (Test God Mode): extinguishes the player and resets fire
 * every tick. Singleplayer keeps it authoritative on the integrated server.
 */
public final class FireResistance extends Module {

    public FireResistance() {
        super("Fire Resistance", "Immunity to fire and lava (singleplayer / test servers)", Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null && !mc.isInSingleplayer()) {
            ChatUtil.message("§7Fire Resistance: on servers this only clears the visual fire");
        }
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.setFireTicks(0);
        mc.player.extinguish();
        if (mc.isInSingleplayer() && mc.getServer() != null) {
            ServerPlayerEntity serverPlayer = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
            if (serverPlayer != null) {
                serverPlayer.setFireTicks(0);
                serverPlayer.extinguish();
            }
        }
    }
}
