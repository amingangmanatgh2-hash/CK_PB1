package ckpb1.client.mixin;

import ckpb1.client.CKPB1Client;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** World-space overlay rendering hook (paths, boxes, highlights). */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void ckpb1$afterWorldRender(MatrixStack matrices, float tickDelta, long limitTime,
                                        boolean blockOutlines, Camera camera, GameRenderer gameRenderer,
                                        LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix,
                                        CallbackInfo ci) {
        // render while the depth state is still usable for our line drawing
        RenderSystem.depthFunc(515);
        CKPB1Client.onWorldRenderEvent(matrices, camera);
    }
}
