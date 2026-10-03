package ckpb1.client.hud.elements;

import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Direction;

/** Coordinates HUD element (XYZ + facing direction). */
public final class CoordinatesElement extends HudElement {

    public CoordinatesElement() {
        super("Coordinates", "Current position and facing direction", 6, 84);
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        String xyz = String.format("XYZ: %.0f / %.0f / %.0f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Direction facing = mc.player.getHorizontalFacing();
        String text = xyz + "  [" + facing + "]";
        width = mc.textRenderer.getWidth(text) + 6;
        height = 12;
        drawPanel(context, width, height);
        text(context, "§f" + text, 3, 2, 0xFFFFFF);
    }
}
