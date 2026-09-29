package ckpb1.client;

import ckpb1.client.command.CommandProcessor;
import ckpb1.client.core.CKLog;
import ckpb1.client.core.event.ClientEvents;
import ckpb1.client.core.CombatState;
import ckpb1.client.core.CpsTracker;
import ckpb1.client.core.ModuleManager;
import ckpb1.client.core.Stats;
import ckpb1.client.core.StatusOverlay;
import ckpb1.client.gui.ClickGuiScreen;
import ckpb1.client.gui.HudEditorScreen;
import ckpb1.client.hud.HudManager;
import ckpb1.client.hud.elements.ArmorStatusElement;
import ckpb1.client.hud.elements.BedWarsStatusElement;
import ckpb1.client.hud.elements.CoordinatesElement;
import ckpb1.client.hud.elements.CpsElement;
import ckpb1.client.hud.elements.FpsElement;
import ckpb1.client.hud.elements.KeystrokesElement;
import ckpb1.client.hud.elements.PingElement;
import ckpb1.client.hud.elements.PotionStatusElement;
import ckpb1.client.hud.elements.SessionElement;
import ckpb1.client.hud.elements.TargetHudElement;
import ckpb1.client.hud.elements.WatermarkElement;
import ckpb1.common.CKPB1;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

/**
 * CK_PB1 client entrypoint. Registers every module, HUD element and the
 * global event fan-out. CK_PB1 is the client name - modules carry their own
 * feature names.
 */
public final class CKPB1Client implements ClientModInitializer {

    private static ModuleManager modules;
    private static HudManager hud;
    private static boolean initialized;

    public static ModuleManager modules() {
        return modules;
    }

    public static HudManager hud() {
        return hud;
    }

    public static ClickGuiScreen clickGui() {
        return new ClickGuiScreen();
    }

    public static HudEditorScreen hudEditor() {
        return new HudEditorScreen();
    }

    public static void openBedListScreen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ckpb1.modules.bedwars.BedDestroyerV2 v2 =
                (ckpb1.modules.bedwars.BedDestroyerV2) modules.byName("Bed Destroyer V2");
        if (v2 != null) {
            mc.setScreen(new ckpb1.client.gui.BedListScreen(v2));
        }
    }

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        hud = new HudManager(modules);

        // ---- Combat -----------------------------------------------------
        ckpb1.modules.combat.TargetSelector targetSelector = new ckpb1.modules.combat.TargetSelector();
        modules.register(targetSelector);
        modules.register(new ckpb1.modules.combat.CombatAssistant(targetSelector));
        modules.register(new ckpb1.modules.combat.KillFarm());
        ckpb1.modules.combat.KillFarm killFarm =
                (ckpb1.modules.combat.KillFarm) modules.byName("Kill Farm");
        killFarm.setKeyCode(GLFW.GLFW_KEY_K);
        modules.register(new ckpb1.modules.combat.CpsCounter());
        modules.register(new ckpb1.modules.combat.ReachDisplay());
        modules.register(new ckpb1.modules.combat.HitInformation());
        modules.register(new ckpb1.modules.combat.ComboInformation());
        modules.register(new ckpb1.modules.combat.ProjectileInformation());
        modules.register(new ckpb1.modules.combat.AutoFireballDefense());

        // ---- Render -----------------------------------------------------
        modules.register(new ckpb1.modules.render.CrosshairModule());
        modules.register(new ckpb1.modules.render.Fullbright());

        // ---- Movement ---------------------------------------------------
        modules.register(new ckpb1.modules.movement.MovementAssistant());
        modules.register(new ckpb1.modules.movement.Fly());

        // ---- Player (singleplayer / test only) --------------------------
        modules.register(new ckpb1.modules.player.InfiniteHealth());
        modules.register(new ckpb1.modules.player.NoFallDamage());
        modules.register(new ckpb1.modules.player.FireResistance());
        ckpb1.modules.player.Teleport teleport = new ckpb1.modules.player.Teleport();
        modules.register(teleport);
        teleport.setKeyCode(GLFW.GLFW_KEY_J);

        // ---- World ------------------------------------------------------
        modules.register(new ckpb1.modules.world.ResourceAssistant());

        // ---- Utility ----------------------------------------------------
        modules.register(new ckpb1.modules.util.AutoClicker());
        modules.register(new ckpb1.modules.util.AutoTool());

        // ---- BedWars ----------------------------------------------------
        modules.register(new ckpb1.modules.bedwars.BedDestroyerV1());
        modules.register(new ckpb1.modules.bedwars.BedDestroyerV2());
        ckpb1.modules.bedwars.BridgeAssistant bridge = new ckpb1.modules.bedwars.BridgeAssistant();
        modules.register(bridge);
        bridge.setKeyCode(GLFW.GLFW_KEY_B);

        // ---- Automation -------------------------------------------------
        modules.register(new ckpb1.modules.automation.BedWarsAutomation());

        // ---- HUD elements -----------------------------------------------
        hud.register(new WatermarkElement());
        hud.register(new FpsElement());
        hud.register(new PingElement());
        hud.register(new CpsElement());
        hud.register(new CoordinatesElement());
        hud.register(new KeystrokesElement());
        hud.register(new ArmorStatusElement());
        hud.register(new PotionStatusElement());
        hud.register(new TargetHudElement());
        hud.register(new SessionElement());
        hud.register(new BedWarsStatusElement());

        // default-on HUD elements for a good first run
        setDefaultEnabled("Watermark", true);
        setDefaultEnabled("FPS", true);
        setDefaultEnabled("CPS", true);
        setDefaultEnabled("Coordinates", true);
        setDefaultEnabled("Keystrokes", true);
        setDefaultEnabled("Session Info", true);
        setDefaultEnabled("Target HUD", true);

        // load persisted state (module settings + HUD layout)
        modules.loadAll();
        hud.loadLayout();

        // global listeners
        ClientEvents.addTick(this::tick);
        ClientEvents.addKey(modules);
        ClientEvents.addChat(CommandProcessor::handle);

        initialized = true;
        CKLog.info(CKPB1.fullName() + " initialized with " + modules.all().size() + " modules");
    }

    private static void setDefaultEnabled(String moduleName, boolean enabled) {
        var m = modules.byName(moduleName);
        if (m != null && !m.isEnabled()) {
            m.setEnabled(enabled, false);
        }
    }

    private void tick(MinecraftClient mc) {
        if (!initialized || mc == null) {
            return;
        }
        Stats.tick(mc);
        CombatState.tick(mc);
        modules.onClientTick(mc);
    }

    // ------------------------------------------------- mixin event plumbing

    public static void onClientTickEvent() {
        if (!initialized) {
            return;
        }
        ClientEvents.fireTick(MinecraftClient.getInstance());
    }

    public static void onHudRenderEvent(DrawContext context) {
        if (!initialized) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null || mc.player == null) {
            return;
        }
        if (mc.options.hudHidden) {
            return;
        }
        hud.render(context);
        ClientEvents.fireHudRender(context);
        StatusOverlay.render(context);
    }

    public static void onWorldRenderEvent(MatrixStack matrices, Camera camera) {
        if (!initialized) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            return;
        }
        ClientEvents.fireWorldRender(matrices, camera);
        for (var module : modules.all()) {
            if (module.isEnabled()) {
                try {
                    module.onWorldRender(matrices, camera);
                } catch (Exception e) {
                    CKLog.warn("Module " + module.getName() + " threw in onWorldRender: " + e);
                }
            }
        }
    }

    public static boolean onKeyEvent(int key, int scancode, int action, int modifiers) {
        if (!initialized) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (mc.currentScreen instanceof ClickGuiScreen) {
                mc.setScreen(null);
            } else if (mc.currentScreen == null) {
                mc.setScreen(clickGui());
            }
        }
        return ClientEvents.fireKey(key, scancode, action, modifiers);
    }

    public static void onMouseButtonEvent(int button, int action) {
        if (!initialized) {
            return;
        }
        CpsTracker.onClick(button, action);
        ClientEvents.fireMouse(button, action);
    }

    public static void onAttackEntityEvent(PlayerEntity attacker, Entity target) {
        if (!initialized) {
            return;
        }
        CombatState.onAttackEntity(attacker, target);
        ClientEvents.fireAttack(attacker, target);
    }

    public static boolean onChatEvent(String message) {
        return initialized && ClientEvents.fireChat(message);
    }
}
