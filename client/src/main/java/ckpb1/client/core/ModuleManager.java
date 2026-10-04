package ckpb1.client.core;

import ckpb1.common.json.MiniJson;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Owns every CK_PB1 module: registration, per-profile persistence, keybinds
 * and the tick/render fan-out.
 */
public final class ModuleManager implements ckpb1.client.core.event.ClientEvents.TickListener,
        ckpb1.client.core.event.ClientEvents.KeyListener {

    public static final String DEFAULT_PROFILE = "default";

    private final List<Module> modules = new ArrayList<>();
    private final List<Module> view = Collections.unmodifiableList(modules);
    private String activeProfile = DEFAULT_PROFILE;

    private Path configDir() {
        return FabricLoader.getInstance().getGameDir().resolve("config").resolve("ckpb1");
    }

    private Path profilesDir() {
        return configDir().resolve("profiles");
    }

    private Path activeProfileFile() {
        return configDir().resolve("active-profile.txt");
    }

    public void register(Module module) {
        modules.add(module);
        Collections.sort(modules, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    public List<Module> all() {
        return view;
    }

    public Module byName(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    public List<Module> byCategory(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == category) {
                out.add(m);
            }
        }
        return out;
    }

    public String activeProfile() {
        return activeProfile;
    }

    // ------------------------------------------------------------- dispatch

    @Override
    public void onClientTick(MinecraftClient mc) {
        for (Module m : modules) {
            if (m.isEnabled()) {
                try {
                    m.onTick();
                } catch (Exception e) {
                    CKLog.warn("Module " + m.getName() + " threw in onTick: " + e);
                }
            }
        }
    }

    @Override
    public boolean onKey(int key, int scancode, int action, int modifiers) {
        if (action != GLFW.GLFW_PRESS) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            return false;
        }
        // Keybinds only fire while no text-entry screen is open.
        if (mc.currentScreen != null) {
            return false;
        }
        for (Module m : modules) {
            if (m.getKeyCode() == key && key != GLFW.GLFW_KEY_UNKNOWN) {
                m.toggle();
                return false; // still let the game see the key
            }
        }
        return false;
    }

    // ----------------------------------------------------------- persistence

    public void loadAll() {
        try {
            if (Files.exists(activeProfileFile())) {
                String name = Files.readString(activeProfileFile(), StandardCharsets.UTF_8).trim();
                if (!name.isBlank() && profileFile(name).toFile().exists()) {
                    activeProfile = name;
                }
            }
        } catch (IOException e) {
            CKLog.warn("Could not read active profile marker: " + e.getMessage());
        }
        loadProfile(activeProfile);
    }

    public List<String> profiles() {
        List<String> out = new ArrayList<>();
        try {
            if (Files.isDirectory(profilesDir())) {
                try (var stream = Files.list(profilesDir())) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                            .forEach(p -> {
                                String n = p.getFileName().toString();
                                out.add(n.substring(0, n.length() - 5));
                            });
                }
            }
        } catch (IOException e) {
            CKLog.warn("Could not list profiles: " + e.getMessage());
        }
        if (out.isEmpty()) {
            out.add(DEFAULT_PROFILE);
        }
        Collections.sort(out);
        return out;
    }

    private Path profileFile(String name) {
        return profilesDir().resolve(safeProfileName(name) + ".json");
    }

    private String safeProfileName(String name) {
        return name.replaceAll("[^A-Za-z0-9_\\- ]", "_");
    }

    public void saveProfile() {
        try {
            Files.createDirectories(profilesDir());
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("profile", activeProfile);
            Map<String, Object> mods = new LinkedHashMap<>();
            for (Module m : modules) {
                mods.put(m.getName(), m.toJson());
            }
            root.put("modules", mods);
            Files.writeString(profileFile(activeProfile), MiniJson.write(root), StandardCharsets.UTF_8);
            Files.writeString(activeProfileFile(), activeProfile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            CKLog.warn("Could not save profile: " + e.getMessage());
        }
    }

    public void loadProfile(String name) {
        activeProfile = safeProfileName(name.isBlank() ? DEFAULT_PROFILE : name);
        Path file = profileFile(activeProfile);
        if (!Files.exists(file)) {
            saveProfile();
            return;
        }
        try {
            Map<String, Object> root = MiniJson.obj(MiniJson.parse(Files.readString(file, StandardCharsets.UTF_8)));
            Object mods = root.get("modules");
            if (mods instanceof Map<?, ?> modMap) {
                for (Module m : modules) {
                    Object o = modMap.get(m.getName());
                    if (o != null) {
                        m.fromJson(MiniJson.obj(o));
                    }
                }
            }
            Files.writeString(activeProfileFile(), activeProfile, StandardCharsets.UTF_8);
        } catch (Exception e) {
            CKLog.warn("Could not load profile '" + activeProfile + "': " + e.getMessage());
        }
    }

    public boolean deleteProfile(String name) {
        if (name.equalsIgnoreCase(DEFAULT_PROFILE) || name.equalsIgnoreCase(activeProfile)) {
            return false;
        }
        try {
            return Files.deleteIfExists(profileFile(name));
        } catch (IOException e) {
            return false;
        }
    }

    /** Human readable name of a GLFW key code, used by the Click GUI. */
    public static String keyName(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return "none";
        }
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + (keyCode - GLFW.GLFW_KEY_A)));
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (keyCode - GLFW.GLFW_KEY_0)));
        }
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) return "R-Shift";
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) return "L-Shift";
        if (keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) return "R-Ctrl";
        if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) return "L-Ctrl";
        if (keyCode == GLFW.GLFW_KEY_SPACE) return "Space";
        if (keyCode == GLFW.GLFW_KEY_TAB) return "Tab";
        if (keyCode == GLFW.GLFW_KEY_UP) return "Up";
        if (keyCode == GLFW.GLFW_KEY_DOWN) return "Down";
        if (keyCode == GLFW.GLFW_KEY_LEFT) return "Left";
        if (keyCode == GLFW.GLFW_KEY_RIGHT) return "Right";
        try {
            return InputUtil.fromKeyCode(keyCode, 0).getLocalizedText().getString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "key " + keyCode;
        }
    }
}
