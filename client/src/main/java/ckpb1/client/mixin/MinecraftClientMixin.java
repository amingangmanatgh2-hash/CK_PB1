package ckpb1.client.mixin;

import ckpb1.client.CKPB1Client;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fans the client tick out to the CK_PB1 event bus. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void ckpb1$onClientTick(CallbackInfo ci) {
        CKPB1Client.onClientTickEvent();
    }
}
