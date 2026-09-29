package ckpb1.client.mixin;

import ckpb1.client.CKPB1Client;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Intercepts outgoing chat for local CK_PB1 commands (.ckpb1 / .ck). */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void ckpb1$onSendChatMessage(String content, CallbackInfo ci) {
        if (CKPB1Client.onChatEvent(content)) {
            ci.cancel();
        }
    }
}
