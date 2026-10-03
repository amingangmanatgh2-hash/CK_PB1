package ckpb1.modules.render;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.render.OverlayRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Tracers: draws lines from your eyes to nearby players and mobs
 * (classic Wurst/LiquidBounce render module).
 */
public final class Tracers extends Module {

    public final BoolSetting players = add(new BoolSetting("Players",
            "Trace players", true));
    public final BoolSetting mobs = add(new BoolSetting("Mobs",
            "Trace living mobs", false));

    public Tracers() {
        super("Tracers", "Draws lines from your eyes to players and mobs", Category.RENDER);
    }

    @Override
    public void onWorldRender(MatrixStack matrices, Camera camera) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        Vec3d eye = mc.player.getEyePos();
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive()) {
                continue;
            }
            boolean isPlayer = entity instanceof PlayerEntity;
            if (isPlayer ? !players.isOn() : !(entity instanceof LivingEntity) || !mobs.isOn()) {
                continue;
            }
            Box box = entity.getBoundingBox();
            OverlayRenderer.line(matrices, camera, eye, box.getCenter(),
                    isPlayer ? 0x8040C8FF : 0x80FF6E6E);
        }
    }
}
