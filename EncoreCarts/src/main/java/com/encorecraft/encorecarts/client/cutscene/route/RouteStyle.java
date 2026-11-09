package com.encorecraft.encorecarts.client.cutscene.route;

import com.encorecraft.encorecarts.travel.RouteKind;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/** Look and sound of one hidden route: sky, fog, weather particles, biome tint and sounds. */
public record RouteStyle(RouteKind kind, int skyTop, int skyHorizon, int fogColor, float fogStart, float fogEnd,
                         Particles particles, ResourceKey<Biome> tintBiome, SoundEvent hoofSound,
                         @Nullable SoundEvent ambientSound, float ambientChance) {
    public enum Particles { NONE, SNOW, ASH, POLLEN }

    public static RouteStyle of(RouteKind kind) {
        return switch (kind) {
            case MEADOW -> new RouteStyle(kind, 0xFF5E98E8, 0xFFC3DBF5, 0xFFC3DBF5, 34, 104, Particles.POLLEN,
                    Biomes.PLAINS, SoundEvents.HORSE_STEP, SoundEvents.PARROT_AMBIENT, 0.004F);
            case FOREST -> new RouteStyle(kind, 0xFF6C9FD8, 0xFFBFD6D0, 0xFFAFC6B4, 22, 88, Particles.POLLEN,
                    Biomes.FOREST, SoundEvents.HORSE_STEP, SoundEvents.PARROT_AMBIENT, 0.006F);
            case TAIGA -> new RouteStyle(kind, 0xFF8EA3BA, 0xFFDCE3EA, 0xFFD8DFE7, 14, 72, Particles.SNOW,
                    Biomes.SNOWY_TAIGA, SoundEvents.SNOW_STEP, SoundEvents.WOLF_HOWL, 0.0008F);
            case DESERT -> new RouteStyle(kind, 0xFF6FA9E6, 0xFFF0DEB2, 0xFFEAD7AA, 30, 118, Particles.NONE,
                    Biomes.DESERT, SoundEvents.SAND_STEP, null, 0.0F);
            case SWAMP -> new RouteStyle(kind, 0xFF7E958F, 0xFFB4C0A4, 0xFF98A68A, 12, 62, Particles.NONE,
                    Biomes.SWAMP, SoundEvents.HORSE_STEP, SoundEvents.FROG_AMBIENT, 0.01F);
            case NETHER -> new RouteStyle(kind, 0xFF2A0707, 0xFF6A1C10, 0xFF5A180E, 10, 58, Particles.ASH,
                    Biomes.NETHER_WASTES, SoundEvents.NETHERRACK_STEP, SoundEvents.GHAST_AMBIENT, 0.0015F);
        };
    }
}
