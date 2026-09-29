package ckpb1.client.mixin;

import ckpb1.modules.player.NoFallDamage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cancels fall damage for the local player while the No Fall Damage module is
 * active (fully effective in singleplayer where the mixin also applies to the
 * integrated server; visual-only on remote servers).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void ckpb1$handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (NoFallDamage.shouldCancelFall((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
