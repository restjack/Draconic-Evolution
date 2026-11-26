package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.brandonscore.client.shader.BCShader;
import com.brandon3055.brandonscore.client.shader.BCShaders;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import java.util.EnumMap;
import java.util.Map;

/** Material variants preserve the original alpha-zero program and each pass's blend/depth policy. */
public final class TileRenderPipelines {
    private TileRenderPipelines() {}
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("draconicevolution", path); }
    private static final AlphaZero PYLON = new AlphaZero("pylon_shell", BlendFunction.LIGHTNING, false);
    public static final RenderType PYLON_SHELL = PYLON.renderType("de_pylon_shell", id("textures/block/pylon_sphere_texture.png"), VertexFormat.Mode.QUADS);
    private static final AlphaZero OUTER = new AlphaZero("core_outer", BlendFunction.TRANSLUCENT, true);
    private static final AlphaZero STAB_INNER = new AlphaZero("core_stab_inner", null, true);
    private static final AlphaZero STAB_OUTER = new AlphaZero("core_stab_outer", BlendFunction.TRANSLUCENT, true);


    public static final RenderType CORE_OUTER = OUTER.renderType("de_core_outer", id("textures/block/energy_core/energy_core_overlay.png"), VertexFormat.Mode.QUADS);
    public static final RenderType CORE_STAB_INNER = STAB_INNER.renderType("de_core_stab_inner", id("textures/block/energy_core/stabilizer_sphere.png"), VertexFormat.Mode.QUADS);
    public static final RenderType CORE_STAB_OUTER = STAB_OUTER.renderType("de_core_stab_outer", id("textures/block/energy_core/stabilizer_sphere.png"), VertexFormat.Mode.QUADS);
    private static final RenderPipeline BEAM_PIPELINE = texturePipeline("core_beam", VertexFormat.Mode.QUADS, false, true);
    private static final RenderPipeline OUTER_BEAM_PIPELINE = texturePipeline("core_beam_outer", VertexFormat.Mode.TRIANGLE_STRIP, true, false);
    public static final RenderType CORE_BEAM = textureType("de_core_beam", BEAM_PIPELINE, "textures/block/energy_core/stabilizer_beam.png");
    /** A strip must own its buffer; sharing a RenderType would join distinct stabilizer beams. */
    public static RenderType newOuterBeam() {
        return textureType("de_core_beam_outer", OUTER_BEAM_PIPELINE, "textures/block/energy_core/stabilizer_beam.png");
    }
    private static RenderPipeline texturePipeline(String name, VertexFormat.Mode mode, boolean blend, boolean depthWrite) {
        return RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                .withLocation(id("pipeline/" + name))
                .withVertexShader(id("core/tile_texture")).withFragmentShader(id("core/tile_texture"))
                .withSampler("Sampler0").withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, mode)
                .withColorTargetState(blend ? new ColorTargetState(BlendFunction.TRANSLUCENT) : ColorTargetState.DEFAULT)
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, depthWrite)).withCull(true).build();
    }
    private static RenderType textureType(String name, RenderPipeline pipeline, String texture) {
        return RenderType.create(name, RenderSetup.builder(pipeline).withTexture("Sampler0", id(texture)).createRenderSetup());
    }
    private static final RenderPipeline CELESTIAL_PIPELINE = texturePipeline("celestial", VertexFormat.Mode.QUADS, true, true);
    public static final RenderType CELESTIAL_WEATHER = textureType("de_celestial_weather", CELESTIAL_PIPELINE, "textures/block/crafting/fusion_particle.png");
    public static final RenderType CELESTIAL_TIME = textureType("de_celestial_time", CELESTIAL_PIPELINE, "textures/particle/todo/celestial_manipulator.png");
    private static final RenderPipeline CHAOS = RenderPipelines.CUTOUT_BLOCK.toBuilder()
            .withLocation(id("pipeline/chaos_surface"))
            .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.TRIANGLES)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build();
    public static final RenderType CHAOS_SURFACE = RenderType.create("de_chaos_surface", RenderSetup.builder(CHAOS)
            .withTexture("Sampler0", id("textures/block/chaos_crystal.png")).useLightmap().createRenderSetup());

    private static final RenderPipeline FUSION_PARTICLE_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/fusion_particles"))
            .withVertexShader(id("core/tile_unlit_lightmap")).withFragmentShader(id("core/tile_unlit_lightmap"))
            .withSampler("Sampler0").withSampler("Sampler2")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false).withDepthStencilState(DepthStencilState.DEFAULT).build();
    public static final RenderType FUSION_PARTICLES = RenderType.create("de_fusion_particles", RenderSetup.builder(FUSION_PARTICLE_PIPELINE)
            .withTexture("Sampler0", net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_PARTICLES).useLightmap().createRenderSetup());

    private static final RenderPipeline STAB_ACTIVE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(id("pipeline/stabilizer_active"))
            .withVertexShader(id("core/tile_unlit_lightmap")).withFragmentShader(id("core/tile_unlit_lightmap"))
            .withSampler("Sampler0").withSampler("Sampler2")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withCull(true).withDepthStencilState(DepthStencilState.DEFAULT).build();
    public static final RenderType STABILIZER_ACTIVE = RenderType.create("de_stabilizer_active", RenderSetup.builder(STAB_ACTIVE)
            .withTexture("Sampler0", id("textures/block/energy_core/stabilizer_large.png")).useLightmap().createRenderSetup());
    private static final RenderPipeline GLOW = RenderPipelines.ENTITY_SOLID.toBuilder().withLocation(id("pipeline/reactor_glow"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING)).build();
    public static final RenderType STAB_GLOW = glow("stabilizer");
    public static final RenderType INJECTOR_GLOW = glow("injector");
    private static RenderType glow(String part) {
        return RenderType.create("de_reactor_" + part + "_glow", RenderSetup.builder(GLOW)
                .withTexture("Sampler0", id("textures/block/reactor/reactor_" + part + ".png"))
                .useLightmap().useOverlay().createRenderSetup());
    }

    private static final class AlphaZero extends BCShader<AlphaZero> {
        private final String name;
        private final BlendFunction blend;
        private final boolean depthWrite;
        private final Map<VertexFormat.Mode, RenderPipeline> pipelines = new EnumMap<>(VertexFormat.Mode.class);
        AlphaZero(String name, BlendFunction blend, boolean depthWrite) {
            super(Identifier.fromNamespaceAndPath("brandonscore", "position_color_tex_alpha0"), DefaultVertexFormat.POSITION_TEX_COLOR);
            this.name = name; this.blend = blend; this.depthWrite = depthWrite;
        }
        @Override
        public RenderPipeline pipeline(VertexFormat.Mode mode) {
            return pipelines.computeIfAbsent(mode, m -> BCShaders.POS_COLOUR_TEX_ALPHA0.pipeline(m).toBuilder()
                    .withLocation(id("pipeline/" + name + "_" + m.name().toLowerCase(java.util.Locale.ROOT)))
                    // BC's vertex shader ignores ModelMat, which ExtractedModel uses to place shader geometry.
                    .withVertexShader(id("core/tile_alpha0_model"))
                    .withColorTargetState(blend == null ? ColorTargetState.DEFAULT : new ColorTargetState(blend))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, depthWrite)).withCull(true).build());
        }
    }

    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PYLON.pipeline(VertexFormat.Mode.QUADS));
        for (AlphaZero shader : new AlphaZero[]{OUTER, STAB_INNER, STAB_OUTER}) event.registerPipeline(shader.pipeline(VertexFormat.Mode.QUADS));
        event.registerPipeline(BEAM_PIPELINE);
        event.registerPipeline(OUTER_BEAM_PIPELINE);
        event.registerPipeline(CHAOS);
        event.registerPipeline(STAB_ACTIVE);
        event.registerPipeline(GLOW);
        event.registerPipeline(CELESTIAL_PIPELINE);
        com.brandon3055.draconicevolution.client.render.effect.RenderEnergyBolt.register(event);
        com.brandon3055.draconicevolution.client.render.entity.DraconicGuardianRenderer.registerPipelines(event);
        event.registerPipeline(FUSION_PARTICLE_PIPELINE);
        ExtractedBuildGuide.register(event);
    }
}
