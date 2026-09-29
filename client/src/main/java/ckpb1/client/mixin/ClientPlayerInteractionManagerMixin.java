package ckpb1.client.mixin;

import ckpb1.client.CKPB1Client;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Attack hook feeding reach/combo/hit info and kill tracking. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void ckpb1$beforeAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        CKPB1Client.onAttackEntityEvent(player, target);
    }
}
