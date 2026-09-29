package ckpb1.modules.player;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * No Fall Damage (Test God Mode): cancels fall damage for the local player.
 * Fully effective in singleplayer (integrated server mixin); on servers it
 * keeps the fall distance reset client-side.
 */
public final class NoFallDamage extends Module {

    public NoFallDamage() {
        super("No Fall Damage", "Cancels fall damage (singleplayer / test servers)", Category.PLAYER);
    }

    /** Called from the LivingEntity mixin. */
    public static boolean shouldCancelFall(LivingEntity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            return false;
        }
        var module = ckpb1.client.CKPB1Client.modules() == null ? null
                : ckpb1.client.CKPB1Client.modules().byName("No Fall Damage");
        if (!(module instanceof NoFallDamage noFall) || !module.isEnabled()) {
            return false;
        }
        return entity.getUuid().equals(mc.player.getUuid());
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.fallDistance = 0;
        // singleplayer: reset on the integrated server so damage never triggers
        if (mc.isInSingleplayer() && mc.getServer() != null) {
            ServerPlayerEntity serverPlayer = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
            if (serverPlayer != null) {
                serverPlayer.fallDistance = 0;
            }
        }
    }
}
