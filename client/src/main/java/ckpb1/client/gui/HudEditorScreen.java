package ckpb1.client.gui;

import ckpb1.client.CKPB1Client;
import ckpb1.client.CKPB1Client;
import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * CK_PB1 HUD editor: drag HUD elements anywhere on screen. Layout is saved
 * on close. Buttons at the top: toggle snap-to-grid, reset layout, close.
 */
public final class HudEditorScreen extends Screen {

    private HudElement dragging;
    private int dragOffX, dragOffY;
    private boolean snap = true;

    public HudEditorScreen() {
        super(Text.literal("CK_PB1 HUD Editor"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        // render world HUD normally + editor chrome
        CKPB1Client.hud().render(context);

        // top bar with buttons
        context.fill(0, 0, width, 26, 0xC8161622);
        context.drawTextWithShadow(mc.textRenderer, "§bCK_PB1 HUD Editor §7- drag elements | "
                + (snap ? "snap: on" : "snap: off"), 8, 9, 0xFFFFFF);
        drawButton(context, "Snap", width - 230, 6, 60, 16, inBox(mouseX, mouseY, width - 230, 6, 60, 16));
        drawButton(context, "Reset", width - 162, 6, 60, 16, inBox(mouseX, mouseY, width - 162, 6, 60, 16));
        drawButton(context, "Close", width - 94, 6, 60, 16, inBox(mouseX, mouseY, width - 94, 6, 60, 16));

        // element outlines + names + drag handles
        for (HudElement e : CKPB1Client.hud().elements()) {
            boolean hover = e.isHovered(mouseX, mouseY) || dragging == e;
            context.fill(e.getX() - 4, e.getY() - 4, e.getX() + e.getWidth() + 4, e.getY() + e.getHeight() + 4,
                    hover ? 0x3040C8FF : 0x20000000);
            context.drawBorder(e.getX() - 4, e.getY() - 4, e.getWidth() + 8, e.getHeight() + 8,
                    hover ? ColorHelper.Argb.getArgb(255, 80, 220, 255) : ColorHelper.Argb.getArgb(160, 120, 140, 180));
            context.drawTextWithShadow(mc.textRenderer,
                    (e.isEnabled() ? "§f" : "§8") + e.getName() + (e.isEnabled() ? "" : " §7(off)"),
                    e.getX() - 4, e.getY() - 14, 0xFFFFFF);
        }
        String hint = "Tip: enable/disable elements in the Click GUI (HUD category). ESC saves & closes.";
        int hw = mc.textRenderer.getWidth(hint);
        context.drawTextWithShadow(mc.textRenderer, "§7" + hint, width / 2 - hw / 2, height - 14, 0xFFFFFF);
    }

    private void drawButton(DrawContext context, String label, int x, int y, int w, int h, boolean hover) {
        MinecraftClient mc = MinecraftClient.getInstance();
        context.fill(x, y, x + w, y + h, hover ? 0xFF2E6E9E : 0xFF22466A);
        context.drawBorder(x, y, w, h, 0xFF4090C8);
        int tw = mc.textRenderer.getWidth(label);
        context.drawTextWithShadow(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inBox(mouseX, mouseY, width - 230, 6, 60, 16)) {
                snap = !snap;
                return true;
            }
            if (inBox(mouseX, mouseY, width - 162, 6, 60, 16)) {
                for (HudElement e : CKPB1Client.hud().elements()) {
                    e.setPosition(20, 20);
                }
                // re-flow defaults: rough grid so elements don't overlap
                int y = 20;
                for (HudElement e : CK_PB1Client.hud().elements()) {
                    e.setPosition(20, y);
                    y += e.getHeight() + 18;
                }
                return true;
            }
            if (inBox(mouseX, mouseY, width - 94, 6, 60, 16)) {
                close();
                return true;
            }
            HudElement hit = CKPB1Client.hud().elementAt(mouseX, mouseY);
            if (hit != null) {
                dragging = hit;
                dragOffX = (int) mouseX - hit.getX();
                dragOffY = (int) mouseY - hit.getY();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            int nx = (int) mouseX - dragOffX;
            int ny = (int) mouseY - dragOffY;
            if (snap) {
                nx = Math.round(nx / 5.0f) * 5;
                ny = Math.round(ny / 5.0f) * 5;
            }
            dragging.setPosition(nx, ny);
            dragging.clampToScreen();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            CKPB1Client.hud().saveLayout();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        CKPB1Client.hud().saveLayout();
        super.close();
    }
}
