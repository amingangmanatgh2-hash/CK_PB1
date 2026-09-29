package ckpb1.client.hud.elements;

import ckpb1.client.core.CpsTracker;
import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.ColorHelper;

/** Keystrokes HUD element (WASD, LMB/RMB with CPS, space bar). */
public final class KeystrokesElement extends HudElement {

    private static final int CELL = 20;
    private static final int GAP = 1;

    public KeystrokesElement() {
        super("Keystrokes", "Live movement keys and mouse buttons with CPS", 6, 110);
        width = CELL * 3 + GAP * 4;
        height = CELL * 4 + GAP * 5 + 6;
    }

    private void key(DrawContext ctx, String label, int x, int y, int w, int h, boolean pressed) {
        int bg = pressed ? ColorHelper.Argb.getArgb(220, 70, 200, 255) : 0x90282838;
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.drawBorder(x, y, w, h, 0x50FFFFFF);
        MinecraftClient mc = MinecraftClient.getInstance();
        int tw = mc.textRenderer.getWidth(label);
        ctx.drawTextWithShadow(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2,
                pressed ? 0x001018 : 0xE8E8E8);
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) {
            return;
        }
        KeyBinding forward = mc.options.forwardKey;
        KeyBinding back = mc.options.backKey;
        KeyBinding left = mc.options.leftKey;
        KeyBinding right = mc.options.rightKey;
        KeyBinding jump = mc.options.jumpKey;
        KeyBinding sneak = mc.options.sneakKey;

        int cellX = x + CELL + GAP;
        int rowY = y;
        // W
        key(context, "W", cellX, rowY, CELL, CELL, forward.isPressed());
        rowY += CELL + GAP;
        // A S D
        key(context, "A", x, rowY, CELL, CELL, left.isPressed());
        key(context, "S", cellX, rowY, CELL, CELL, back.isPressed());
        key(context, "D", x + 2 * (CELL + GAP), rowY, CELL, CELL, right.isPressed());
        rowY += CELL + GAP;
        // LMB / RMB with CPS
        key(context, "LMB " + CpsTracker.leftCps(), x, rowY, CELL * 2 + GAP, CELL, CpsTracker.leftDown());
        key(context, "RMB " + CpsTracker.rightCps(), x + 2 * (CELL + GAP), rowY, CELL, CELL, CpsTracker.rightDown());
        rowY += CELL + GAP;
        // space + sneak
        boolean jumpPressed = jump.isPressed() || sneak.isPressed();
        String spaceLabel = sneak.isPressed() ? "SNEAK" : "SPACE";
        key(context, spaceLabel, x, rowY, CELL * 3 + GAP * 2, CELL / 2 + 2, jumpPressed);
    }
}
