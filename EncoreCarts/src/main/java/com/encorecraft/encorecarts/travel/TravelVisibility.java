package com.encorecraft.encorecarts.travel;

import com.encorecraft.encorecarts.mixin.ChunkMapAccessor;
import com.encorecraft.encorecarts.mixin.TrackedEntityAccessor;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Hides travelling players from everybody else: {@code ServerPlayerMixin} makes the entity tracker treat them
 * as invisible, and the tracker is refreshed immediately so other clients drop (and later re-add) the entity.
 */
public final class TravelVisibility {
    private static final Set<UUID> HIDDEN = ConcurrentHashMap.newKeySet();

    private TravelVisibility() {
    }

    public static boolean isHidden(UUID player) {
        return HIDDEN.contains(player);
    }

    static void hide(ServerPlayer player) {
        if (HIDDEN.add(player.getUUID())) {
            refreshTracking(player);
        }
    }

    static void unhide(ServerPlayer player) {
        if (HIDDEN.remove(player.getUUID())) {
            refreshTracking(player);
        }
    }

    static void forget(UUID player) {
        HIDDEN.remove(player);
    }

    static void reset() {
        HIDDEN.clear();
    }

    private static void refreshTracking(ServerPlayer player) {
        ChunkMap chunkMap = player.serverLevel().getChunkSource().chunkMap;
        Object tracked = ((ChunkMapAccessor) chunkMap).encorecarts$getEntityMap().get(player.getId());
        if (tracked instanceof TrackedEntityAccessor accessor) {
            accessor.encorecarts$updatePlayers(player.serverLevel().players());
        }
    }
}
