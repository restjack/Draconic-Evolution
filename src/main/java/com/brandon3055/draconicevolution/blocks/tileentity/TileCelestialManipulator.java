package com.brandon3055.draconicevolution.blocks.tileentity;

import codechicken.lib.data.MCDataInput;

import codechicken.lib.vec.Vector3;

import com.brandon3055.brandonscore.api.power.OPStorage;

import com.brandon3055.brandonscore.blocks.TileBCore;

import com.brandon3055.brandonscore.capability.CapabilityOP;

import com.brandon3055.brandonscore.client.particle.IntParticleType.IntParticleData;

import com.brandon3055.brandonscore.lib.IChangeListener;

import com.brandon3055.brandonscore.lib.IInteractTile;

import com.brandon3055.brandonscore.lib.Vec3D;

import com.brandon3055.brandonscore.lib.datamanager.DataFlags;

import com.brandon3055.brandonscore.lib.datamanager.ManagedBool;

import com.brandon3055.brandonscore.lib.datamanager.ManagedByte;

import com.brandon3055.brandonscore.lib.datamanager.ManagedInt;

import com.brandon3055.brandonscore.lib.datamanager.ManagedLong;

import com.brandon3055.brandonscore.lib.datamanager.ManagedString;

import com.brandon3055.brandonscore.lib.datamanager.ManagedTextComponent;

import com.brandon3055.brandonscore.lib.datamanager.ManagedUUID;

import com.brandon3055.draconicevolution.DEConfig;

import com.brandon3055.draconicevolution.api.modules.lib.ModularOPStorage;

import com.brandon3055.draconicevolution.client.DEParticles;

import com.brandon3055.draconicevolution.client.render.effect.EffectTrackerCelestialManipulator;

import com.brandon3055.draconicevolution.client.sound.CelestialModifierSound;

import com.brandon3055.draconicevolution.handlers.DESounds;

import com.brandon3055.draconicevolution.init.DEContent;

import com.brandon3055.draconicevolution.inventory.CelestialManipulatorMenu;

import com.brandon3055.draconicevolution.utils.LogHelper;

import java.util.HashMap;

import java.util.LinkedList;

import java.util.List;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;

import net.minecraft.client.Minecraft;

import net.minecraft.core.BlockPos;

import net.minecraft.core.Direction;

import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceKey;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundSource;

import net.minecraft.world.InteractionResult;

import net.minecraft.world.MenuProvider;

import net.minecraft.world.entity.player.Inventory;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;




/**
 * Created by brandon3055 on 28/09/2016.
 * <p>
 * restjack fork (unofficial-1.29): power is charged when the weather actually changes; runs are saved; time skips check
 * power first and ease in/out; redstone defaults to Off and can pick every action; replies go to the screen's status
 * box. Server rules (Extras > Server > celestialManipulator): owner lock, per-dimension cooldown, chat notice to the
 * dimension, keep-skies-clear mode. Comparator: 15 while running, else charge 0-14.
 */
public class TileCelestialManipulator extends TileBCore implements IChangeListener, MenuProvider, IInteractTile {

    public static final String[] ACTIONS = {"STOP_RAIN", "START_RAIN", "START_STORM", "SUN_RISE", "MID_DAY", "SUN_SET", "MOON_RISE", "MIDNIGHT", "MOON_SET", "SKIP_24"};
    /** Lang suffix (gui.draconicevolution.celestial_manipulator.X) and icon name for each action. */
    public static final String[] KEYS = {"clear", "rain", "storm", "sunrise", "noon", "sunset", "moonrise", "midnight", "moonset", "skip24"};
    public static final int[] TARGETS = {-1, -1, -1, 0, 5900, 12000, 13000, 17900, 22500, -1};
    public static final long[] WEATHER_COST = {256000, 256000, 384000};
    public static final int OP_PER_TICK = 320;
    /** Game ticks skipped per real tick at full speed (16k OP/t). */
    public static final int WARP_RATE = 50;
    public static final int SET_KEEP_CLEAR = 0, SET_LOCK = 1, SET_NOTICE = 2, SET_QUIET = 3;

    /** Last finished run per dimension (game time). Server only; a restart clears it. */
    private static final Map<ResourceKey<Level>, Long> LAST_USE = new HashMap<>();

    public final ManagedBool active = register(new ManagedBool("active", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool weatherToggleRunning = register(new ManagedBool("weather_toggle_running", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool timeWarpRunning = register(new ManagedBool("time_warp_running", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool timeWarpStopping = register(new ManagedBool("time_warp_stopping", DataFlags.SYNC_TILE));
    public final ManagedBool redstoneSignal = register(new ManagedBool("redstone_signal", DataFlags.SAVE_NBT_SYNC_TILE));
    /** Redstone action: -1 = off, else an index into {@link #ACTIONS}. New name so old saves (where 0 = clear) start Off. */
    public final ManagedByte rsAction = register(new ManagedByte("rs_action", -1, DataFlags.SAVE_NBT_SYNC_TILE));
    /** Current or last run: action index, who ("p:Name", "rs", "auto"), cost, skip target and length. */
    public final ManagedByte runAction = register(new ManagedByte("run_action", -1, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedString runBy = register(new ManagedString("run_by", "", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedLong runCost = register(new ManagedLong("run_cost", DataFlags.SAVE_NBT));
    public final ManagedLong targetTime = register(new ManagedLong("target_time", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedLong runLength = register(new ManagedLong("run_length", DataFlags.SAVE_NBT_SYNC_TILE));

    public final ManagedUUID owner = register(new ManagedUUID("owner", (java.util.UUID) null, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedString ownerName = register(new ManagedString("owner_name", "", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool keepClear = register(new ManagedBool("keep_clear", false, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool ownerOnly = register(new ManagedBool("owner_only", false, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool notice = register(new ManagedBool("notice", true, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool quiet = register(new ManagedBool("quiet", true, DataFlags.SAVE_NBT_SYNC_TILE));

    // Screen-only data
    public final ManagedTextComponent status = register(new ManagedTextComponent("status", DataFlags.SYNC_CONTAINER));
    public final ManagedBool raining = register(new ManagedBool("raining", DataFlags.SYNC_CONTAINER));
    public final ManagedBool thundering = register(new ManagedBool("thundering", DataFlags.SYNC_CONTAINER));
    public final ManagedInt feed = register(new ManagedInt("feed", DataFlags.SYNC_CONTAINER));
    public final ManagedInt cooldown = register(new ManagedInt("cooldown", DataFlags.SYNC_CONTAINER));

    public int timer = 0;
    private int statusTicks = 0;
    private long lastStored = -1, spent = 0;
    private int lastComparator = -1;

    public OPStorage opStorage = new ModularOPStorage(this, 8000000, 4000000, 4000000);

    public TileCelestialManipulator(BlockPos pos, BlockState state) {
        super(DEContent.TILE_CELESTIAL_MANIPULATOR.get(), pos, state);
        capManager.setManaged("energy", CapabilityOP.OP, opStorage).saveBoth().syncContainer();
    }

    public static void register(RegisterCapabilitiesEvent event) {
        com.brandon3055.brandonscore.capability.CapabilityOP.registerBlockEntity(event, DEContent.TILE_CELESTIAL_MANIPULATOR.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide()) serverUpkeep();

        if (weatherToggleRunning.get()) {
            timer++;
            if (level.isClientSide()) {
                clientEffects().updateWeatherEffects();
            } else if (timer >= 230) {
                finishWeather();
            }
        } else if (timeWarpRunning.get()) {
            timer++;
            if (level.isClientSide()) {
                clientEffects().updateSunEffect();
            } else if (timer > 100) {
                warpStep();
            }
        } else if (timeWarpStopping.get()) {
            if (timer > 100) {
                timer = 100;
            }

            timer--;
            if (timer <= 0 && !level.isClientSide()) {
                timeWarpStopping.set(false);
            }

            if (level.isClientSide() && timer >= 0) {
                clientEffects().updateSunEffect();
            } else if (level.isClientSide()) {
                if (clientEffects != null) clientEffects.stop();
            }
        } else if (active.get()) {
            if (!level.isClientSide()) active.set(false);
        } else if (level.isClientSide()) {
            clientEffects().standbyParticleEffect();
        }
    }

    private void serverUpkeep() {
        if (statusTicks > 0 && --statusTicks == 0) status.set(null);
        if (onInterval(20)) {
            long stored = opStorage.getOPStored();
            if (lastStored >= 0) feed.set((int) Math.max(0, Math.min(Integer.MAX_VALUE, (stored - lastStored + spent) / 20)));
            lastStored = stored;
            spent = 0;
            raining.set(level.isRaining());
            thundering.set(level.isThundering());
            cooldown.set(cooldownLeft());
            int c = getComparatorOutput();
            if (c != lastComparator) {
                lastComparator = c;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
        if (onInterval(100) && keepClear.get() && rules() && level.isRaining() && !active.get() && cooldownLeft() == 0
                && opStorage.getOPStored() >= WEATHER_COST[0]) {
            handleInteract("STOP_RAIN", null, "auto");
        }
    }

    private static boolean rules() {
        return DEConfig.extra("celestialManipulator");
    }

    //region Runs

    /** Seconds until this dimension's cooldown ends (0 = ready). */
    public int cooldownLeft() {
        if (!rules() || DEConfig.celestialCooldown <= 0 || level == null) return 0;
        Long last = LAST_USE.get(level.dimension());
        if (last == null) return 0;
        long left = DEConfig.celestialCooldown * 20L - (level.getGameTime() - last);
        return left <= 0 ? 0 : (int) ((left + 19) / 20);
    }

    /** Game ticks from now to the given time of day (0-23999). */
    public long ticksTill(int time) {
        int currentTime = (int) Math.floorMod(level.getOverworldClockTime(), 24000L);
        return currentTime > time ? (24000 - (currentTime - time)) : time - currentTime;
    }

    /** Ticks a time action would skip (0 for weather actions). */
    public long skipLength(int action) {
        if (action == 9) return 24000;
        return TARGETS[action] < 0 ? 0 : ticksTill(TARGETS[action]);
    }

    /** OP an action costs right now. */
    public long costOf(int action) {
        return action < 3 ? WEATHER_COST[action] : skipLength(action) * OP_PER_TICK;
    }

    private void startWeather(int action, String by) {
        runAction.set((byte) action);
        runBy.set(by);
        runCost.set(WEATHER_COST[action]);
        timer = 0;
        weatherToggleRunning.set(true);
        active.set(true);
    }

    private void finishWeather() {
        timer = 0;
        weatherToggleRunning.set(false);
        int action = runAction.get();
        long cost = runCost.get();
        if (action < 0 || action > 2) return;
        if (opStorage.getOPStored() < cost) {
            setStatus(Component.translatable("celestial_manipulator.draconicevolution.power", fmt(cost), fmt(opStorage.getOPStored())).withStyle(ChatFormatting.RED));
            return;
        }
        opStorage.modifyEnergyStored(-cost);
        spent += cost;
        boolean rain = action != 0, storm = action == 2;
        int time = (10 * 60 * 20) + level.getRandom().nextInt(20 * 60 * 20);
        var weather = ((ServerLevel) level).getWeatherData();
        weather.setRaining(rain);
        weather.setThundering(storm);
        weather.setRainTime(rain ? time : 0);
        weather.setClearWeatherTime(rain ? 0 : time);
        raining.set(rain);
        thundering.set(storm);
        finished(label(action));
    }

    private void startTimeWarp(int action, long length, String by) {
        runAction.set((byte) action);
        runBy.set(by);
        runLength.set(length);
        targetTime.set(level.getOverworldClockTime() + length);
        timer = 0;
        timeWarpRunning.set(true);
        active.set(true);
    }

    /** One tick of the skip: ramps up over 2 s, slows over the last 1000 ticks of game time, 320 OP per skipped tick. */
    private void warpStep() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        long left = targetTime.get() - level.getOverworldClockTime();
        if (left <= 0) {
            stopTimeWarp(label(runAction.get()));
            return;
        }
        double ramp = Math.min(1D, (timer - 100) / 40D);
        double tail = Math.min(1D, left / 1000D);
        long ticks = Math.min(left, Math.max(2, Math.round(WARP_RATE * Math.min(ramp, tail))));
        ticks = Math.min(ticks, opStorage.getOPStored() / OP_PER_TICK);
        if (ticks <= 0) {
            stopTimeWarp(Component.translatable("celestial_manipulator.draconicevolution.notice.out_of_power", clock()));
            return;
        }
        // MC26 clock changes sync authoritatively; preserve clock rate and pause state.
        var clock = serverLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
        serverLevel.clockManager().addTicks(clock, (int) ticks);
        opStorage.modifyEnergyStored(-ticks * OP_PER_TICK);
        spent += ticks * OP_PER_TICK;
        if (left - ticks <= 0) stopTimeWarp(label(runAction.get()));
    }

    /** Ends a running skip and reports what happened (done, stopped at, out of power). */
    private void stopTimeWarp(Component what) {
        timeWarpRunning.set(false);
        timeWarpStopping.set(true);
        finished(what);
    }

    /** Status box, server log, chat notice to the dimension, cooldown. */
    private void finished(Component what) {
        setStatus(Component.translatable("celestial_manipulator.draconicevolution.done", what).withStyle(ChatFormatting.GREEN));
        Component who = whoName();
        LogHelper.info("Celestial Manipulator at " + worldPosition.toShortString() + ": " + who.getString() + " -> " + what.getString());
        if (!(level instanceof ServerLevel sl)) return;
        LAST_USE.put(level.dimension(), level.getGameTime());
        cooldown.set(cooldownLeft());
        if (rules() && notice.get()) {
            Component msg = Component.literal("[Celestial] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.translatable("celestial_manipulator.draconicevolution.notice", who, what).withStyle(ChatFormatting.YELLOW));
            for (ServerPlayer p : sl.players()) p.sendSystemMessage(msg);
        }
    }

    private Component whoName() {
        String by = runBy.get();
        if (by.startsWith("p:")) return Component.literal(by.substring(2));
        if (by.equals("auto")) return Component.translatable("celestial_manipulator.draconicevolution.by_auto");
        return Component.translatable("celestial_manipulator.draconicevolution.by_redstone", worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    public static Component label(int action) {
        return action < 0 || action >= KEYS.length ? Component.empty() : Component.translatable("gui.draconicevolution.celestial_manipulator." + KEYS[action]);
    }

    /** "06:30" from the Overworld clock (tick 0 = 06:00). */
    public String clock() {
        return clock(level.getOverworldClockTime());
    }

    public static String clock(long time) {
        long t = Math.floorMod(time, 24000L);
        long h = (t / 1000 + 6) % 24, m = (t % 1000) * 60 / 1000;
        return String.format("%02d:%02d", h, m);
    }

    public static String fmt(long op) {
        if (op >= 1_000_000) return String.format("%.2fM", op / 1_000_000D).replace(".00M", "M");
        if (op >= 1_000) return (op / 1000) + "k";
        return String.valueOf(op);
    }

    private void setStatus(Component c) {
        status.set(c);
        statusTicks = 100;
    }

    //endregion

    //region Owner

    /** Owner, the owner's FTB Teams party (or scoreboard team) or an operator. Unowned = anyone. */
    public boolean isOwnerOrTeam(ServerPlayer player) {
        if (owner.isNull() || player.getUUID().equals(owner.get()) || isOp(player)) return true;
        if (level instanceof ServerLevel sl) {
            ServerPlayer o = sl.getServer().getPlayerList().getPlayer(owner.get());
            if (TileCoreMonitor.teamMembers(sl, owner.get(), o).contains(player)) return true;
            var team = sl.getServer().getScoreboard().getPlayersTeam(player.getScoreboardName());
            return o == null && team != null && !ownerName.get().isEmpty() && team.getPlayers().contains(ownerName.get());
        }
        return false;
    }

    private static boolean isOp(ServerPlayer player) {
        return net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }

    /** May this player use and change it? Locked = owner, team and operators only. */
    public boolean canControl(ServerPlayer player) {
        return !rules() || !ownerOnly.get() || isOwnerOrTeam(player);
    }

    public void setOwner(@Nullable Player player) {
        owner.set(player == null ? null : player.getUUID());
        ownerName.set(player == null ? "" : player.getName().getString());
    }

    //endregion

    //region Interact

    @Override
    public void receivePacketFromClient(MCDataInput data, ServerPlayer client, int id) {
        if (id == 0) {
            handleInteract(data.readString(), client, "p:" + client.getName().getString());
        } else if (id == 1) {
            int v = data.readInt();
            if (!canControl(client)) {
                lockedStatus();
            } else if (v >= -1 && v < ACTIONS.length) {
                rsAction.set((byte) v);
            }
        } else if (id == 2) {
            int which = data.readByte();
            boolean v = data.readBoolean();
            if (which == SET_LOCK) {
                if (!isOwnerOrTeam(client)) {
                    lockedStatus();
                    return;
                }
                if (owner.isNull()) setOwner(client); // locking an unowned block claims it
                ownerOnly.set(v);
            } else if (!canControl(client)) {
                lockedStatus();
            } else if (which == SET_KEEP_CLEAR) {
                keepClear.set(v);
            } else if (which == SET_NOTICE) {
                notice.set(v);
            } else if (which == SET_QUIET) {
                quiet.set(v);
            }
        }
    }

    private void lockedStatus() {
        setStatus(Component.translatable("celestial_manipulator.draconicevolution.locked", ownerName.get().isEmpty() ? "?" : ownerName.get()).withStyle(ChatFormatting.RED));
    }

    /**
     * Starts or stops a run. player = null for redstone / keep-clear (no permission check; still waits for the cooldown).
     * by = "p:Name", "rs" or "auto". Every outcome lands in the status box.
     */
    public void handleInteract(String actionName, @Nullable ServerPlayer player, String by) {
        if (actionName.endsWith("STOP")) {
            if (player != null && !canControl(player)) {
                lockedStatus();
            } else if (timeWarpRunning.get()) {
                runBy.set(by);
                stopTimeWarp(Component.translatable("celestial_manipulator.draconicevolution.notice.stopped", clock()));
            } else {
                setStatus(Component.translatable("celestial_manipulator.draconicevolution.nothing_to_stop"));
            }
            return;
        }
        int action = java.util.Arrays.asList(ACTIONS).indexOf(actionName);
        if (action < 0) return;

        if (active.get()) {
            setStatus(Component.translatable("celestial_manipulator.draconicevolution.busy", label(runAction.get())).withStyle(ChatFormatting.GOLD));
            return;
        }
        if (player != null && !canControl(player)) {
            lockedStatus();
            return;
        }
        int cd = cooldownLeft();
        if (cd > 0 && !(player != null && isOp(player))) {
            setStatus(Component.translatable("celestial_manipulator.draconicevolution.cooldown", cd).withStyle(ChatFormatting.GOLD));
            return;
        }
        String already = switch (action) {
            case 0 -> level.isRaining() ? null : "already_clear";
            case 1 -> level.isRaining() ? "already_raining" : null;
            case 2 -> level.isRaining() && level.isThundering() ? "already_storming" : null;
            default -> null;
        };
        if (already != null) {
            setStatus(Component.translatable("celestial_manipulator.draconicevolution." + already).withStyle(ChatFormatting.GOLD));
            return;
        }
        long cost = costOf(action);
        if (opStorage.getOPStored() < cost) {
            setStatus(Component.translatable("celestial_manipulator.draconicevolution.power", fmt(cost), fmt(opStorage.getOPStored())).withStyle(ChatFormatting.RED));
            return;
        }
        if (action < 3) {
            startWeather(action, by);
        } else {
            startTimeWarp(action, skipLength(action), by);
        }
        setStatus(Component.translatable("celestial_manipulator.draconicevolution.running", label(action), fmt(cost)).withStyle(ChatFormatting.AQUA));
    }

    /** Comparator: 15 while running, else stored charge 0-14. */
    public int getComparatorOutput() {
        if (active.get()) return 15;
        return (int) (opStorage.getOPStored() * 14 / Math.max(1, opStorage.getMaxOPStored()));
    }

    //endregion

    private ClientEffects clientEffects;

    public ClientEffects clientEffects() {
        if (clientEffects == null) clientEffects = new ClientEffects();
        return clientEffects;
    }

    public final class ClientEffects {
        private void stop() {
            if (effects != null) effects.clear();
            if (sound != null) {
                sound.kill();
                sound = null;
            }
        }

        private CelestialModifierSound sound;

        //region Weather Effect

        private List<EffectTrackerCelestialManipulator> effects;

        public void startWeatherEffect() {
            timer = 0;

            effects = new LinkedList<>();
            for (int i = 0; i < 8; i++) {
                effects.add(new EffectTrackerCelestialManipulator(level, Vec3D.getCenter(worldPosition).add(0, 1.5, 0), Vec3D.getCenter(worldPosition).add(0, 1.5, 0)));
            }

            Vec3D vec = Vec3D.getCenter(worldPosition.offset(0, 1, 0));
            sound = new CelestialModifierSound(DESounds.ELECTRIC_BUZZ.get(), worldPosition, level.getRandom());
            sound.updateSound(vec, 0.01F, 0.5F);
            Minecraft.getInstance().getSoundManager().play(sound);
            level.playLocalSound(vec.x, vec.y, vec.z, DESounds.FUSION_COMPLETE.get(), SoundSource.BLOCKS, getSoundVolume(), 0.5F, false);
        }

        public void updateWeatherEffects() {
            if (effects == null || effects.size() < 8 || sound == null) {
                startWeatherEffect();
                return;
            }

            double riseEnd = 20;
            double expandStart = 60;
            double expandEnd = 100;
            double ascendStart = 140;
            double ascendStop = 200;
            double ascendHeight = 500;

            double height = (Math.min(riseEnd, timer) / riseEnd) * 3D;
            double expansion = timer > expandStart ? ((Math.min(expandEnd, timer) - expandStart) / (expandEnd - expandStart)) : 0D;
            double secondaryExpand = 0;

            double ascPos = 0;

            if (timer > ascendStart) {
                ascPos = (timer - ascendStart) / (ascendStop - ascendStart);
                height += ascPos * (ascendHeight * ascPos);
                secondaryExpand = ascPos * ascPos * 100D;
            }

            Vec3D effectFocus = Vec3D.getCenter(worldPosition).add(0, height, 0);


    //        if (timer < )


            double scaleTime = (timer - expandStart) / (ascendStart - expandStart);
            if (timer > expandStart && timer < ascendStart) {
                for (int i = 0; i < 10; i++) {
                    Direction dir = Direction.values()[2 + level.getRandom().nextInt(4)];
                    IntParticleData data = new IntParticleData(DEParticles.SPARK.get(),
                            0, //R
                            127,  //G
                            255,  //B
                            (int) (1F * (level.getRandom().nextFloat() + 0.1) * 100), //Scale
                            (int) (5D * scaleTime * 100), //Spark scale
                            10, 0, //Max Age, Additional random age
                            (int) (0), //Gravity
                            (int) (1F * 1000), //Friction
                            1 //Set velocity direct
                    );
                    double speed = 0.3 / 10;
                    Vector3 pos = Vector3.fromTileCenter(TileCelestialManipulator.this).add(dir.getStepX() * -0.3, 0, dir.getStepZ() * -0.3);
                    level.addParticle(data, pos.x, pos.y, pos.z, speed * dir.getStepX(), 0, speed * dir.getStepZ());
                }
            }

            if (timer == ascendStart) {
                for (int i = 0; i < 100; i++) {
                    IntParticleData data = new IntParticleData(DEParticles.SPARK.get(),
                            0, //R
                            127,  //G
                            255,  //B
                            (int) (5F * (level.getRandom().nextFloat() + 0.1) * 100), //Scale
                            (int) (100 * 100), //Spark scale
                            80, 0, //Max Age, Additional random age
                            (int) (-0.5 * 1000), //Gravity
                            (int) (1F * 1000) //Friction
                            //Set velocity direct
                    );
                    Vector3 pos = Vector3.fromTileCenter(TileCelestialManipulator.this);
                    level.addParticle(data, pos.x, pos.y, pos.z, 0.5 * level.getRandom().nextGaussian(), 0, 0.5 * level.getRandom().nextGaussian());
                }
            }


            if (timer == riseEnd) {
    //            level.playLocalSound(effectFocus.x, effectFocus.y, effectFocus.z, DESounds.fusionComplete, SoundSource.BLOCKS, 1F, 1F, false);
            } else if (timer == ascendStart) {
                level.playLocalSound(effectFocus.x, effectFocus.y, effectFocus.z, DESounds.FUSION_COMPLETE.get(), SoundSource.BLOCKS, 1F, 2F, false);
                for (int i = 0; i < 100; i++) {
                    try {
                        //                    SubParticle particle = new SubParticle(world, effects.get(world.rand.nextInt(effects.size())).pos);
                        //                    particle.setScale(2);
                        //TODO particles
                        //                    BCEffectHandler.spawnFXDirect(DEParticles.DE_SHEET, particle, 128, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }

            sound.updateSound(effectFocus.copy().add(0, -(height * 0.8), 0), (float) expansion, (float) expansion + 0.5F);

            for (EffectTrackerCelestialManipulator effect : effects) {
                effect.onUpdate();
            }

            double rotation = (double) timer * (expansion * 0.05D);
            for (EffectTrackerCelestialManipulator effect : effects) {
                effect.effectFocus = effectFocus;
                effect.scale = (float) expansion * (1F + (float) (ascPos * 5));

                double indexPos = (double) effects.indexOf(effect) / (double) effects.size();
                double offset = indexPos * (Math.PI * 2);
                double offsetX = Math.sin(rotation + offset) * expansion * (3 + secondaryExpand);
                double offsetZ = Math.cos(rotation + offset) * expansion * (3 + secondaryExpand);

                effect.pos = effectFocus.copy().add(offsetX, 0, offsetZ);

                offset = (indexPos + (1D / (double) effects.size())) * (Math.PI * 2);
                offsetX = Math.sin(rotation + offset) * expansion * (3 + secondaryExpand);
                offsetZ = Math.cos(rotation + offset) * expansion * (3 + secondaryExpand);
                effect.linkPos = effectFocus.copy().add(offsetX, 0, offsetZ);
            }

            if (timer >= 220) {
                level.playLocalSound(effectFocus.x, effectFocus.y, effectFocus.z, DESounds.BOOM.get(), SoundSource.BLOCKS, quiet.get() ? 1 : 100, 1F, false);
                timer = 0;
                weatherToggleRunning.set(false);
                effects.clear();
                sound.kill();
                sound = null;
            }
        }

        //endregion

        //region Sun Effect

        public void startSunEffect() {
            timeWarpRunning.set(true);
            timeWarpStopping.set(false);
            timer = 0;

            effects = new LinkedList<>();
            for (int i = 0; i < 12; i++) {
                effects.add(new EffectTrackerCelestialManipulator(level, Vec3D.getCenter(worldPosition).add(0, 1.5, 0), Vec3D.getCenter(worldPosition).add(0, 1.5, 0)));
            }

            effects.get(0).red = 1F;
            effects.get(0).green = 0.821F;
            effects.get(0).blue = 0.174F;
            effects.get(0).renderBolts = false;

            effects.get(1).red = 1F;
            effects.get(1).green = 1F;
            effects.get(1).blue = 1F;
            effects.get(1).renderBolts = false;

            sound = new CelestialModifierSound(DESounds.SUN_DIAL_EFFECT.get(), worldPosition, level.getRandom());
            sound.updateSound(Vec3D.getCenter(worldPosition), getSoundVolume(), 0.5F);
            Minecraft.getInstance().getSoundManager().play(sound);
        }

        public void stopSunEffect() {
            timeWarpRunning.set(false);
            timeWarpStopping.set(true);
            if (timer > 100) {
                timer = 100;
            }
        }

        public void updateSunEffect() {
            if (effects == null || effects.size() < 2 || sound == null) {
                startSunEffect();
            }

            double depProg = Math.min(timer / 100F, 1D);

            EffectTrackerCelestialManipulator effect;
            Vec3D focus = Vec3D.getCenter(worldPosition).add(0, 5.5 * depProg, 0);
            sound.updateSound(focus, (float) depProg, 0.5F + (float) depProg);

    //		if (timer % 4 == 0) {
    //			for (int i = 0; i < (timer % 20 <= 10 ? 10 : 2); i++) {
    //				//TODO particles
    //				//                BCEffectHandler.spawnFXDirect(DEParticles.DE_SHEET, new EffectTrackerCelestialManipulator.SubParticle2(world, focus, effects.get(i % 2)), 128, true);
    //			}
    //		}

            IntParticleData data = new IntParticleData(DEParticles.SPARK.get(),
                    255, //R
                    127,  //G
                    0,  //B
                    (int) (1.2F * (level.getRandom().nextFloat() + 0.1) * 100), //Scale
                    (int) (0.5F * 100), //Spark scale
                    30, 10, //Max Age, Additional random age
                    (int) (0) //Gravity
            );
            Vector3 pos = Vector3.fromTileCenter(TileCelestialManipulator.this);
            level.addParticle(data, pos.x, pos.y, pos.z, 0.002D, 0.04, 0.002D);

            Direction dir = Direction.values()[2 + level.getRandom().nextInt(4)];
            data = new IntParticleData(DEParticles.SPARK.get(),
                    255, //R
                    127,  //G
                    255,  //B
                    (int) (1F * (level.getRandom().nextFloat() + 0.1) * 100), //Scale
                    (int) (0.5F * 100), //Spark scale
                    10, 0, //Max Age, Additional random age
                    (int) (0), //Gravity
                    (int) (1F * 1000), //Friction
                    1 //Set velocity direct
            );
            double speed = 0.3 / 10;
            pos = Vector3.fromTileCenter(TileCelestialManipulator.this).add(dir.getStepX() * -0.3, 0, dir.getStepZ() * -0.3);
            level.addParticle(data, pos.x, pos.y, pos.z, speed * dir.getStepX(), 0, speed * dir.getStepZ());


            double rotation = (level.getOverworldClockTime() % 24000) / 24000D;
            double offset;
            double offsetX;
            double offsetY;

            for (int i = 0; i < 2; i++) {
                effect = effects.get(i);
                effect.scale = (float) depProg;
                effect.onUpdate();
                offset = rotation * (Math.PI * 2);
                offsetX = Math.cos(offset) * 2.5 * depProg;
                offsetY = Math.sin(offset) * 2.5 * depProg;
                effect.pos = focus.copy().add(offsetX, offsetY, 0);
                rotation += 0.5F;
                effect.effectFocus = focus;
            }

            rotation = timer / 50D;

            double inc = 1D / (effects.size() - 2);
            for (int i = 2; i < effects.size(); i++) {
                effect = effects.get(i);
                offset = rotation * (Math.PI * 2);
                double x = Math.cos(offset) * 1 * depProg;
                double z = Math.sin(offset) * 1 * depProg;
                effect.scale = (float) depProg;
                effect.onUpdate();

                effect.pos = focus.copy().add(x, (i % 2 == 0 ? x : -x) * 0.8, z);
                if (i % 2 == 0) {
                    effect.red = 0.9F;
                    effect.green = 0.5F;
                    effect.blue = 0.1F;
                } else {
                    effect.red = 0.3F;
                    effect.green = 0.2F;
                    effect.blue = 1;
                }

                effect.effectFocus = focus;
                effect.renderBolts = false;
                rotation += inc;
            }
        }

        //endregion

        //region Rendering

        private void standbyParticleEffect() {
            IntParticleData data = new IntParticleData(DEParticles.SPARK.get(),
                    76, //R
                    0,  //G
                    255,  //B
                    (int) (0.4F * (level.getRandom().nextFloat() + 0.1) * 100), //Scale
                    (int) (0.15F * 100), //Spark scale
                    30, 10, //Max Age, Additional random age
                    (int) (0) //Gravity
            );
            Vector3 pos = Vector3.fromTileCenter(TileCelestialManipulator.this);
            level.addParticle(data, pos.x, pos.y, pos.z, 0.002D, 0.01, 0.002D);


            //        SubParticle particle = new SubParticle(world, Vec3D.getCenter(pos));
            //        particle.setScale(0.2F);
            //TODO particles
            //        BCEffectHandler.spawnFXDirect(DEParticles.DE_SHEET, particle);
            if (effects != null) {
                effects = null;
            }
        }

        public void extractEffects(float partialTicks, com.mojang.blaze3d.vertex.PoseStack pose,
                                   net.minecraft.client.renderer.MultiBufferSource buffers) {
            if (effects == null) return;
            for (EffectTrackerCelestialManipulator effect : effects) {
                effect.extractEffect(pose, buffers, partialTicks, worldPosition, weatherToggleRunning.get());
            }
        }

        //endregion
    }

    @Override
    public void onNeighborChange(BlockPos blockChanged) {
        if (level.isClientSide()) return;
        if (level.hasNeighborSignal(worldPosition)) {
            if (!redstoneSignal.get()) {
                redstoneSignal.set(true);
                int a = rsAction.get();
                if (a >= 0 && a < ACTIONS.length) handleInteract(ACTIONS[a], null, "rs");
            }
        } else {
            if (redstoneSignal.get()) {
                redstoneSignal.set(false);
                // Skip 24 on redstone: the signal going off stops the skip early.
                if (timeWarpRunning.get() && rsAction.get() == 9) handleInteract("STOP", null, "rs");
            }
        }
    }

    private float getSoundVolume() {
        return quiet.get() ? 1 : 10;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int currentWindowIndex, Inventory playerInventory, Player player) {
        return new CelestialManipulatorMenu(currentWindowIndex, player.getInventory(), this);
    }

    @Override
    public InteractionResult onBlockUse(BlockState state, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer) {
            player.openMenu(this, worldPosition);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}
