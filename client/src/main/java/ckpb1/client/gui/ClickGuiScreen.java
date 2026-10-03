package ckpb1.client.gui;

import ckpb1.client.CKPB1Client;
import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.ModuleManager;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ListSetting;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import ckpb1.client.core.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CK_PB1 Click GUI.
 *
 * <ul>
 *   <li>Left-click a module: toggle it</li>
 *   <li>Right-click a module: expand/collapse its settings</li>
 *   <li>Middle-click a module: capture a new keybind</li>
 *   <li>Drag panel headers to move panels, mouse wheel to scroll</li>
 *   <li>Top-right search box filters modules; [HUD] opens the HUD editor</li>
 * </ul>
 */
public final class ClickGuiScreen extends Screen {

    private static final int PANEL_W = 118;
    private static final int HEADER_H = 18;
    private static final int ROW_H = 16;
    private static final int SETTING_H = 15;

    private final List<Panel> panels = new ArrayList<>();

    // drag state
    private Panel draggingPanel;
    private int dragOffX, dragOffY;
    private NumberSetting draggingSlider;
    private Module draggingSliderModule;

    // keybind capture
    private Module capturingKey;

    // search
    private final StringBuilder search = new StringBuilder();
    private boolean searchFocused;

    // list editing
    private ListSetting activeList;
    private final StringBuilder listInput = new StringBuilder();

    /** Extra rows a setting currently occupies (expanded list items + input row). */
    private int settingExtra(Setting<?> s) {
        return s instanceof ListSetting ls && activeList == ls ? (ls.get().size() + 1) * SETTING_H : 0;
    }

    private static final class Panel {
        final Category category;
        int x;
        int y;
        boolean open = true;
        int scroll;
        final List<Module> expanded = new ArrayList<>();

        Panel(Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }
    }

    public ClickGuiScreen() {
        super(Text.literal("CK_PB1"));
        int i = 0;
        for (Category category : Category.values()) {
            panels.add(new Panel(category, 16 + (i % 5) * (PANEL_W + 8), 30 + (i / 5) * (HEADER_H + 4)));
            i++;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // --------------------------------------------------------------- helpers

    private boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private List<Module> visibleModules(Panel panel) {
        List<Module> all = CKPB1Client.modules().byCategory(panel.category);
        if (search.length() == 0) {
            return all;
        }
        String q = search.toString().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : all) {
            if (m.getName().toLowerCase(Locale.ROOT).contains(q)
                    || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(m);
            }
        }
        return out;
    }

    private int panelContentHeight(Panel panel) {
        int h = HEADER_H;
        for (Module m : visibleModules(panel)) {
            h += ROW_H;
            if (panel.expanded.contains(m)) {
                for (Setting<?> s : m.settings()) {
                    h += SETTING_H + settingExtra(s);
                }
            }
        }
        return h;
    }

    // ---------------------------------------------------------------- render

    /** Accent color per category (used for panel headers and strips). */
    private static int categoryColor(Category c) {
        return switch (c) {
            case COMBAT -> 0xFFFF6E6E;
            case MOVEMENT -> 0xFF55DD88;
            case PLAYER -> 0xFFFFC860;
            case RENDER -> 0xFFB48EFF;
            case WORLD -> 0xFF9050FF;
            case UTILITY -> 0xFF40C8FF;
            case BEDWARS -> 0xFFFF9055;
            case AUTOMATION -> 0xFFFF7AB8;
            case HUD -> 0xFF7FE8D8;
        };
    }

    private int enabledCount() {
        int n = 0;
        for (Module m : CKPB1Client.modules().all()) {
            if (m.isEnabled()) {
                n++;
            }
        }
        return n;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        context.fill(0, 0, width, height, 0x60101018);

        // header bar with gradient
        context.fillGradient(0, 0, width, 24, 0xF01A2438, 0xF0101828);
        context.fill(0, 24, width, 25, 0xFF40C8FF);
        String profile = CKPB1Client.modules().activeProfile();
        int on = enabledCount();
        int total = CKPB1Client.modules().all().size();
        context.drawTextWithShadow(mc.textRenderer, "§bCK_PB1 §f" + ckpb1.common.CKPB1.VERSION, 8, 4, 0xFFFFFF);
        context.drawTextWithShadow(mc.textRenderer,
                "§7profile §f" + profile + " §8| §7on §a" + on + "§8/§f" + total, 8, 14, 0xFFFFFF);
        // search box
        int sbX = width - 190;
        context.fill(sbX, 5, sbX + 130, 19, searchFocused ? 0xFF2A3A50 : 0xFF1E2430);
        context.drawBorder(sbX, 5, 130, 14, 0xFF40506A);
        String searchHint = search.length() == 0 ? "search..." : search.toString();
        context.drawTextWithShadow(mc.textRenderer, searchFocused ? searchHint + "_" : searchHint, sbX + 4, 9, search.length() == 0 ? 0xFF7A8698 : 0xFFE8F0FF);
        // HUD editor button
        boolean hudHover = inBox(mouseX, mouseY, sbX + 136, 5, 48, 14);
        context.fill(sbX + 136, 5, sbX + 184, 19, hudHover ? 0xFF2E6E9E : 0xFF22466A);
        context.drawTextWithShadow(mc.textRenderer, "HUD Editor", sbX + 141, 9, 0xFFFFFFFF);

        // panels
        for (Panel panel : panels) {
            List<Module> modules = visibleModules(panel);
            int contentH = panelContentHeight(panel);
            int maxH = height - panel.y - 8;
            int panelH = Math.min(contentH, Math.max(HEADER_H + ROW_H, maxH));
            // panel background + border
            context.fill(panel.x, panel.y, panel.x + PANEL_W, panel.y + panelH, 0xD0141420);
            context.drawBorder(panel.x, panel.y, PANEL_W, panelH, panel.open ? 0xFF2A3A50 : 0xFF202A3A);
            // header with category accent strip
            boolean headerHover = inBox(mouseX, mouseY, panel.x, panel.y, PANEL_W, HEADER_H);
            int accent = categoryColor(panel.category);
            context.fill(panel.x, panel.y, panel.x + PANEL_W, panel.y + HEADER_H,
                    headerHover ? 0xFF243248 : 0xFF1A2638);
            context.fill(panel.x, panel.y, panel.x + 3, panel.y + HEADER_H, accent);
            context.drawTextWithShadow(mc.textRenderer, "§b" + panel.category.label, panel.x + 7, panel.y + 5, 0xFFFFFF);
            String count = modules.size() + (panel.open ? " -" : " +");
            context.drawTextWithShadow(mc.textRenderer, count, panel.x + PANEL_W - mc.textRenderer.getWidth(count) - 4, panel.y + 5, 0xFF9FB6CC);
            if (!panel.open) {
                continue;
            }

            // clip area marker (manual clip: rows drawn only inside panel bounds)
            int rowY = panel.y + HEADER_H - panel.scroll;
            int bottom = panel.y + panelH;

            for (Module module : modules) {
                if (rowY + ROW_H <= panel.y + HEADER_H || rowY >= bottom) {
                    rowY += ROW_H;
                    // still account settings height for scroll consistency
                    if (panel.expanded.contains(module)) {
                        for (Setting<?> s : module.settings()) {
                            rowY += SETTING_H + settingExtra(s);
                        }
                    }
                    continue;
                }
                boolean hovered = inBox(mouseX, mouseY, panel.x, rowY, PANEL_W, ROW_H);
                context.fill(panel.x + 1, rowY, panel.x + PANEL_W - 1, rowY + ROW_H,
                        hovered ? 0xFF1E2A3E : 0x00000000);
                context.drawTextWithShadow(mc.textRenderer,
                        (module.isEnabled() ? "§a" : "§7") + module.getName(),
                        panel.x + 5, rowY + 4, 0xFFFFFF);
                // keybind marker
                if (module.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN) {
                    context.drawTextWithShadow(mc.textRenderer, "§e["
                            + ModuleManager.keyName(module.getKeyCode()) + "]",
                            panel.x + PANEL_W - 16 - mc.textRenderer.getWidth("[" + ModuleManager.keyName(module.getKeyCode()) + "]") - 4,
                            rowY + 4, 0xFFFFFF);
                }
                // settings chevron
                context.drawTextWithShadow(mc.textRenderer, panel.expanded.contains(module) ? "§b-" : "§b+",
                        panel.x + PANEL_W - 10, rowY + 4, 0xFFFFFF);
                rowY += ROW_H;

                if (panel.expanded.contains(module)) {
                    for (Setting<?> setting : module.settings()) {
                        rowY = renderSetting(context, panel, module, setting, rowY, mouseX, mouseY, bottom);
                    }
                }
                rowY += 1;
            }
        }

        // tooltip: hovered module description
        Module hoveredModule = moduleAt(mouseX, mouseY);
        if (hoveredModule != null) {
            List<String> lines = wrap(mc, hoveredModule.getDescription(), 220);
            int ty = mouseY + 10;
            int tw = 220;
            int th = lines.size() * 10 + 6;
            context.fill(mouseX, ty, mouseX + tw, ty + th, 0xF0101018);
            context.drawBorder(mouseX, ty, tw, th, 0xFF3A4A66);
            int ly = ty + 3;
            for (String line : lines) {
                context.drawTextWithShadow(mc.textRenderer, "§7" + line, mouseX + 4, ly, 0xB0B0B0);
                ly += 10;
            }
        }
        if (capturingKey != null) {
            String msg = "Press a key for '" + capturingKey.getName() + "' (ESC clears)";
            int w = mc.textRenderer.getWidth(msg);
            context.fill(width / 2 - w / 2 - 6, height - 44, width / 2 + w / 2 + 6, height - 28, 0xF0101018);
            context.drawBorder(width / 2 - w / 2 - 6, height - 44, w + 12, 16, 0xFF40506A);
            context.drawTextWithShadow(mc.textRenderer, "§e" + msg, width / 2 - w / 2, height - 39, 0xFFFFFF);
        }

        // bottom status bar
        context.fillGradient(0, height - 16, width, height, 0xE0101828, 0xE01A2438);
        String status;
        if (capturingKey != null) {
            status = "§ebind capture: " + capturingKey.getName();
        } else if (hoveredModule != null) {
            String bind = hoveredModule.getKeyCode() == GLFW.GLFW_KEY_UNKNOWN
                    ? "none" : ModuleManager.keyName(hoveredModule.getKeyCode());
            status = "§b" + hoveredModule.getName() + " §8| §7" + hoveredModule.getDescription()
                    + " §8| §7bind: §f" + bind;
            if (status.length() > 110) {
                status = status.substring(0, 110) + "...";
            }
        } else {
            status = "§7L-toggle §8| §7R-settings §8| §7M-bind §8| §7type to search §8| §7profile: §f"
                    + CKPB1Client.modules().activeProfile();
        }
        context.drawTextWithShadow(mc.textRenderer, status, 8, height - 12, 0xFFFFFF);
    }

    @SuppressWarnings("unchecked")
    private int renderSetting(DrawContext context, Panel panel, Module module, Setting<?> setting,
                              int rowY, int mouseX, int mouseY, int bottom) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (rowY + SETTING_H <= panel.y + HEADER_H || rowY >= bottom) {
            return rowY + SETTING_H + settingExtra(setting);
        }
        boolean hovered = inBox(mouseX, mouseY, panel.x + 2, rowY, PANEL_W - 4, SETTING_H);

        if (setting instanceof BoolSetting bool) {
            context.fill(panel.x + 2, rowY, panel.x + PANEL_W - 2, rowY + SETTING_H,
                    hovered ? 0xFF1C2838 : 0x80000000);
            context.drawTextWithShadow(mc.textRenderer, setting.name, panel.x + 8, rowY + 3, 0xFFD0D8E8);
            String v = bool.isOn() ? "§aON" : "§cOFF";
            context.drawTextWithShadow(mc.textRenderer, v,
                    panel.x + PANEL_W - mc.textRenderer.getWidth(v) - 8, rowY + 3, 0xFFFFFF);
        } else if (setting instanceof NumberSetting number) {
            context.fill(panel.x + 2, rowY, panel.x + PANEL_W - 2, rowY + SETTING_H,
                    hovered ? 0xFF1C2838 : 0x80000000);
            // slider track
            context.fill(panel.x + 8, rowY + 11, panel.x + PANEL_W - 8, rowY + 13, 0xFF3A4456);
            context.fill(panel.x + 8, rowY + 11,
                    panel.x + 8 + (int) ((PANEL_W - 16) * number.normalized()), rowY + 13, 0xFF40C8FF);
            context.drawTextWithShadow(mc.textRenderer, setting.name, panel.x + 8, rowY + 1, 0xFFD0D8E8);
            context.drawTextWithShadow(mc.textRenderer, number.display(),
                    panel.x + PANEL_W - mc.textRenderer.getWidth(number.display()) - 8, rowY + 1, 0xFF9FD8FF);
        } else if (setting instanceof ModeSetting mode) {
            context.fill(panel.x + 2, rowY, panel.x + PANEL_W - 2, rowY + SETTING_H,
                    hovered ? 0xFF1C2838 : 0x80000000);
            context.drawTextWithShadow(mc.textRenderer, setting.name, panel.x + 8, rowY + 3, 0xFFD0D8E8);
            String v = mode.display();
            context.drawTextWithShadow(mc.textRenderer, "§e" + v,
                    panel.x + PANEL_W - mc.textRenderer.getWidth(v) - 8, rowY + 3, 0xFFFFFF);
        } else if (setting instanceof ListSetting list) {
            boolean active = activeList == list;
            context.fill(panel.x + 2, rowY, panel.x + PANEL_W - 2, rowY + SETTING_H,
                    hovered || active ? 0xFF1C2838 : 0x80000000);
            context.drawTextWithShadow(mc.textRenderer, setting.name, panel.x + 8, rowY + 3, 0xFFD0D8E8);
            String v = list.display();
            context.drawTextWithShadow(mc.textRenderer, v,
                    panel.x + PANEL_W - mc.textRenderer.getWidth(v) - 8, rowY + 3, 0xFFB0B0B0);
            rowY += SETTING_H;
            if (active) {
                for (String item : list.get()) {
                    if (rowY >= bottom) {
                        break;
                    }
                    boolean itemHover = inBox(mouseX, mouseY, panel.x + 6, rowY, PANEL_W - 8, SETTING_H);
                    context.fill(panel.x + 6, rowY, panel.x + PANEL_W - 2, rowY + SETTING_H,
                            itemHover ? 0xFF482430 : 0x60000000);
                    context.drawTextWithShadow(mc.textRenderer, "§c[x] §f" + item, panel.x + 10, rowY + 3, 0xFFFFFF);
                    rowY += SETTING_H;
                }
                // input row
                if (rowY < bottom) {
                    context.fill(panel.x + 6, rowY, panel.x + PANEL_W - 2, rowY + SETTING_H, 0xFF1E2430);
                    String t = listInput.length() == 0 ? "type + ENTER..." : listInput.toString();
                    context.drawTextWithShadow(mc.textRenderer, "§7" + t, panel.x + 10, rowY + 3, 0xFFE8F0FF);
                    rowY += SETTING_H;
                }
            }
        }
        return rowY;
    }

    private List<String> wrap(MinecraftClient mc, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (mc.textRenderer.getWidth(line + word) > maxWidth - 8 && line.length() > 0) {
                out.add(line.toString());
                line = new StringBuilder();
            }
            line.append(word).append(' ');
        }
        if (!line.isEmpty()) {
            out.add(line.toString().trim());
        }
        return out;
    }

    private Module moduleAt(double mx, double my) {
        for (Panel panel : panels) {
            if (!panel.open) {
                continue;
            }
            int rowY = panel.y + HEADER_H - panel.scroll;
            for (Module module : visibleModules(panel)) {
                if (inBox(mx, my, panel.x, rowY, PANEL_W, ROW_H)) {
                    return module;
                }
                rowY += ROW_H;
                if (panel.expanded.contains(module)) {
                    for (Setting<?> s : module.settings()) {
                        rowY += SETTING_H + settingExtra(s);
                    }
                }
                rowY += 1;
            }
        }
        return null;
    }

    // -------------------------------------------------------------- interact

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // search box / hud button (top bar)
        int sbX = width - 190;
        if (inBox(mouseX, mouseY, sbX, 5, 130, 14)) {
            searchFocused = true;
            activeList = null;
            return true;
        }
        searchFocused = false;
        if (inBox(mouseX, mouseY, sbX + 136, 5, 48, 14)) {
            this.close();
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.setScreen(CKPB1Client.hudEditor());
            return true;
        }
        if (capturingKey != null) {
            return true; // wait for keyPressed
        }

        for (Panel panel : panels) {
            if (!panel.open) {
                if (inBox(mouseX, mouseY, panel.x, panel.y, PANEL_W, HEADER_H) && button == 0) {
                    panel.open = true;
                }
                continue;
            }
            if (inBox(mouseX, mouseY, panel.x, panel.y, PANEL_W, HEADER_H)) {
                if (button == 0) {
                    draggingPanel = panel;
                    dragOffX = (int) mouseX - panel.x;
                    dragOffY = (int) mouseY - panel.y;
                } else if (button == 1) {
                    panel.open = false;
                }
                return true;
            }
            // rows
            int rowY = panel.y + HEADER_H - panel.scroll;
            for (Module module : visibleModules(panel)) {
                if (inBox(mouseX, mouseY, panel.x, rowY, PANEL_W, ROW_H)) {
                    if (button == 0) {
                        module.toggle();
                        CKPB1Client.modules().saveProfile();
                    } else if (button == 1) {
                        if (panel.expanded.contains(module)) {
                            panel.expanded.remove(module);
                        } else {
                            panel.expanded.add(module);
                        }
                    } else if (button == 2) {
                        capturingKey = module;
                    }
                    return true;
                }
                rowY += ROW_H;
                if (panel.expanded.contains(module)) {
                    for (Setting<?> setting : module.settings()) {
                        if (setting instanceof BoolSetting bool) {
                            if (inBox(mouseX, mouseY, panel.x + 2, rowY, PANEL_W - 4, SETTING_H) && button == 0) {
                                bool.toggle();
                                return true;
                            }
                            rowY += SETTING_H;
                        } else if (setting instanceof NumberSetting number) {
                            if (inBox(mouseX, mouseY, panel.x + 2, rowY, PANEL_W - 4, SETTING_H)) {
                                if (button == 0) {
                                    draggingSlider = number;
                                    draggingSliderModule = module;
                                    number.setNormalized((mouseX - (panel.x + 8)) / (PANEL_W - 16));
                                    return true;
                                }
                            }
                            rowY += SETTING_H;
                        } else if (setting instanceof ModeSetting mode) {
                            if (inBox(mouseX, mouseY, panel.x + 2, rowY, PANEL_W - 4, SETTING_H)) {
                                if (button == 0) {
                                    mode.cycleForward();
                                } else if (button == 1) {
                                    mode.cycleBackward();
                                }
                                return true;
                            }
                            rowY += SETTING_H;
                        } else if (setting instanceof ListSetting list) {
                            if (inBox(mouseX, mouseY, panel.x + 2, rowY, PANEL_W - 4, SETTING_H)) {
                                if (button == 0) {
                                    if (activeList == list) {
                                        activeList = null;
                                    } else {
                                        activeList = list;
                                        listInput.setLength(0);
                                    }
                                }
                                return true;
                            }
                            rowY += SETTING_H;
                            if (activeList == list) {
                                for (String item : list.get()) {
                                    if (inBox(mouseX, mouseY, panel.x + 6, rowY, PANEL_W - 8, SETTING_H) && button == 0) {
                                        list.remove(item);
                                        return true;
                                    }
                                    rowY += SETTING_H;
                                }
                                rowY += SETTING_H; // input row
                            }
                        }
                        // note: list item/input rows use settingExtra() layout
                    }
                }
                rowY += 1;
            }
            // keybind marker click (left of chevron): quick capture
            // handled inside module row click - simplified: shift+click captures key
            // (full bind flow also available via .bind command)
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingPanel != null) {
            draggingPanel.x = Math.max(0, (int) mouseX - dragOffX);
            draggingPanel.y = Math.max(24, (int) mouseY - dragOffY);
            return true;
        }
        if (draggingSlider != null && draggingSliderModule != null) {
            for (Panel panel : panels) {
                if (panel.expanded.contains(draggingSliderModule)) {
                    draggingSlider.setNormalized((mouseX - (panel.x + 8)) / (PANEL_W - 16));
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPanel != null || draggingSlider != null) {
            draggingPanel = null;
            draggingSlider = null;
            draggingSliderModule = null;
            CKPB1Client.modules().saveProfile();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (Panel panel : panels) {
            int contentH = panelContentHeight(panel);
            int visibleH = Math.min(contentH, height - panel.y - 8);
            if (inBox(mouseX, mouseY, panel.x, panel.y, PANEL_W, visibleH) && contentH > visibleH) {
                panel.scroll = Math.max(0, Math.min(contentH - visibleH, panel.scroll - (int) amount * ROW_H));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingKey != null) {
            capturingKey.setKeyCode(keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            String keyName = keyCode == GLFW.GLFW_KEY_ESCAPE ? "none" : ModuleManager.keyName(keyCode);
            ckpb1.client.core.ChatUtil.message("§7Bound " + capturingKey.getName() + " to " + keyName);
            capturingKey = null;
            CKPB1Client.modules().saveProfile();
            return true;
        }
        if (activeList != null && keyCode == GLFW.GLFW_KEY_ENTER) {
            if (listInput.length() > 0) {
                activeList.add(listInput.toString());
                listInput.setLength(0);
                CKPB1Client.modules().saveProfile();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && search.length() > 0) {
                search.deleteCharAt(search.length() - 1);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (activeList != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && listInput.length() > 0) {
                listInput.deleteCharAt(listInput.length() - 1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused) {
            if (chr >= 32) {
                search.append(chr);
            }
            return true;
        }
        // start searching as soon as the user types anything
        if (activeList == null && capturingKey == null && chr >= 32) {
            searchFocused = true;
            search.append(chr);
            return true;
        }
        if (activeList != null) {
            if (chr >= 32) {
                listInput.append(chr);
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
}
