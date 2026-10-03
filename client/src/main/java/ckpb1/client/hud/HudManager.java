package ckpb1.client.hud;

import ckpb1.client.core.CKLog;
import ckpb1.client.core.ModuleManager;
import ckpb1.common.json.MiniJson;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers, renders and persists the layout of CK_PB1 HUD elements.
 */
public final class HudManager {

    private final List<HudElement> elements = new ArrayList<>();
    private final List<HudElement> view = Collections.unmodifiableList(elements);
    private final ModuleManager modules;

    public HudManager(ModuleManager modules) {
        this.modules = modules;
    }

    public void register(HudElement element) {
        elements.add(element);
        modules.register(element);
    }

    public List<HudElement> elements() {
        return view;
    }

    public HudElement byName(String name) {
        for (HudElement e : elements) {
            if (e.getName().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    /** Renders every enabled HUD element. */
    public void render(DrawContext context) {
        for (HudElement e : elements) {
            if (e.isEnabled()) {
                try {
                    e.render(context);
                } catch (Exception ex) {
                    CKLog.warn("HUD element " + e.getName() + " threw: " + ex);
                }
            }
        }
    }

    public HudElement elementAt(double mouseX, double mouseY) {
        for (HudElement e : elements) {
            if (e.isHovered(mouseX, mouseY)) {
                return e;
            }
        }
        return null;
    }

    // ------------------------------------------------------- layout storage

    private Path layoutFile() {
        return FabricLoader.getInstance().getGameDir()
                .resolve("config").resolve("ckpb1").resolve("hud-layout.json");
    }

    public void loadLayout() {
        Path file = layoutFile();
        if (!Files.exists(file)) {
            return;
        }
        try {
            Map<String, Object> root = MiniJson.obj(MiniJson.parse(Files.readString(file, StandardCharsets.UTF_8)));
            for (HudElement e : elements) {
                Object o = root.get(e.getName());
                if (o instanceof Map<?, ?> m) {
                    e.setX(MiniJson.obj(m).containsKey("x") ? (int) MiniJson.lng(m.get("x"), e.getX()) : e.getX());
                    e.setY(MiniJson.obj(m).containsKey("y") ? (int) MiniJson.lng(m.get("y"), e.getY()) : e.getY());
                }
            }
        } catch (Exception e) {
            CKLog.warn("Could not load HUD layout: " + e.getMessage());
        }
    }

    public void saveLayout() {
        try {
            Files.createDirectories(layoutFile().getParent());
            Map<String, Object> root = new LinkedHashMap<>();
            for (HudElement e : elements) {
                Map<String, Object> pos = new LinkedHashMap<>();
                pos.put("x", e.getX());
                pos.put("y", e.getY());
                root.put(e.getName(), pos);
            }
            Files.writeString(layoutFile(), MiniJson.write(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            CKLog.warn("Could not save HUD layout: " + e.getMessage());
        }
    }
}
