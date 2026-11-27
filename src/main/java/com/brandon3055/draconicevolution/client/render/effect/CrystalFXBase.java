package com.brandon3055.draconicevolution.client.render.effect;

import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.draconicevolution.api.energy.IENetEffectTile;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Created by brandon3055 on 29/11/2016.
 */
public abstract class CrystalFXBase<T extends BlockEntity & IENetEffectTile> extends Particle {

    // Particle no longer carries colour in MC26; these are DE effect parameters.
    protected float rCol = 1, gCol = 1, bCol = 1, alpha = 1;

    public abstract net.minecraft.resources.Identifier texture();
    public abstract void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, Camera camera, float partialTick);

    // CrystalFXRenderer owns rendering and ticking; never enqueue this group into ParticleEngine.
    @Override
    public final net.minecraft.client.particle.ParticleRenderType getGroup() {
        return net.minecraft.client.particle.ParticleRenderType.NO_RENDER;
    }

    public final boolean belongsTo(ClientLevel level) { return this.level == level && !tile.isRemoved(); }

    protected final T tile;
    protected int ticksTillDeath = 0;
    protected float fxState;
    public boolean renderEnabled = true;
    private int ttl = 10;

    public CrystalFXBase(ClientLevel worldIn, T tile) {
        super(worldIn, tile.getBlockPos().getX() + 0.5, tile.getBlockPos().getY() + 0.5, tile.getBlockPos().getZ() + 0.5);
        this.tile = tile;
        // Start alive: CrystalFXRenderer ticks new effects in the same client tick, before the tile has called updateFX.
        // With 0 here a re-created beam removed itself at once, the handler saw a dead beam and re-created all of them
        // again, forever, so once any beam died (pause, lag) a crystal never showed a flow beam again.
        this.ticksTillDeath = 4;
    }

    //    public CrystalGLFXBase(World worldIn, T tile) {
//        super(worldIn, Vec3D.getCenter(tile.getPos()));
//        this.tile = tile;
//        this.ticksTillDeath = 4;
//        this.texturesPerRow = 8;
//        this.baseScale = 1.5F;
//    }


    public void updateFX(float fxState) {
        this.fxState = fxState;
        ticksTillDeath = 4;
        if (ttl-- <= 0) {
            remove();
        }
    }

    @Override
    public void tick() {
        ttl = 10;
    }

    protected void setPosition(Vec3D pos) {
        setPos(pos.x, pos.y, pos.z);
    }

    protected Vector3f[] getRenderVectors(Camera renderInfo, float viewX, float viewY, float viewZ, float scale) {
        Quaternionf quaternion;
        quaternion = renderInfo.rotation();
        Vector3f vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
        vector3f1.rotate(quaternion);
        Vector3f[] renderVector = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F)};

        for (int i = 0; i < 4; ++i) {
            Vector3f vector3f = renderVector[i];
            vector3f.rotate(quaternion);
            vector3f.mul(scale);
            vector3f.add(viewX, viewY, viewZ);
        }
        return renderVector;
    }
}
