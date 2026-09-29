package ckpb1.client.hud;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.ColorHelper;

/**
 * A draggable HUD element. HUD elements are modules (Category.HUD) so they
 * can be toggled/keybound like every other CK_PB1 module; their position is
 * edited in the HUD editor screen and persisted in hud-layout.json.
 */
public abstract class HudElement extends Module {

    protected int x;
    protected int y;
    protected int width = 60;
    protected int height = 12;

    protected HudElement(String name, String description, int defaultX, int defaultY) {
        super(name, description, Category.HUD);
        this.x = defaultX;
        this.y = defaultY;
    }

    /** Draws the element (already position-aware via x/y). */
    public abstract void render(DrawContext context);

    /** Draws the element even when the module is disabled - used by the HUD editor. */
    public final void renderPreview(DrawContext context) {
        render(context);
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public final void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    public final void setX(int newX) {
        this.x = newX;
    }

    public final void setY(int newY) {
        this.y = newY;
    }

    public final boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /** Clamps the element inside the screen. */
    public final void clampToScreen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        this.x = Math.max(0, Math.min(sw - width, x));
        this.y = Math.max(0, Math.min(sh - height, y));
    }

    /** Standard CK_PB1 HUD panel background. */
    protected final void drawPanel(DrawContext context, int w, int h) {
        context.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0x8010101C);
        context.drawBorder(x - 3, y - 3, w + 6, h + 6,
                ColorHelper.Argb.getArgb(140, 70, 200, 255));
    }

    protected final void text(DrawContext context, String s, int ox, int oy, int color) {
        MinecraftClient mc = MinecraftClient.getInstance();
        context.drawTextWithShadow(mc.textRenderer, s, x + ox, y + oy, color);
    }
}
