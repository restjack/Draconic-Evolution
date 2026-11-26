package com.brandon3055.draconicevolution.lib;

import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** eli.8: recipe/data condition {"type": "draconicevolution:config", "key": "<Extras switch>"}; true while that switch is on. */
public record ConfigCondition(String key) implements ICondition {
    public static final MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.mapCodec(b ->
            b.group(Codec.STRING.fieldOf("key").forGetter(ConfigCondition::key)).apply(b, ConfigCondition::new));
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, DraconicEvolution.MODID);

    static {
        CONDITIONS.register("config", () -> CODEC);
    }

    public static void init(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }

    @Override
    public boolean test(IContext context) {
        return DEConfig.extra(key);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
