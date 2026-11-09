package com.encorecraft.encorecarts.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
    /** Values are package-private {@code ChunkMap.TrackedEntity} instances, see {@link TrackedEntityAccessor}. */
    @Accessor("entityMap")
    Int2ObjectMap<?> encorecarts$getEntityMap();
}
