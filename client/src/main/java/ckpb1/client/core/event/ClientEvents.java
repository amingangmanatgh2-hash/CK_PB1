package ckpb1.client.core.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CK_PB1 client event bus. Events are dispatched from mixins (see
 * ckpb1.client.mixin) and the module manager fans them out to modules.
 */
public final class ClientEvents {

    private ClientEvents() {
    }

    public interface TickListener {
        void onClientTick(MinecraftClient mc);
    }

    public interface HudRenderListener {
        void onHudRender(DrawContext context);
    }

    public interface WorldRenderListener {
        void onWorldRender(MatrixStack matrices, Camera camera);
    }

    /** @return true if the event is fully consumed and vanilla should not process it. */
    public interface KeyListener {
        boolean onKey(int key, int scancode, int action, int modifiers);
    }

    public interface MouseListener {
        void onMouseButton(int button, int action);
    }

    public interface AttackListener {
        void onAttackEntity(PlayerEntity attacker, Entity target);
    }

    /** @return true if the chat message was handled as a CK_PB1 command. */
    public interface ChatListener {
        boolean onChatMessage(String message);
    }

    private static final List<TickListener> TICK = new CopyOnWriteArrayList<>();
    private static final List<HudRenderListener> HUD = new CopyOnWriteArrayList<>();
    private static final List<WorldRenderListener> WORLD = new CopyOnWriteArrayList<>();
    private static final List<KeyListener> KEY = new CopyOnWriteArrayList<>();
    private static final List<MouseListener> MOUSE = new CopyOnWriteArrayList<>();
    private static final List<AttackListener> ATTACK = new CopyOnWriteArrayList<>();
    private static final List<ChatListener> CHAT = new CopyOnWriteArrayList<>();

    public static void addTick(TickListener l) {
        TICK.add(l);
    }

    public static void addHud(HudRenderListener l) {
        HUD.add(l);
    }

    public static void addWorld(WorldRenderListener l) {
        WORLD.add(l);
    }

    public static void addKey(KeyListener l) {
        KEY.add(l);
    }

    public static void addMouse(MouseListener l) {
        MOUSE.add(l);
    }

    public static void addAttack(AttackListener l) {
        ATTACK.add(l);
    }

    public static void addChat(ChatListener l) {
        CHAT.add(l);
    }

    public static void fireTick(MinecraftClient mc) {
        for (TickListener l : TICK) l.onClientTick(mc);
    }

    public static void fireHudRender(DrawContext context) {
        for (HudRenderListener l : HUD) l.onHudRender(context);
    }

    public static void fireWorldRender(MatrixStack matrices, Camera camera) {
        for (WorldRenderListener l : WORLD) l.onWorldRender(matrices, camera);
    }

    /** @return true when any listener consumed the key. */
    public static boolean fireKey(int key, int scancode, int action, int modifiers) {
        boolean consumed = false;
        for (KeyListener l : KEY) {
            if (l.onKey(key, scancode, action, modifiers)) {
                consumed = true;
            }
        }
        return consumed;
    }

    public static void fireMouse(int button, int action) {
        for (MouseListener l : MOUSE) l.onMouseButton(button, action);
    }

    public static void fireAttack(PlayerEntity attacker, Entity target) {
        for (AttackListener l : ATTACK) l.onAttackEntity(attacker, target);
    }

    public static boolean fireChat(String message) {
        for (ChatListener l : CHAT) {
            if (l.onChatMessage(message)) {
                return true;
            }
        }
        return false;
    }
}
