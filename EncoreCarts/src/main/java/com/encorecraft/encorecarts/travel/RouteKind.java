package com.encorecraft.encorecarts.travel;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;

/**
 * The hidden routes of the travel cutscene. Each one is a client-side scene that never exists in any level;
 * the server picks it from the biome at the departure station.
 */
public enum RouteKind {
    MEADOW,
    FOREST,
    TAIGA,
    DESERT,
    SWAMP,
    NETHER;

    public static RouteKind byId(int id) {
        RouteKind[] values = values();
        return id >= 0 && id < values.length ? values[id] : MEADOW;
    }

    public static RouteKind forBiome(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_NETHER)) {
            return NETHER;
        }
        if (biome.is(Tags.Biomes.IS_SNOWY) || biome.is(BiomeTags.IS_TAIGA) || biome.is(Tags.Biomes.IS_ICY)
                || biome.is(Tags.Biomes.IS_COLD_OVERWORLD)) {
            return TAIGA;
        }
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.IS_SAVANNA)
                || biome.is(Tags.Biomes.IS_DRY_OVERWORLD)) {
            return DESERT;
        }
        if (biome.is(Tags.Biomes.IS_SWAMP) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH)
                || biome.is(Tags.Biomes.IS_MUSHROOM)) {
            return SWAMP;
        }
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(Tags.Biomes.IS_DENSE_VEGETATION_OVERWORLD)) {
            return FOREST;
        }
        return MEADOW;
    }
}
