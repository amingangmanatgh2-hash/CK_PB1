package ckpb1.modules.combat;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Projectile Information: nearby projectiles with type, speed and distance. */
public final class ProjectileInformation extends Module {

    public ProjectileInformation() {
        super("Projectile Information", "Lists nearby projectiles (type, speed, distance)", Category.COMBAT);
    }

    @Override
    public void onHudRender(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ProjectileEntity projectile)) {
                continue;
            }
            double dist = mc.player.distanceTo(entity);
            if (dist > 20) {
                continue;
            }
            Vec3d vel = projectile.getVelocity();
            double speed = Math.sqrt(vel.x * vel.x + vel.y * vel.y + vel.z * vel.z);
            String type = projectile.getType().getName().getString();
            lines.add(String.format("%s  %.0fm  %.1f b/t", type, dist, speed));
            if (lines.size() >= 5) {
                break;
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        int x = mc.getWindow().getScaledWidth() - 130;
        int y = 80;
        int widest = 60;
        for (String line : lines) {
            widest = Math.max(widest, mc.textRenderer.getWidth(line));
        }
        context.fill(x - widest - 8, y, x + 4, y + lines.size() * 11 + 6, 0x80101020);
        context.drawBorder(x - widest - 8, y, widest + 12, lines.size() * 11 + 6, 0x5040C8FF);
        int ly = y + 3;
        context.drawTextWithShadow(mc.textRenderer, "§bProjectiles", x - widest - 4, ly, 0xFFFFFF);
        ly += 11;
        for (String line : lines) {
            context.drawTextWithShadow(mc.textRenderer, line, x - widest - 4, ly, 0xD0D0D0);
            ly += 11;
        }
    }
}
