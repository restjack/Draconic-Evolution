package com.brandon3055.draconicevolution.blocks.tileentity;

import codechicken.lib.colour.EnumColour;

import codechicken.lib.data.MCDataInput;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.draconicevolution.lib.TransactionalOPStorage;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.brandon3055.brandonscore.blocks.TileBCore;

import com.brandon3055.brandonscore.capability.CapabilityOP;

import com.brandon3055.brandonscore.client.particle.IntParticleType.IntParticleData;

import com.brandon3055.brandonscore.lib.Vec3D;

import com.brandon3055.brandonscore.lib.datamanager.*;

import com.brandon3055.brandonscore.utils.Utils;

import com.brandon3055.draconicevolution.blocks.StructureBlock;

import com.brandon3055.draconicevolution.blocks.machines.EnergyPylon.Mode;

import com.brandon3055.draconicevolution.blocks.machines.EnergyPylon;

import com.brandon3055.draconicevolution.client.DEParticles;

import com.brandon3055.draconicevolution.init.DEContent;

import java.util.LinkedList;

import java.util.List;

import net.minecraft.core.BlockPos;

import net.minecraft.core.Direction;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.util.RandomSource;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.phys.shapes.CollisionContext;

import net.minecraft.world.phys.shapes.Shapes;

import net.minecraft.world.phys.shapes.VoxelShape;



import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import net.neoforged.neoforge.common.Tags;

import org.jetbrains.annotations.Nullable;


import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent; // port-26.1.2: RenderHighlightEvent удалён


/**
 * Created by brandon3055 on 30/3/2016.
 */
public class TileEnergyPylon extends TileBCore implements MultiBlockController {
    private static final VoxelShape SPHERE_SHAPE = Shapes.box(0.2, 0.2, 0.2, 0.8, 0.8, 0.8);

    public final ManagedEnum<Mode> ioMode = register(new ManagedEnum<>("io_mode", Mode.OUTPUT, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedEnum<Direction> direction = register(new ManagedEnum<>("direction", Direction.UP, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedEnum<EnumColour> colour = register(new ManagedEnum<>("colour", EnumColour.class, null, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool structureValid = register(new ManagedBool("structure_valid", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedPos coreOffset = register(new ManagedPos("core_offset", (BlockPos) null, DataFlags.SAVE_NBT_SYNC_TILE));
    private final ManagedByte particleRate = register(new ManagedByte("particle_rate", DataFlags.SYNC_TILE));

    private TileEnergyCore core = null;
    private int coreSelection = 0;
    private int tick = 0;
    private int lastCompOverride = 0;

    private byte pendingParticleRate;
    private final SnapshotJournal<Byte> particleJournal = new SnapshotJournal<>() {
        @Override protected Byte createSnapshot() { return pendingParticleRate; }
        @Override protected void revertToSnapshot(Byte snapshot) { pendingParticleRate = snapshot; }
        @Override protected void onRootCommit(Byte original) { particleRate.set(pendingParticleRate); }
    };

    private static byte particleRateFor(long energy) {
        return (byte) Math.min(20, Math.max(1, energy / 500));
    }

    public IOPStorage opAdapter = new TransactionalOPStorage() {
        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            TileEnergyCore core = getCore();
            if (!canReceive() || core == null || !core.active.get()) return 0;
            return showTransfer(core.energy.insert(amount, transaction), transaction);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            TileEnergyCore core = getCore();
            if (!canExtract() || core == null || !core.active.get()) return 0;
            return showTransfer(core.energy.extract(amount, transaction), transaction);
        }

        private int showTransfer(int moved, TransactionContext transaction) {
            if (moved > 0) {
                particleJournal.updateSnapshots(transaction);
                pendingParticleRate = particleRateFor(moved);
            }
            return moved;
        }

        @Override
        public boolean canExtract() {
            return ioMode.get().canExtract();
        }

        @Override
        public boolean canReceive() {
            return ioMode.get().canReceive();
        }

        @Override
        public long receiveOP(long maxReceive, boolean simulate) {
            if (coreOffset.isNull() || !canReceive() || getCore() == null || !core.active.get()) {
                return 0;
            }
            long received = core.energy.receiveOP(maxReceive, simulate);
            if (!simulate && received > 0) {
                particleRate.set(particleRateFor(received));
            }
            return received;
        }

        @Override
        public long extractOP(long maxExtract, boolean simulate) {
            if (coreOffset.isNull() || !canExtract() || getCore() == null || !core.active.get()) {
                return 0;
            }
            long extracted = core.energy.extractOP(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                particleRate.set(particleRateFor(extracted));
            }
            return extracted;
        }

        @Override
        public long getOPStored() {
            return coreOffset.notNull() && getCore() != null ? core.energy.getOPStored() : 0;
        }

        @Override
        public long getMaxOPStored() {
            return coreOffset.notNull() && getCore() != null ? core.energy.getMaxOPStored() : 0;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return (int) receiveOP(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return (int) extractOP(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(getOPStored(), Integer.MAX_VALUE / 2);
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(getMaxOPStored(), Integer.MAX_VALUE);
        }

        @Override
        public long modifyEnergyStored(long amount) {
            return 0; //Invalid operation for this device
        }
    };


    public TileEnergyPylon(BlockPos pos, BlockState state) {
        super(DEContent.TILE_ENERGY_PYLON.get(), pos, state);
        capManager.set(CapabilityOP.OP, opAdapter);
        enableTileDebug();
    }

    public static void register(RegisterCapabilitiesEvent event) {
        com.brandon3055.brandonscore.capability.CapabilityOP.registerBlockEntity(event, DEContent.TILE_ENERGY_PYLON.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (!structureValid.get() || coreOffset.isNull() || getCore() == null || !core.active.get()) {
            return;
        }

        if (tick++ % 10 == 0 && opAdapter.getMaxOPStored() > 0) {
            updateComparators();
        }

        if (!level.isClientSide() && ioMode.get().canExtract()) {
            long extracted = core.energy.extractOP(sendEnergyToAll(core.energy.getUncappedStored(), core.energy.getUncappedStored()), false);
            if (extracted > 0) {
                particleRate.set(particleRateFor(extracted));
            }
        }

        if (level.isClientSide()) {
            spawnParticles();
        }

        if (!level.isClientSide() && (particleRate.get() > 1 || (particleRate.get() > 0 && level.getRandom().nextInt(2) == 0))) {
            particleRate.subtract((byte) 2);
        }
    }

    public void updateComparators() {
        int cOut = 0;
        if (getCore() != null) {
            cOut = (int) (((double) core.energy.getUncappedStored() / core.energy.getMaxOPStored()) * 15D);
        }
        if (cOut != lastCompOverride) {
            lastCompOverride = cOut;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }
    // ### Core Connection Handling

    @Override
    public InteractionResult handleRemoteClick(Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            ioMode.set(ioMode.get().reverse());
            level.setBlockAndUpdate(worldPosition, level.getBlockState(worldPosition).setValue(EnergyPylon.MODE, ioMode.get()));
            // eli.7: say which way energy now flows; "input/output" alone reads ambiguously.
            if (com.brandon3055.draconicevolution.DEConfig.extra("pylonModeMessage")) player.sendOverlayMessage(modeText(ioMode.get()));
        }
        return InteractionResult.SUCCESS;
    }

    /** Particle tint for undyed pylons (eli.7). Keep in step with the energy_pylon_input/output textures. */
    public static final int INPUT_RGB = 0x3CE65A;
    public static final int OUTPUT_RGB = 0xFF5A32;

    /** "Pylon: INPUT (charging core)" / "Pylon: OUTPUT (draining core)", coloured to match the textures. */
    public static net.minecraft.network.chat.Component modeText(Mode mode) {
        boolean in = mode == Mode.INPUT;
        return net.minecraft.network.chat.Component.literal("Pylon: " + (in ? "INPUT (charging core)" : "OUTPUT (draining core)"))
                .withColor(in ? INPUT_RGB : OUTPUT_RGB);
    }

    @Nullable
    public TileEnergyCore getCore() {
        BlockPos offset = coreOffset.get();
        if (offset == null || level == null) return null;
        BlockPos pos = worldPosition.subtract(offset);
        // Check before any lookup: level.getBlockEntity adds a chunk ticket on every call and kept the core's chunk loaded.
        if (!com.brandon3055.draconicevolution.lib.LoadedLookup.isLoaded(level, pos)) {
            core = null;
            return null;
        }
        if (core != null && !core.isRemoved() && core.getBlockPos().equals(pos)) return core;
        if (!level.isAreaLoaded(pos, 16)) return null;
        BlockEntity entity = com.brandon3055.draconicevolution.lib.LoadedLookup.blockEntity(level, pos);
        core = entity instanceof TileEnergyCore energyCore ? energyCore : null;
        if (core == null && !level.isClientSide()) coreOffset.set(null);
        return core;
    }

    private List<TileEnergyCore> findActiveCores() {
        List<TileEnergyCore> list = new LinkedList<>();
        int range = 18;
        Direction dir = direction.get();
        BlockPos offset = new BlockPos(dir.getStepX() * range, dir.getStepY() * range, dir.getStepZ() * range);
        BlockPos min = worldPosition.offset(-18, -18, -18).offset(offset);
        BlockPos max = worldPosition.offset(18, 18, 18).offset(offset);

        for (BlockPos blockPos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(blockPos).is(DEContent.ENERGY_CORE.get())) {
                if (level.getBlockEntity(blockPos) instanceof TileEnergyCore tile && tile.active.get()) {
                    list.add(tile);
                }
            }
        }

        return list;
    }

    public void selectNextCore() {
        if (level.isClientSide()) {
            return;
        }
        List<TileEnergyCore> cores = findActiveCores();
        if (cores.isEmpty()) {
            core = null;
            coreOffset.set(null);
            return;
        }

        if (coreSelection >= cores.size()) {
            coreSelection = 0;
        }

        TileEnergyCore selectedCore = cores.get(coreSelection);
        coreOffset.set(new BlockPos(worldPosition.subtract(selectedCore.getBlockPos())));
        core = selectedCore;
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        coreSelection++;
        detectAndSendChanges(false);
        drawParticleBeam();
    }
    // ### MultiBlock Handling


    @Override
    public boolean validateStructure() {
        debug("ValidateStructure, IsValid: " + structureValid.get());
        if (!structureValid.get()) {
            boolean found = false;
            for (Direction dir : Direction.values()) {
                BlockPos pos = worldPosition.relative(dir);
                BlockState testState = level.getBlockState(pos);
                if (testState.is(Tags.Blocks.GLASS_BLOCKS)) {
                    colour.set(getGlassColour(testState));
                    StructureBlock.buildingLock = true;
                    level.setBlockAndUpdate(worldPosition, level.getBlockState(worldPosition).setValue(EnergyPylon.FACING, dir));
                    level.setBlockAndUpdate(pos, DEContent.STRUCTURE_BLOCK.get().defaultBlockState());
                    StructureBlock.buildingLock = false;
                    if (level.getBlockEntity(pos) instanceof TileStructureBlock tile) {
                        tile.blockName.set(BuiltInRegistries.BLOCK.getKey(testState.getBlock()));
                        tile.setController(this);
                        direction.set(dir);
                        found = true;
                        break;
                    }
                }
            }

            debug("Glass block found: " + found);
            if (!found) {
                level.setBlockAndUpdate(worldPosition, level.getBlockState(worldPosition).setValue(EnergyPylon.FACING, Direction.UP));
                colour.set(null);
            }
        }

        structureValid.set(isStructureValid());
        debug("Validate Result: " + structureValid.get());
        if (structureValid.get() && coreOffset.isNull()) {
            selectNextCore();
        } else if (!structureValid.get() && coreOffset.notNull()) {
            coreOffset.set(null);
        }

        return structureValid.get();
    }

    @Override
    public boolean isStructureValid() {
        return isGlass(worldPosition.relative(direction.get()));
    }

    private boolean isGlass(BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileStructureBlock tile && tile.getOriginalBlock().defaultBlockState().is(Tags.Blocks.GLASS_BLOCKS);
    }

    @Override
    public void receivePacketFromServer(MCDataInput data, int id) {
        if (id == 0) drawParticleBeam();
    }
    // ### Rendering


    public void drawParticleBeam() {
        if (!level.isClientSide()) {
            sendPacketToChunk(mcDataOutput -> {}, 0);
            return;
        }
        if (getCore() == null) return;

        BlockPos thisPos = worldPosition.relative(direction.get());
        Vec3D coreVec = Vec3D.getDirectionVec(new Vec3D(thisPos).add(0.5, 0.5, 0.5), new Vec3D(core.getBlockPos()).add(0.5, 0.5, 0.5));
        double coreDistance = Utils.getDistance(new Vec3D(thisPos).add(0.5, 0.5, 0.5), Vec3D.getCenter(core.getBlockPos()));

        for (int i = 0; i < 100; i++) {
            double location = i / 100D;
            Vec3D particlePos = new Vec3D(thisPos).add(0.5, 0.5, 0.5);
            particlePos.add(coreVec.x * coreDistance * location, coreVec.y * coreDistance * location, coreVec.z * coreDistance * location);

            double speed = 0.02F;
            double offset = 0.2F;
            double randX = level.getRandom().nextDouble() - 0.5D;
            double randY = level.getRandom().nextDouble() - 0.5D;
            double randZ = level.getRandom().nextDouble() - 0.5D;
            particlePos.add(randX * offset, randY * offset, randZ * offset);

            level.addParticle(new IntParticleData(DEParticles.LINE_INDICATOR.get(), 150, 0, 255, 40 + level.getRandom().nextInt(20)), particlePos.x, particlePos.y, particlePos.z, randX * speed, randY * speed, randZ * speed);
        }
    }

    private void spawnParticles() {
        RandomSource rand = level.getRandom();
        if (getCore() == null || particleRate.get() <= 0) return;
        if (particleRate.get() > 20) particleRate.set((byte) 20);

        Vec3D spawn;
        Vec3D dest;

        // eli.7: undyed pylons tint their particles by mode (matches the side textures).
        int rgb = !com.brandon3055.draconicevolution.DEConfig.extra("pylonModeColours") ? 0x00C8FF : ioMode.get() == Mode.INPUT ? INPUT_RGB : OUTPUT_RGB; // 0x00C8FF = upstream colour
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        if (colour.notNull()) {
            r = (int) (colour.get().rF() * 255);
            g = (int) (colour.get().gF() * 255);
            b = (int) (colour.get().bF() * 255);
        }

        if (particleRate.get() > 10) {
            for (int i = 0; i <= particleRate.get() / 10; i++) {
                spawn = getParticleSpawn(rand);
                dest = getParticleDest(rand);
                level.addParticle(new IntParticleData(DEParticles.ENERGY.get(), r, g, b, 200), spawn.x, spawn.y, spawn.z, dest.x, dest.y, dest.z);

            }
        } else if (rand.nextInt(Math.max(1, 10 - particleRate.get())) == 0) {
            spawn = getParticleSpawn(rand);
            dest = getParticleDest(rand);
            level.addParticle(new IntParticleData(DEParticles.ENERGY.get(), r, g, b, 200), spawn.x, spawn.y, spawn.z, dest.x, dest.y, dest.z);
        }
    }

    private Vec3D getParticleSpawn(RandomSource random) {
        if (ioMode.get().canExtract()) {
            double range = core.tier.get();
            return new Vec3D(core.getBlockPos()).add((random.nextFloat() - 0.5F) * range, (random.nextFloat() - 0.5F) * range, (random.nextFloat() - 0.5F) * range);
        } else {
            return Vec3D.getCenter(worldPosition.relative(direction.get()));
        }
    }

    private Vec3D getParticleDest(RandomSource random) {
        if (ioMode.get().canExtract()) {
            return Vec3D.getCenter(worldPosition.relative(direction.get()));
        } else {
            double range = core.tier.get() / 2D;
            return new Vec3D(core.getBlockPos()).add(0.5, 0.5, 0.5).add((random.nextFloat() - 0.5F) * range, (random.nextFloat() - 0.5F) * range, (random.nextFloat() - 0.5F) * range);
        }
    }

    @Override
    public VoxelShape getShapeForPart(BlockPos pos, CollisionContext context) {
        return SPHERE_SHAPE;
    }

    @Override
    public boolean renderSelectionBox(ExtractBlockOutlineRenderStateEvent event) {
        return false;
    }

    @Nullable
    public static EnumColour getGlassColour(BlockState state) {
        if (state.is(Tags.Blocks.DYED_WHITE)) return EnumColour.WHITE;
        else if (state.is(Tags.Blocks.DYED_ORANGE)) return EnumColour.ORANGE;
        else if (state.is(Tags.Blocks.DYED_MAGENTA)) return EnumColour.MAGENTA;
        else if (state.is(Tags.Blocks.DYED_LIGHT_BLUE)) return EnumColour.LIGHT_BLUE;
        else if (state.is(Tags.Blocks.DYED_YELLOW)) return EnumColour.YELLOW;
        else if (state.is(Tags.Blocks.DYED_LIME)) return EnumColour.LIME;
        else if (state.is(Tags.Blocks.DYED_PINK)) return EnumColour.PINK;
        else if (state.is(Tags.Blocks.DYED_GRAY)) return EnumColour.GRAY;
        else if (state.is(Tags.Blocks.DYED_LIGHT_GRAY)) return EnumColour.LIGHT_GRAY;
        else if (state.is(Tags.Blocks.DYED_CYAN)) return EnumColour.CYAN;
        else if (state.is(Tags.Blocks.DYED_PURPLE)) return EnumColour.PURPLE;
        else if (state.is(Tags.Blocks.DYED_BLUE)) return EnumColour.BLUE;
        else if (state.is(Tags.Blocks.DYED_BROWN)) return EnumColour.BROWN;
        else if (state.is(Tags.Blocks.DYED_GREEN)) return EnumColour.GREEN;
        else if (state.is(Tags.Blocks.DYED_RED)) return EnumColour.RED;
        else if (state.is(Tags.Blocks.DYED_BLACK)) return EnumColour.BLACK;
        return null;
    }
}
