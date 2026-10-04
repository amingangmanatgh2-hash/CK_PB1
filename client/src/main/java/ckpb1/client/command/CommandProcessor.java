package ckpb1.client.command;

import ckpb1.client.CKPB1Client;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.Module;
import ckpb1.client.core.ModuleManager;
import ckpb1.client.core.setting.ListSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * CK_PB1 client chat commands. Prefix: {@code .ckpb1} (short: {@code .ck}).
 *
 * <p>Commands are fully local - they are never sent to the server.</p>
 */
public final class CommandProcessor {

    public static final String PREFIX = ".ckpb1";
    public static final String SHORT_PREFIX = ".ck";

    private CommandProcessor() {
    }

    /** @return true when the message was a CK_PB1 command and must not be sent. */
    public static boolean handle(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String trimmed = message.trim();
        String body;
        if (trimmed.startsWith(PREFIX)) {
            body = trimmed.substring(PREFIX.length()).trim();
        } else if (trimmed.startsWith(SHORT_PREFIX + " ") || trimmed.equals(SHORT_PREFIX)) {
            body = trimmed.substring(SHORT_PREFIX.length()).trim();
        } else {
            return false;
        }
        if (body.isEmpty()) {
            help();
            return true;
        }
        String[] parts = body.split("\\s+");
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "help" -> {
                help();
            }
            case "gui" -> openGui();
            case "hud" -> openHudEditor();
            case "toggle" -> toggle(parts);
            case "bind" -> bind(parts);
            case "profile" -> profile(parts);
            case "tp" -> teleport(parts);
            case "forward" -> teleportForward(parts);
            case "targets" -> targets(parts);
            case "beds" -> openBedList();
            case "info" -> info();
            default -> ChatUtil.message("§cUnknown command '" + parts[0] + "' §7- try .ck help");
        }
        return true;
    }

    private static void help() {
        ChatUtil.message("§bCK_PB1 §7commands:");
        ChatUtil.message("§f.gui §7- open the Click GUI (RShift)");
        ChatUtil.message("§f.hud §7- open the HUD editor");
        ChatUtil.message("§f.toggle <module> §7- enable/disable a module");
        ChatUtil.message("§f.bind <module> <key> §7- set a module keybind");
        ChatUtil.message("§f.profile list|new|load|delete <name> §7- manage profiles");
        ChatUtil.message("§f.tp <x> <y> <z> §7| §f.forward <m> §7- Teleport module (singleplayer/test)");
        ChatUtil.message("§f.targets list|add|remove|clear [player] §7- Kill Farm target list");
        ChatUtil.message("§f.beds §7- Bed Destroyer V2 bed list");
    }

    private static void openGui() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.send(() -> mc.setScreen(CKPB1Client.clickGui()));
        }
    }

    private static void openHudEditor() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.send(() -> mc.setScreen(CKPB1Client.hudEditor()));
        }
    }

    private static void openBedList() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.send(() -> CKPB1Client.openBedListScreen());
        }
    }

    private static void toggle(String[] parts) {
        if (parts.length < 2) {
            ChatUtil.message("§cUsage: .toggle <module>");
            return;
        }
        Module m = CKPB1Client.modules().byName(parts[1]);
        if (m == null) {
            ChatUtil.message("§cUnknown module '" + parts[1] + "'");
            return;
        }
        m.toggle();
    }

    private static void bind(String[] parts) {
        if (parts.length < 2) {
            ChatUtil.message("§cUsage: .bind <module> <key|none>");
            return;
        }
        Module m = CKPB1Client.modules().byName(parts[1]);
        if (m == null) {
            ChatUtil.message("§cUnknown module '" + parts[1] + "'");
            return;
        }
        if (parts.length < 3 || parts[2].equalsIgnoreCase("none")) {
            m.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
            ChatUtil.message("§7Keybind of " + m.getName() + " cleared");
            return;
        }
        int key = parseKey(parts[2]);
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            ChatUtil.message("§cUnknown key '" + parts[2] + "' (A-Z, 0-9, F1-F12, RShift...)");
            return;
        }
        m.setKeyCode(key);
        ChatUtil.message("§7Bound " + m.getName() + " to " + ModuleManager.keyName(key));
    }

    private static int parseKey(String s) {
        String up = s.toUpperCase(Locale.ROOT);
        if (up.length() == 1) {
            char c = up.charAt(0);
            if (c >= 'A' && c <= 'Z') return GLFW.GLFW_KEY_A + (c - 'A');
            if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + (c - '0');
        }
        return switch (up) {
            case "RSHIFT", "R-SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LSHIFT", "L-SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RCTRL", "R-CTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LCTRL", "L-CTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            default -> {
                if (up.startsWith("F") && up.length() > 1) {
                    try {
                        int n = Integer.parseInt(up.substring(1));
                        if (n >= 1 && n <= 12) yield GLFW.GLFW_KEY_F1 + (n - 1);
                    } catch (NumberFormatException ignored) {
                    }
                }
                yield GLFW.GLFW_KEY_UNKNOWN;
            }
        };
    }

    private static void profile(String[] parts) {
        ModuleManager modules = CKPB1Client.modules();
        if (parts.length < 2) {
            ChatUtil.message("§cUsage: .profile list|new|load|delete <name>");
            return;
        }
        String sub = parts[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> ChatUtil.message("§7Profiles: §f" + String.join(", ", modules.profiles())
                    + " §7(active: §f" + modules.activeProfile() + "§7)");
            case "new", "save" -> {
                if (parts.length < 3) {
                    ChatUtil.message("§cUsage: .profile new <name>");
                    return;
                }
                modules.saveProfile();
                ChatUtil.message("§aSaved profile '" + parts[2] + "'");
            }
            case "load" -> {
                if (parts.length < 3) {
                    ChatUtil.message("§cUsage: .profile load <name>");
                    return;
                }
                modules.loadProfile(parts[2]);
                CKPB1Client.hud().loadLayout();
                ChatUtil.message("§aLoaded profile '" + modules.activeProfile() + "'");
            }
            case "delete" -> {
                if (parts.length < 3) {
                    ChatUtil.message("§cUsage: .profile delete <name>");
                    return;
                }
                ChatUtil.message(modules.deleteProfile(parts[2])
                        ? "§aDeleted profile '" + parts[2] + "'"
                        : "§cCannot delete '" + parts[2] + "' (active/default profiles are protected)");
            }
            default -> ChatUtil.message("§cUnknown profile subcommand");
        }
    }

    private static void teleport(String[] parts) {
        Module m = CKPB1Client.modules().byName("Teleport");
        if (!(m instanceof ckpb1.modules.player.Teleport tp)) {
            ChatUtil.message("§cTeleport module not available");
            return;
        }
        if (parts.length < 4) {
            ChatUtil.message("§cUsage: .tp <x> <y> <z>");
            return;
        }
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            tp.teleportTo(x, y, z);
        } catch (NumberFormatException e) {
            ChatUtil.message("§cCoordinates must be numbers");
        }
    }

    private static void teleportForward(String[] parts) {
        Module m = CKPB1Client.modules().byName("Teleport");
        if (!(m instanceof ckpb1.modules.player.Teleport tp)) {
            ChatUtil.message("§cTeleport module not available");
            return;
        }
        double dist = 10;
        if (parts.length >= 2) {
            try {
                dist = Double.parseDouble(parts[1]);
            } catch (NumberFormatException e) {
                ChatUtil.message("§cDistance must be a number");
                return;
            }
        }
        tp.teleportForward(dist);
    }

    private static void targets(String[] parts) {
        Module killFarm = CKPB1Client.modules().byName("Kill Farm");
        if (killFarm == null) {
            ChatUtil.message("§cKill Farm module not available");
            return;
        }
        ListSetting list = (ListSetting) killFarm.setting("Target List");
        if (list == null) {
            ChatUtil.message("§cKill Farm target list not found");
            return;
        }
        if (parts.length < 2) {
            ChatUtil.message("§cUsage: .targets list|add|remove|clear [player]");
            return;
        }
        switch (parts[1].toLowerCase(Locale.ROOT)) {
            case "list" -> ChatUtil.message("§7Kill Farm targets: §f" + (list.get().isEmpty() ? "(empty)" : String.join(", ", list.get())));
            case "add" -> {
                if (parts.length < 3) {
                    ChatUtil.message("§cUsage: .targets add <player>");
                    return;
                }
                list.add(parts[2]);
                ChatUtil.message("§aAdded '" + parts[2] + "' to the Kill Farm target list");
            }
            case "remove" -> {
                if (parts.length < 3) {
                    ChatUtil.message("§cUsage: .targets remove <player>");
                    return;
                }
                ChatUtil.message(list.remove(parts[2])
                        ? "§aRemoved '" + parts[2] + "'"
                        : "§c'" + parts[2] + "' is not in the target list");
            }
            case "clear" -> {
                list.clear();
                ChatUtil.message("§aKill Farm target list cleared");
            }
            default -> ChatUtil.message("§cUnknown targets subcommand");
        }
    }

    private static void info() {
        ChatUtil.message("§b" + ckpb1.common.CKPB1.fullName() + " §7| modules: §f"
                + CKPB1Client.modules().all().size() + " §7| profile: §f"
                + CKPB1Client.modules().activeProfile());
    }
}
