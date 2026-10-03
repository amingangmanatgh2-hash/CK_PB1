package ckpb1.client.mixin;

import ckpb1.client.CKPB1Client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HUD render hook + vanilla crosshair replacement for the Crosshair module. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void ckpb1$afterHudRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        CKPB1Client.onHudRenderEvent(context);
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void ckpb1$replaceCrosshair(DrawContext context, CallbackInfo ci) {
        if (ckpb1.modules.render.CrosshairModule.shouldReplaceVanillaCrosshair()) {
            ci.cancel();
        }
    }
}
