package com.brandon3055.draconicevolution.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * eli.8 Core Monitor: "show me where it is" highlight, like AE2 Extended's wireless hub. A pulsing box around a block
 * (a pylon, or the core) that draws through walls at any distance, for 30 seconds or until toggled off. Client only;
 * the chat waypoint and particle column from the server still happen as well.
 */
public final class MonitorHighlight {
    private static final int DURATION = 20 * 30;

    private static final RenderPipeline FILL_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("draconicevolution", "pipeline/monitor_highlight_fill"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withCull(false).withDepthStencilState(Optional.empty()).build();
    private static final RenderPipeline LINE_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("draconicevolution", "pipeline/monitor_highlight_lines"))
            .withDepthStencilState(Optional.empty()).build();
    private static final RenderType FILL = RenderType.create("de_monitor_highlight_fill", RenderSetup.builder(FILL_PIPELINE).createRenderSetup());
    private static final RenderType LINES = RenderType.create("de_monitor_highlight_lines", RenderSetup.builder(LINE_PIPELINE).createRenderSetup());

    private record Mark(String dim, BlockPos pos, long until, int rgb) {}

    private static final List<Mark> MARKS = new ArrayList<>();

    private MonitorHighlight() {
    }

    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    public static boolean isOn(String dim, BlockPos pos) {
        long t = now();
        return MARKS.stream().anyMatch(m -> m.pos.equals(pos) && m.dim.equals(dim) && m.until > t);
    }

    /** Turns the highlight on (30 s) or off. Returns true if it is now on. */
    public static boolean toggle(String dim, BlockPos pos, int rgb) {
        if (isOn(dim, pos)) {
            MARKS.removeIf(m -> m.pos.equals(pos) && m.dim.equals(dim));
            return false;
        }
        MARKS.add(new Mark(dim, pos.immutable(), now() + DURATION, rgb));
        return true;
    }

    /** Called from the level render stage (after translucent particles). */
    public static void render(PoseStack pose, float partialTicks) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || MARKS.isEmpty()) return;
        long t = now();
        MARKS.removeIf(m -> m.until <= t);
        String dim = mc.level.dimension().identifier().toString();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
        float pulse = 0.6F + 0.4F * (float) Math.sin((t + partialTicks) * Math.PI / 15); // 1.5 s
        for (Mark m : MARKS) {
            if (!m.dim.equals(dim)) continue;
            pose.pushPose();
            pose.translate(m.pos.getX() - cam.x, m.pos.getY() - cam.y, m.pos.getZ() - cam.z);
            float a = -0.03F, b = 1.03F;
            int fill = ((int) (0x40 * pulse) << 24) | m.rgb;
            box(source.getBuffer(FILL), pose.last(), a, b, fill);
            source.endBatch(FILL);
            edges(source.getBuffer(LINES), pose.last(), a, b, ((int) (0xFF * pulse) << 24) | m.rgb);
            source.endBatch(LINES);
            pose.popPose();
        }
    }

    private static void box(VertexConsumer vc, PoseStack.Pose p, float a, float b, int c) {
        float[][][] faces = {
                {{a, a, a}, {b, a, a}, {b, a, b}, {a, a, b}}, {{a, b, a}, {a, b, b}, {b, b, b}, {b, b, a}},
                {{a, a, a}, {a, b, a}, {b, b, a}, {b, a, a}}, {{a, a, b}, {b, a, b}, {b, b, b}, {a, b, b}},
                {{a, a, a}, {a, a, b}, {a, b, b}, {a, b, a}}, {{b, a, a}, {b, b, a}, {b, b, b}, {b, a, b}}};
        for (float[][] f : faces) for (float[] v : f) vc.addVertex(p, v[0], v[1], v[2]).setColor(c);
    }

    private static void edges(VertexConsumer vc, PoseStack.Pose p, float a, float b, int c) {
        float[] s = {a, b};
        for (float y : s) for (float z : s) line(vc, p, a, y, z, b, y, z, c);
        for (float x : s) for (float z : s) line(vc, p, x, a, z, x, b, z, c);
        for (float x : s) for (float y : s) line(vc, p, x, y, a, x, y, b, c);
    }

    private static void line(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1, int c) {
        float nx = x1 - x0, ny = y1 - y0, nz = z1 - z0, len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len; ny /= len; nz /= len;
        vc.addVertex(p, x0, y0, z0).setColor(c).setNormal(p, nx, ny, nz).setLineWidth(3F);
        vc.addVertex(p, x1, y1, z1).setColor(c).setNormal(p, nx, ny, nz).setLineWidth(3F);
    }
}
