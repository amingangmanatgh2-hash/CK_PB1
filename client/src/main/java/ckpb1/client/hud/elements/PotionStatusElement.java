package ckpb1.client.hud.elements;

import ckpb1.client.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Language;

import java.util.ArrayList;
import java.util.List;

/** Potion status HUD element: active effects with duration. */
public final class PotionStatusElement extends HudElement {

    public PotionStatusElement() {
        super("Potion Status", "Active status effects and remaining time", 200, 6);
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            String name = Language.getInstance().get(effect.getTranslationKey());
            if (name == null || name.startsWith("effect.")) {
                name = effect.getTranslationKey();
            }
            int amp = effect.getAmplifier() + 1;
            int ticks = effect.getDuration();
            String time = effect.isInfinite() ? "inf"
                    : String.format("%d:%02d", ticks / 1200, (ticks % 1200) / 20);
            lines.add(name + (amp > 1 ? " " + amp : "") + "  " + time);
        }
        if (lines.isEmpty()) {
            lines.add("No effects");
        }
        int widest = 40;
        for (String line : lines) {
            widest = Math.max(widest, mc.textRenderer.getWidth(line));
        }
        width = widest + 6;
        height = lines.size() * 11 + 4;
        drawPanel(context, width, height);
        int ly = 2;
        for (String line : lines) {
            text(context, line, 3, ly, 0xD8E8FF);
            ly += 11;
        }
    }
}
