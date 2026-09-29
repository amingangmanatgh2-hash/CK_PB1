package ckpb1.modules.render;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/** Fullbright: raises gamma to see in the dark; restores the old value on disable. */
public final class Fullbright extends Module {

    private Double previousGamma;

    public Fullbright() {
        super("Fullbright", "Maximum brightness without torches", Category.RENDER);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) {
            return;
        }
        SimpleOption<Double> gamma = (SimpleOption<Double>) (SimpleOption<?>) mc.options.getGamma();
        previousGamma = gamma.getValue();
        gamma.setValue(16.0);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null || previousGamma == null) {
            return;
        }
        SimpleOption<Double> gamma = (SimpleOption<Double>) (SimpleOption<?>) mc.options.getGamma();
        gamma.setValue(previousGamma);
        previousGamma = null;
    }
}
