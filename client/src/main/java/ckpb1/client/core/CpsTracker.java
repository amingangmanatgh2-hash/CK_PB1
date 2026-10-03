package ckpb1.client.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Global clicks-per-second tracker (left/right mouse buttons).
 * Fed by the mouse mixin, read by the CPS HUD element and keystrokes.
 */
public final class CpsTracker {

    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    private CpsTracker() {
    }

    public static void onClick(int button, int action) {
        if (action != 1) { // GLFW_PRESS only
            return;
        }
        long now = System.currentTimeMillis();
        if (button == 0) {
            LEFT.addLast(now);
        } else if (button == 1) {
            RIGHT.addLast(now);
        }
        trim();
    }

    private static void trim() {
        long cutoff = System.currentTimeMillis() - 1000;
        while (!LEFT.isEmpty() && LEFT.peekFirst() < cutoff) LEFT.pollFirst();
        while (!RIGHT.isEmpty() && RIGHT.peekFirst() < cutoff) RIGHT.pollFirst();
    }

    public static int leftCps() {
        trim();
        return LEFT.size();
    }

    public static int rightCps() {
        trim();
        return RIGHT.size();
    }

    public static boolean leftDown() {
        return net.minecraft.client.MinecraftClient.getInstance().options.attackKey.isPressed();
    }

    public static boolean rightDown() {
        return net.minecraft.client.MinecraftClient.getInstance().options.useKey.isPressed();
    }
}
