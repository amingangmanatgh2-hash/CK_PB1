package ckpb1.client.hud.elements;

import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

/** Ping HUD element (multiplayer). Shows a dash in singleplayer. */
public final class PingElement extends HudElement {

    public PingElement() {
        super("Ping", "Latency to the server", 6, 52);
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String text;
        int color = 0xB0B0B0;
        if (mc.player != null && mc.player.networkHandler != null && mc.getCurrentServerEntry() != null) {
            PlayerListEntry entry = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid());
            int ping = entry != null ? entry.getLatency() : -1;
            text = "Ping: " + (ping < 0 ? "..." : ping + "ms");
            color = ping < 0 || ping < 80 ? 0x55FF55 : ping < 200 ? 0xFFE055 : 0xFF5555;
        } else {
            text = "Ping: SP";
        }
        width = mc.textRenderer.getWidth(text) + 6;
        height = 12;
        drawPanel(context, width, height);
        text(context, text, 3, 2, color);
    }
}
