package ckpb1.modules.utility;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Anti AFK: performs small human-like actions at random intervals so you are
 * not kicked for idling (simplified LiquidBounce "AntiAFK"). Rotate smoothly
 * turns the camera, Sneak taps sneak briefly, Mixed alternates both.
 */
public final class AntiAfk extends Module {

    public final ModeSetting mode = add(new ModeSetting("Mode",
            "What to do when the idle timer fires", "Mixed", "Rotate", "Sneak"));
    public final NumberSetting minSeconds = add(new NumberSetting("Min Interval",
            "Shortest time between actions", 25, 5, 120, 1, "s"));
    public final NumberSetting maxSeconds = add(new NumberSetting("Max Interval",
            "Longest time between actions", 60, 5, 300, 1, "s"));

    private long nextActionAt;
    private boolean sneaking;
    private int sneakTicksLeft;
    private float targetYaw;
    private boolean rotating;

    public AntiAfk() {
        super("Anti AFK", "Random small actions so you are not kicked for idling", Category.UTILITY);
    }

    @Override
    protected void onEnable() {
        scheduleNext();
    }

    @Override
    protected void onDisable() {
        stopActions();
    }

    private void scheduleNext() {
        int lo = Math.max(1, minSeconds.getInt());
        int hi = Math.max(lo, maxSeconds.getInt());
        nextActionAt = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(lo, hi + 1) * 1000L;
        stopActions();
    }

    private void stopActions() {
        sneaking = false;
        sneakTicksLeft = 0;
        rotating = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.input.sneaking = false;
        }
    }

    private void startAction(MinecraftClient mc) {
        boolean rotate = switch (mode.get()) {
            case "Rotate" -> true;
            case "Sneak" -> false;
            default -> ThreadLocalRandom.current().nextBoolean(); // Mixed
        };
        if (rotate) {
            targetYaw = mc.player.getYaw() + ThreadLocalRandom.current().nextFloat(-55f, 55f);
            rotating = true;
        } else {
            sneaking = true;
            sneakTicksLeft = 8;
        }
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!rotating && !sneaking && System.currentTimeMillis() >= nextActionAt) {
            startAction(mc);
        }
        if (rotating) {
            float delta = net.minecraft.util.math.MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
            if (Math.abs(delta) < 3f) {
                rotating = false;
                scheduleNext();
            } else {
                mc.player.setYaw(mc.player.getYaw() + Math.signum(delta) * Math.min(6f, Math.abs(delta)));
            }
        } else if (sneaking) {
            mc.player.input.sneaking = true;
            if (--sneakTicksLeft <= 0) {
                mc.player.input.sneaking = false;
                sneaking = false;
                scheduleNext();
            }
        }
    }
}
