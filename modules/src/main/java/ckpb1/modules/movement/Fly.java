package ckpb1.modules.movement;

import ckpb1.client.core.Category;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Fly (Test God Mode): three flight styles for singleplayer and private
 * test servers.
 *
 * <ul>
 *   <li><b>Creative</b> - classic ability-based flight (works fully in
 *       singleplayer; on servers enable allow-flight).</li>
 *   <li><b>Jetpack</b> - smooth velocity-based lifting while you hold jump,
 *       mimicking knockback/elytra movement; no abilities are touched.</li>
 *   <li><b>Glide</b> - caps your fall speed so you float down gently.</li>
 * </ul>
 *
 * <p>Anti-Kick periodically touches ground level so vanilla "floating too
 * long" checks stay quiet. Note that no client can guarantee invisibility
 * against every server-side anti-cheat - server owners can always reject
 * movement they consider impossible.</p>
 */
public final class Fly extends Module {

    public final ModeSetting mode = add(new ModeSetting("Mode",
            "Flight style", "Creative", "Jetpack", "Glide"));
    public final NumberSetting speed = add(new NumberSetting("Fly Speed",
            "Creative-mode flight speed", 0.5, 0.05, 2.0, 0.05, "x"));
    public final NumberSetting jetpackPower = add(new NumberSetting("Jetpack Power",
            "Upward acceleration of the jetpack", 0.18, 0.05, 0.5, 0.01));
    public final NumberSetting glideFallSpeed = add(new NumberSetting("Glide Fall Speed",
            "Maximum fall speed while gliding", 0.12, 0.02, 0.5, 0.01));
    public final BoolSetting antiKick = add(new BoolSetting("Anti-Kick",
            "Touch the ground briefly so floating checks stay quiet", true));

    private int antiKickTimer;

    public Fly() {
        super("Fly", "Flight with Creative / Jetpack / Glide styles (singleplayer / private test servers)",
                Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        antiKickTimer = 0;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mode.is("Creative")) {
            mc.player.getAbilities().allowFlying = true;
            mc.player.getAbilities().flying = true;
        }
        ChatUtil.message("§7Fly §a" + mode.get() + " §7- test environments only");
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        if (mode.is("Creative")) {
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

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        ClientPlayerEntity player = mc.player;

        switch (mode.get()) {
            case "Creative" -> {
                player.getAbilities().allowFlying = true;
                player.getAbilities().flying = true;
                player.getAbilities().setFlySpeed((float) (speed.get() * 0.05));
                if (mc.isInSingleplayer() && mc.getServer() != null) {
                    ServerPlayerEntity serverPlayer =
                            mc.getServer().getPlayerManager().getPlayer(player.getUuid());
                    if (serverPlayer != null) {
                        serverPlayer.getAbilities().allowFlying = true;
                    }
                }
                if (antiKick.isOn() && player.getAbilities().flying) {
                    antiKickTimer++;
                    if (antiKickTimer >= 70) {
                        // brief dip resets vanilla floating checks
                        Vec3d v = player.getVelocity();
                        player.setVelocity(v.x, Math.min(v.y, -0.07), v.z);
                        if (antiKickTimer >= 74) {
                            antiKickTimer = 0;
                        }
                    }
                }
            }
            case "Jetpack" -> {
                // pure velocity movement - no ability flags involved
                Vec3d v = player.getVelocity();
                if (player.input.jumping) {
                    double up = Math.min(v.y + jetpackPower.get(), 0.55);
                    player.setVelocity(v.x, up, v.z);
                }
                antiKickTimer = 0;
            }
            default -> { // Glide
                Vec3d v = player.getVelocity();
                if (v.y < -glideFallSpeed.get()) {
                    player.setVelocity(v.x, -glideFallSpeed.get(), v.z);
                }
                antiKickTimer = 0;
            }
        }
    }
}
