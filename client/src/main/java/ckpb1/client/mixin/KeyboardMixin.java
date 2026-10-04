package ckpb1.client.mixin;

import ckpb1.client.CKPB1Client;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raw key events for CK_PB1 keybinds and GUI toggles. */
@Mixin(Keyboard.class)
public abstract class KeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void ckpb1$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (CKPB1Client.onKeyEvent(key, scancode, action, modifiers)) {
            ci.cancel();
        }
    }
}
