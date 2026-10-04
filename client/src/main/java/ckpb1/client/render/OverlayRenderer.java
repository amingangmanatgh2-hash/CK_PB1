package ckpb1.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

/**
 * Lightweight world-space overlay rendering (lines & boxes) for CK_PB1
 * modules: bed highlights, bridge path preview, resource targets, ...
 */
public final class OverlayRenderer {

    private OverlayRenderer() {
    }

    private static void setup() {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
    }

    private static void teardown() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Draws a single line between two world positions. Color format 0xAARRGGBB. */
    public static void line(MatrixStack matrices, Camera camera, Vec3d from, Vec3d to, int argb) {
        setup();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        MatrixStack.Entry entry = matrices.peek();
        Vec3d a = toCamera(camera, from);
        Vec3d b = toCamera(camera, to);
        emitVertex(buffer, entry, a, argb);
        emitVertex(buffer, entry, b, argb);
        tessellator.draw();
        teardown();
    }

    /** Draws a poly-line through the given world positions. */
    public static void path(MatrixStack matrices, Camera camera, Iterable<Vec3d> points, int argb) {
        Vec3d prev = null;
        for (Vec3d p : points) {
            if (prev != null) {
                line(matrices, camera, prev, p, argb);
            }
            prev = p;
        }
    }

    /** Draws a box outline (12 edges) around a world-space box. */
    public static void box(MatrixStack matrices, Camera camera, Box box, int argb) {
        Vec3d min = new Vec3d(box.minX, box.minY, box.minZ);
        Vec3d max = new Vec3d(box.maxX, box.maxY, box.maxZ);
        Vec3d[] corners = {
                min,
                new Vec3d(max.x, min.y, min.z),
                new Vec3d(max.x, min.y, max.z),
                new Vec3d(min.x, min.y, max.z),
                new Vec3d(min.x, max.y, min.z),
                new Vec3d(max.x, max.y, min.z),
                max,
                new Vec3d(min.x, max.y, max.z)
        };
        // bottom square
        line(matrices, camera, corners[0], corners[1], argb);
        line(matrices, camera, corners[1], corners[2], argb);
        line(matrices, camera, corners[2], corners[3], argb);
        line(matrices, camera, corners[3], corners[0], argb);
        // top square
        line(matrices, camera, corners[4], corners[5], argb);
        line(matrices, camera, corners[5], corners[6], argb);
        line(matrices, camera, corners[6], corners[7], argb);
        line(matrices, camera, corners[7], corners[4], argb);
        // verticals
        line(matrices, camera, corners[0], corners[4], argb);
        line(matrices, camera, corners[1], corners[5], argb);
        line(matrices, camera, corners[2], corners[6], argb);
        line(matrices, camera, corners[3], corners[7], argb);
    }

    private static Vec3d toCamera(Camera camera, Vec3d worldPos) {
        return worldPos.subtract(camera.getPos());
    }

    private static void emitVertex(BufferBuilder buffer, MatrixStack.Entry entry, Vec3d pos, int argb) {
        Vector4f transformed = new Vector4f((float) pos.x, (float) pos.y, (float) pos.z, 1.0f);
        transformed.mul(entry.getPositionMatrix());
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        buffer.vertex(transformed.x, transformed.y, transformed.z).color(r, g, b, a).next();
    }
}
