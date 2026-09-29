package ckpb1.client.mixin;

import ckpb1.client.CKPB1Client;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mouse button events (CPS tracking, ...). */
@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void ckpb1$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        CKPB1Client.onMouseButtonEvent(button, action);
    }
}
