package ckpb1.client.mixin;

import ckpb1.modules.utility.NoSlow;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * No Slow: while the module is active the "using item" movement slowdown in
 * the client player tick is skipped, so eating/shielding does not slow you.
 */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Redirect(method = "tickMovement",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean ckpb1$noSlowUsingItem(ClientPlayerEntity self) {
        if (NoSlow.shouldSkipSlowdown()) {
            return false;
        }
        return self.isUsingItem();
    }
}
