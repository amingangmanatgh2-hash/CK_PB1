package ckpb1.client.core;

import ckpb1.client.core.setting.ListSetting;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import ckpb1.client.core.setting.Setting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class of every CK_PB1 feature module.
 *
 * <p>NOTE: CK_PB1 is the name of the client itself; module names must never
 * reuse it. PvP/automation modules are intended for singleplayer and private
 * test servers only.</p>
 */
public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled = false;
    private int keyCode = GLFW.GLFW_KEY_UNKNOWN;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    // ------------------------------------------------------------- registry

    protected BoolSetting add(BoolSetting s) {
        settings.add(s);
        return s;
    }

    protected NumberSetting add(NumberSetting s) {
        settings.add(s);
        return s;
    }

    protected ModeSetting add(ModeSetting s) {
        settings.add(s);
        return s;
    }

    protected ListSetting add(ListSetting s) {
        settings.add(s);
        return s;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Setting<?>> T add(T s) {
        settings.add(s);
        return s;
    }

    public List<Setting<?>> settings() {
        return settings;
    }

    public Setting<?> setting(String name) {
        for (Setting<?> s : settings) {
            if (s.name.equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ lifecycle

    /** Called when the module becomes enabled. */
    protected void onEnable() {
    }

    /** Called when the module becomes disabled. */
    protected void onDisable() {
    }

    /** Called every client tick while enabled. */
    public void onTick() {
    }

    /** Called every HUD frame while enabled (2D overlays). */
    public void onHudRender(net.minecraft.client.gui.DrawContext context) {
    }

    /** Called every world render frame while enabled (3D overlays). */
    public void onWorldRender(net.minecraft.client.util.math.MatrixStack matrices,
                              net.minecraft.client.render.Camera camera) {
    }

    public final void toggle() {
        setEnabled(!enabled, true);
    }

    public final void setEnabled(boolean value, boolean announce) {
        if (this.enabled == value) {
            return;
        }
        this.enabled = value;
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            if (value) {
                onEnable();
            } else {
                onDisable();
            }
        } catch (Exception e) {
            CKLog.warn("Module " + name + " threw in " + (value ? "onEnable" : "onDisable") + ": " + e);
        }
        if (announce && mc != null && mc.player != null) {
            ChatUtil.message((enabled ? "§aenabled §7" : "§cdisabled §7") + name);
        }
    }

    public final boolean isEnabled() {
        return enabled;
    }

    // --------------------------------------------------------------- access

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    // ---------------------------------------------------------- persistence

    /** Modules may add extra persisted fields (called by the profile store). */
    protected void writeExtra(Map<String, Object> json) {
    }

    /** Modules may restore extra persisted fields (called by the profile store). */
    protected void readExtra(Map<String, Object> json) {
    }

    public final Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", enabled);
        if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            m.put("key", keyCode);
        }
        Map<String, Object> s = new LinkedHashMap<>();
        for (Setting<?> setting : settings) {
            s.put(setting.name, setting.toJson());
        }
        m.put("settings", s);
        writeExtra(m);
        return m;
    }

    public final void fromJson(Map<String, Object> m) {
        if (m.containsKey("enabled")) {
            this.enabled = ckpb1.common.json.MiniJson.bool(m.get("enabled"), false);
        }
        if (m.containsKey("key")) {
            this.keyCode = (int) ckpb1.common.json.MiniJson.lng(m.get("key"), GLFW.GLFW_KEY_UNKNOWN);
        }
        Object settingsObj = m.get("settings");
        if (settingsObj instanceof Map<?, ?> settingsMap) {
            for (Setting<?> setting : settings) {
                Object v = settingsMap.get(setting.name);
                if (v != null) {
                    setting.fromJson(v);
                }
            }
        }
        readExtra(m);
    }
}
