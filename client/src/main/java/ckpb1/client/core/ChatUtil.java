package ckpb1.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Small helper for sending CK_PB1 chat/actionbar messages. */
public final class ChatUtil {

    private ChatUtil() {
    }

    public static final String PREFIX = "§b[CK_PB1]§r ";

    /** Sends a chat message to the local player (never to the server). */
    public static void message(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.sendMessage(Text.literal(PREFIX + text), false);
        }
    }

    /** Shows text on the action bar overlay. */
    public static void overlay(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.sendMessage(Text.literal(PREFIX + text), true);
        }
    }
}
