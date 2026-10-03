package ckpb1.client.gui;

import ckpb1.modules.bedwars.BedDestroyerV2;
import ckpb1.modules.bedwars.BedScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Bed Destroyer V2 bed list: shows every detected bed with distance and
 * defense level; clicking selects (locks) a bed. Test-environment actions:
 * teleport and instant destroy.
 */
public final class BedListScreen extends Screen {

    private static final int ROW_H = 18;
    private final BedDestroyerV2 module;
    private int scroll;

    public BedListScreen(BedDestroyerV2 module) {
        super(Text.literal("CK_PB1 - Beds"));
        this.module = module;
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
        context.fill(0, 0, width, height, 0x60101018);

        List<BedScanner.BedInfo> beds = new ArrayList<>(module.beds());
        int panelW = 240;
        int panelH = Math.min(height - 80, 40 + beds.size() * ROW_H + 30);
        int px = width / 2 - panelW / 2;
        int py = 40;

        context.fill(px, py, px + panelW, py + panelH, 0xE8141420);
        context.drawBorder(px, py, panelW, panelH, 0xFF3A4A66);
        context.drawTextWithShadow(mc.textRenderer, "§bBeds §7(" + beds.size() + ") - click to select & lock",
                px + 8, py + 6, 0xFFFFFF);

        int y = py + 22;
        if (beds.isEmpty()) {
            context.drawTextWithShadow(mc.textRenderer, "§7No beds detected yet (scanning continues)...",
                    px + 8, y, 0xB0B0B0);
        }
        int visibleRows = Math.max(1, (py + panelH - 60 - y) / ROW_H);
        int start = Math.max(0, Math.min(beds.size() - visibleRows, scroll / ROW_H));
        for (int i = start; i < beds.size(); i++) {
            BedScanner.BedInfo bed = beds.get(i);
            if (y + ROW_H > py + panelH - 34) {
                break;
            }
            boolean selected = module.selectedBed() == bed;
            boolean hovered = inBox(mouseX, mouseY, px + 4, y, panelW - 8, ROW_H - 2);
            context.fill(px + 4, y, px + panelW - 4, y + ROW_H - 2,
                    selected ? 0xFF1E5A46 : hovered ? 0xFF1E2A3E : 0x40000000);
            String line = String.format("§f%s  §7%dm  §bdef %d  §7%s", bed.head.toShortString(),
                    (int) bed.distance, bed.defense, selected ? "LOCKED" : "");
            context.drawTextWithShadow(mc.textRenderer, line, px + 10, y + 5, 0xFFFFFF);
            y += ROW_H;
        }

        // action buttons
        int by = py + panelH - 28;
        drawButton(context, "Select", px + 8, by, 70, 18, false);
        drawButton(context, "Teleport (test)", px + 84, by, 100, 18, false);
        drawButton(context, "Destroy (test)", px + 190, by, 90, 18, false);
        context.drawTextWithShadow(mc.textRenderer, "§7ESC to close", px + 8, py + panelH + 8, 0xB0B0B0);
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
        List<BedScanner.BedInfo> beds = module.beds();
        int panelW = 240;
        int panelH = Math.min(height - 80, 40 + beds.size() * ROW_H + 30);
        int px = width / 2 - panelW / 2;
        int py = 40;

        if (button == 0) {
            int by = py + panelH - 28;
            if (inBox(mouseX, mouseY, px + 8, by, 70, 18)) {
                return true; // selection happens by row click
            }
            if (inBox(mouseX, mouseY, px + 84, by, 100, 18)) {
                module.requestTestTeleport(module.selectedBed());
                return true;
            }
            if (inBox(mouseX, mouseY, px + 190, by, 90, 18)) {
                module.requestTestInstantDestroy();
                return true;
            }
            // row click -> select
            int y = py + 22;
            for (BedScanner.BedInfo bed : beds) {
                if (inBox(mouseX, mouseY, px + 4, y, panelW - 8, ROW_H - 2)) {
                    module.selectBed(bed);
                    return true;
                }
                y += ROW_H;
                if (y > py + panelH - 34) {
                    break;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll = Math.max(0, scroll - (int) amount * ROW_H);
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
}
