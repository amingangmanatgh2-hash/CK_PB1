package ckpb1.modules.utility;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import net.minecraft.client.MinecraftClient;

/**
 * Fast Place: removes the vanilla right-click delay between block placements
 * (Wurst "FastPlace", default key B there; here it is a plain toggle).
 */
public final class FastPlace extends Module {

    public FastPlace() {
        super("Fast Place", "Removes the block placement delay", Category.UTILITY);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return;
        }
        // the vanilla 4-tick item-use cooldown becomes 0 every tick
        ((ckpb1.client.mixin.MinecraftClientAccessor) mc).ckpb1$setItemUseCooldown(0);
    }
}
