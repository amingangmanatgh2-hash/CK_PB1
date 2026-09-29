package ckpb1.modules.movement;

import ckpb1.client.core.Category;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Fly (Test God Mode): flight for singleplayer and private test servers.
 *
 * <p>In singleplayer the module grants the ability on the integrated server,
 * so flight is fully accepted. On multiplayer test servers enable
 * allow-flight in server.properties, otherwise the server may reject it.</p>
 */
public final class Fly extends Module {

    public final NumberSetting speed = add(new NumberSetting("Fly Speed",
            "Creative-style flight speed", 0.5, 0.05, 2.0, 0.05, "x"));

    public Fly() {
        super("Fly", "Flight ability (singleplayer / private test servers)", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().flying = true;
        ChatUtil.message("§7Fly enabled §8(test environments)");
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        ClientPlayerEntity player = mc.player;
        player.getAbilities().allowFlying = true;
        player.getAbilities().flying = true;
        player.getAbilities().setFlySpeed((float) (speed.get() * 0.05));
        // singleplayer: grant on the integrated server too, so the server accepts flight
        if (mc.isInSingleplayer() && mc.getServer() != null) {
            ServerPlayerEntity serverPlayer = mc.getServer().getPlayerManager().getPlayer(player.getUuid());
            if (serverPlayer != null) {
                serverPlayer.getAbilities().allowFlying = true;
            }
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        mc.player.getAbilities().allowFlying = mc.player.isCreative();
        mc.player.getAbilities().flying = false;
        if (mc.isInSingleplayer() && mc.getServer() != null) {
            ServerPlayerEntity serverPlayer = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
            if (serverPlayer != null) {
                serverPlayer.getAbilities().allowFlying = serverPlayer.isCreative();
            }
        }
    }
}
