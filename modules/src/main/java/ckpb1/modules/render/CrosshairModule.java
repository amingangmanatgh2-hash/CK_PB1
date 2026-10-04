package ckpb1.modules.render;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Crosshair: replaces the vanilla crosshair with a customizable one
 * (style, color, size, gap, thickness).
 */
public final class CrosshairModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Style",
            "Crosshair shape", "Cross", "Dot", "Circle", "T Shape"));
    public final NumberSetting size = add(new NumberSetting("Size",
            "Arm length / radius", 6, 2, 20, 1));
    public final NumberSetting thickness = add(new NumberSetting("Thickness",
            "Line thickness in pixels", 2, 1, 5, 1));
    public final NumberSetting gap = add(new NumberSetting("Gap",
            "Empty space around the center", 3, 0, 10, 1));
    public final NumberSetting red = add(new NumberSetting("Red", "Red component 0-255", 80, 0, 255, 1));
    public final NumberSetting green = add(new NumberSetting("Green", "Green component 0-255", 220, 0, 255, 1));
    public final NumberSetting blue = add(new NumberSetting("Blue", "Blue component 0-255", 255, 0, 255, 1));
    public final NumberSetting alpha = add(new NumberSetting("Alpha", "Opacity 0-255", 230, 30, 255, 1));

    public CrosshairModule() {
        super("Crosshair", "Customizable crosshair (replaces the vanilla crosshair)", Category.RENDER);
    }

    private static CrosshairModule active() {
        if (MinecraftClient.getInstance() == null) {
            return null;
        }
        var modules = ckpb1.client.CKPB1Client.modules();
        if (modules == null) {
            return null;
        }
        var m = modules.byName("Crosshair");
        return m instanceof CrosshairModule cross ? cross : null;
    }

    /** Called from the InGameHud mixin to decide whether to cancel vanilla crosshair. */
    public static boolean shouldReplaceVanillaCrosshair() {
        CrosshairModule m = active();
        return m != null && m.isEnabled();
    }

    @Override
    public void onHudRender(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        if (mc.player.isUsingItem()) {
            return; // vanilla hides crosshair while using items (e.g. bow)
        }
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int color = ((alpha.getInt() & 0xFF) << 24)
                | ((red.getInt() & 0xFF) << 16)
                | ((green.getInt() & 0xFF) << 8)
                | (blue.getInt() & 0xFF);
        int t = thickness.getInt();
        int s = size.getInt();
        int g = gap.getInt();
        switch (style.get()) {
            case "Dot" -> context.fill(cx - t / 2, cy - t / 2, cx - t / 2 + t, cy - t / 2 + t, color);
            case "Circle" -> {
                int r = s;
                for (int a = 0; a < 360; a += 6) {
                    double rad = Math.toRadians(a);
                    int px = cx + (int) Math.round(Math.cos(rad) * r);
                    int py = cy + (int) Math.round(Math.sin(rad) * r);
                    context.fill(px - 1, py - 1, px + 1, py + 1, color);
                }
                context.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0x80000000 | (color & 0xFFFFFF));
            }
            case "T Shape" -> {
                context.fill(cx - t / 2, cy - g - s, cx - t / 2 + t, cy - g, color); // up
                context.fill(cx - s, cy + g, cx + s, cy + g + t, color);             // horizontal
            }
            default -> { // Cross
                context.fill(cx - t / 2, cy - g - s, cx - t / 2 + t, cy - g, color); // up
                context.fill(cx - t / 2, cy + g, cx - t / 2 + t, cy + g + s, color); // down
                context.fill(cx - g - s, cy - t / 2, cx - g, cy - t / 2 + t, color); // left
                context.fill(cx + g, cy - t / 2, cx + g + s, cy - t / 2 + t, color); // right
            }
        }
    }
}
