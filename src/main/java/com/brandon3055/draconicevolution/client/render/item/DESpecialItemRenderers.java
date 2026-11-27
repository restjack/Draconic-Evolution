package com.brandon3055.draconicevolution.client.render.item;

import com.brandon3055.brandonscore.client.render.ExtractedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import java.util.function.Consumer;

/** MC26 special model: stack state is copied at extraction; material/parameter batches stay separate. */
public final class DESpecialItemRenderers {
    private DESpecialItemRenderers() {}

    public record ChaosShardUnbaked() implements SpecialModelRenderer.Unbaked<ExtractedModel> {
        public static final MapCodec<ChaosShardUnbaked> MAP_CODEC = MapCodec.unit(new ChaosShardUnbaked());
        @Override
        public MapCodec<ChaosShardUnbaked> type() { return MAP_CODEC; }
        @Override
        public SpecialModelRenderer<ExtractedModel> bake(SpecialModelRenderer.BakingContext context) {
            // New OBJ instance per model bake, so resource reloads do not retain stale geometry.
            return new Renderer(new RenderItemChaosShard());
        }
    }

    public record EnergyCrystalUnbaked(String crystalType, int tier) implements SpecialModelRenderer.Unbaked<ExtractedModel> {
        public static final MapCodec<EnergyCrystalUnbaked> MAP_CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
                com.mojang.serialization.Codec.STRING.validate(value -> {
                    try { com.brandon3055.draconicevolution.blocks.energynet.EnergyCrystal.CrystalType.valueOf(value);
                        return com.mojang.serialization.DataResult.success(value);
                    } catch (IllegalArgumentException e) { return com.mojang.serialization.DataResult.error(() -> "Unknown crystal type: " + value); }
                }).fieldOf("crystal_type").forGetter(EnergyCrystalUnbaked::crystalType),
                com.mojang.serialization.Codec.intRange(0, 3).fieldOf("tier").forGetter(EnergyCrystalUnbaked::tier)
        ).apply(i, EnergyCrystalUnbaked::new));
        @Override
        public MapCodec<EnergyCrystalUnbaked> type() { return MAP_CODEC; }
        @Override
        public SpecialModelRenderer<ExtractedModel> bake(SpecialModelRenderer.BakingContext context) {
            return new Renderer(new RenderItemEnergyCrystal(
                    com.brandon3055.draconicevolution.blocks.energynet.EnergyCrystal.CrystalType.valueOf(crystalType),
                    com.brandon3055.brandonscore.api.TechLevel.values()[tier]));
        }
    }

    private record Renderer(DEItemRenderer renderer) implements SpecialModelRenderer<ExtractedModel> {
        @Override
        public ExtractedModel extractArgument(ItemStack stack) {
            ExtractedModel.Capture capture = new ExtractedModel.Capture();
            // No deferred access to the mutable ItemStack, clock or world. Geometry and shader
            // parameters are frozen now. These migrated renderers do not branch on context.
            renderer.renderItem(stack.copy(), ItemDisplayContext.NONE, new PoseStack(), capture, 0, 0);
            return capture.finish();
        }
        @Override
        public void submit(ExtractedModel geometry, PoseStack pose, SubmitNodeCollector collector,
                           int light, int overlay, boolean hasFoil, int outlineColor) {
            if (geometry == null) return;
            // Vanilla supplies lighting at submission. Preserve local geometry/material snapshots.
            new ExtractedModel(geometry.batches().stream().map(batch -> new ExtractedModel.Batch(batch.type(),
                    batch.vertices().stream().map(v -> new ExtractedModel.Vertex(v.x(), v.y(), v.z(), v.color(),
                            v.u(), v.v(), overlay, light, v.nx(), v.ny(), v.nz())).toList())).toList()).submit(pose, collector);
        }
        @Override
        public void getExtents(Consumer<Vector3fc> output) { renderer.getExtents(output); }
    }
}
