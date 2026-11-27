package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.vec.*;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.machines.Grinder;
import com.brandon3055.draconicevolution.blocks.tileentity.TileGrinder;
import com.brandon3055.draconicevolution.init.DEContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.OptionalDouble;

/**
 * Created by brandon3055 on 3/11/19.
 */
public class RenderTileGrinder implements BlockEntityRenderer<TileGrinder, RenderTileGrinder.State> {
    private static final double[] ROTATION_MAP = new double[]{0, 180, 90, -90};
    private static final RenderType swordType = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/block/grinder.png"));
    private static final RenderType fanType = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/block/parts/machine_fan.png"));
    private static final RenderType aoeOutlineType = RenderTypes.linesTranslucent();
    private static final RenderType aoeSolidType = RenderTypes.debugQuads();

    private final CCModel swordModel;
    private final CCModel fanModel;


    public RenderTileGrinder(BlockEntityRendererProvider.Context context) {
        Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/grinder/grinder_fan.obj")).quads().ignoreMtl().parse();
        fanModel = CCModel.combine(map.values());

        map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/grinder/grinder_sword.obj")).quads().ignoreMtl().parse();
        swordModel = CCModel.combine(map.values());
        swordModel.computeNormals();
        swordModel.apply(new Scale(-1 / 16F));
        swordModel.apply(new Rotation(180 * MathHelper.torad, 0, 0, 1));
        swordModel.apply(new Rotation(90 * MathHelper.torad, 0, 1, 0));
    }

    public static class State extends BlockEntityRenderState {
        boolean valid;
        Direction facing = Direction.NORTH;
        Vector3 tilePos, targetA, targetB;
        double animA, animB, fanRotation;
        AABB killZone;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(TileGrinder tile, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        BlockState block = tile.getBlockState();
        state.valid = !tile.isRemoved() && block.is(DEContent.GRINDER.get());
        if (!state.valid) return;
        state.facing = block.getValue(Grinder.FACING);
        state.tilePos = Vector3.fromTileCenter(tile);
        state.targetA = tile.targetA == null ? null : getEntityMovingVec(tile.targetA, partialTicks);
        state.targetB = tile.targetB == null ? null : getEntityMovingVec(tile.targetB, partialTicks);
        state.animA = Math.min(tile.animA + partialTicks * tile.getAnimSpeed(), 1);
        state.animB = Math.min(tile.animB + partialTicks * tile.getAnimSpeed(), 1);
        state.fanRotation = tile.fanRotation + tile.fanSpeed * partialTicks;
        state.killZone = null;
        if (tile.aoeDisplay > 0.51) {
            tile.validateKillZone(true);
            if (tile.killZone != null) state.killZone = tile.killZone.move(-tile.getBlockPos().getX(), -tile.getBlockPos().getY(), -tile.getBlockPos().getZ())
                    .deflate(0.01).deflate(tile.aoe.get() - tile.aoeDisplay);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.valid) return;
        int light = state.lightCoords;
        Direction facing = state.facing;
        Vector3 tilePos = state.tilePos.copy();
        Vector3 targetA = state.targetA == null ? null : state.targetA.copy();
        Vector3 targetB = state.targetB == null ? null : state.targetB.copy();
        double animA = state.animA, animB = state.animB, fanRotation = state.fanRotation;
        collector.submitCustomGeometry(poseStack, swordType, (pose, vertices) -> {
            CCRenderState ccrs = CCRenderState.instance();
            ccrs.reset();
            ccrs.bind(new TransformingVertexConsumer(vertices, new Matrix4(pose)), swordType);
            ccrs.brightness = light;
            ccrs.overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
            renderSword(ccrs, facing, 0.34, tilePos, targetA, animA, 0);
            renderSword(ccrs, facing, -0.34, tilePos, targetB, animB, 0);
        });
        collector.submitCustomGeometry(poseStack, fanType, (pose, vertices) -> {
            CCRenderState ccrs = CCRenderState.instance();
            ccrs.reset();
            ccrs.bind(vertices, fanType);
            ccrs.brightness = light;
            ccrs.overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
            Matrix4 mat = new Matrix4(pose);
            mat.translate(Vector3.CENTER);
            mat.apply(new Rotation(facing.toYRot() * -MathHelper.torad, 0, 1, 0));
            mat.apply(new Scale(-0.0625));
            mat.apply(new Rotation(fanRotation, 0, 0, 1));
            com.brandon3055.draconicevolution.client.render.DEModelEmission.render(fanModel, ccrs, mat);
        });
        AABB box = state.killZone;
        if (box != null) {
            collector.submitCustomGeometry(poseStack, aoeOutlineType, (pose, vertices) -> {
                net.minecraft.world.phys.shapes.Shapes.create(box).forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                    org.joml.Vector3f normal = new org.joml.Vector3f((float)(x2-x1), (float)(y2-y1), (float)(z2-z1)).normalize();
                    vertices.addVertex(pose, (float)x1, (float)y1, (float)z1).setColor(0F, 0F, 0F, 1F).setNormal(pose, normal.x, normal.y, normal.z).setLineWidth(4F);
                    vertices.addVertex(pose, (float)x2, (float)y2, (float)z2).setColor(0F, 0F, 0F, 1F).setNormal(pose, normal.x, normal.y, normal.z).setLineWidth(4F);
                });
            });
            collector.submitCustomGeometry(poseStack, aoeSolidType, (pose, vertices) ->
                    RenderUtils.bufferCuboidSolid(new TransformingVertexConsumer(vertices, new Matrix4(pose)), new Cuboid6(box), 0F, 1F, 1F, 0.2F));
        }
    }

    private void renderSword(CCRenderState ccrs, Direction tileFacing, double sideOffset, Vector3 tilePos, Vector3 targetPos, double attackAnimTime, float partialTicks) {
        attackAnimTime *= 2;
        double yoyo = attackAnimTime > 1D ? 1D - (attackAnimTime - 1D) : attackAnimTime;
        sideOffset *= 1D - yoyo;
        double sideAngle = ROTATION_MAP[tileFacing.get3DDataValue() - 2] * MathHelper.torad;

        Transformation rotation = new Rotation(yoyo * Math.PI * -2.8, 1, 0, 0).at(new Vector3(0, -0.2, 0));
        Vector3 handPos = new Vector3(0.4975 + sideOffset, 0.5, 0.125 + (yoyo * (0.375)));
        handPos.apply(new Rotation(sideAngle, 0, 1, 0).at(new Vector3(0.5, 0, 0.5)));

        Transformation translation = new Translation(handPos);
        TransformationList transforms = rotation.with(translation);
        if (targetPos == null) {
            transforms = transforms.with(new Rotation(sideAngle, 0, 1, 0).at(handPos));
        } else {
            Vector3 targetVec = targetPos.copy().subtract(tilePos);
            double angle = Math.atan2(targetVec.x, targetVec.z) + Math.PI;
            transforms = transforms.with(new Rotation(angle, 0, 1, 0).at(handPos));
            transforms = transforms.with(new Translation(targetVec.multiply(yoyo)));
        }

        com.brandon3055.draconicevolution.client.render.DEModelEmission.render(swordModel, ccrs, transforms);
    }

    private Vector3 getEntityMovingVec(Entity entity, float partialTicks) {
        return new Vector3(entity.getPosition(partialTicks)).add(0, entity.getBbHeight() / 2D, 0);
    }

    @Override
    public AABB getRenderBoundingBox(TileGrinder blockEntity) {
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity).inflate(8);
    }
}
