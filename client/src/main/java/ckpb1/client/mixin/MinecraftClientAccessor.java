package ckpb1.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accessor for the private vanilla item-use cooldown (Fast Place module). */
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Accessor("itemUseCooldown")
    void ckpb1$setItemUseCooldown(int value);
}
