package ckpb1.client.hud.elements;

import ckpb1.client.core.Stats;
import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Session information HUD element: user, server, playtime, kills. */
public final class SessionElement extends HudElement {

    public SessionElement() {
        super("Session Info", "Username, server, playtime and session kills", 6, 300);
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        String[] lines = {
                "User: " + Stats.username(mc),
                "Server: " + Stats.server(),
                "Playtime: " + Stats.playtime(),
                "Kills: " + Stats.kills(),
        };
        int widest = 40;
        for (String line : lines) {
            widest = Math.max(widest, mc.textRenderer.getWidth(line));
        }
        width = widest + 6;
        height = lines.length * 11 + 4;
        drawPanel(context, width, height);
        int ly = 2;
        for (String line : lines) {
            text(context, line, 3, ly, 0xD0D0D0);
            ly += 11;
        }
    }
}
