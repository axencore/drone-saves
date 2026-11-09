package com.encorecraft.encorecarts.station;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.entity.CartEntity;
import com.encorecraft.encorecarts.entity.CoachmanEntity;
import com.encorecraft.encorecarts.network.payload.SyncStationsPayload;
import com.encorecraft.encorecarts.registry.ModEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side station operations. Stations are only ever removed by an explicit admin action
 * (sneak + hit in creative, or {@code /carts remove}); missing carts/coachmen are respawned automatically,
 * so {@code /kill} or entity cleaners cannot silently destroy a station.
 */
public final class StationManager {
    private static final int HEAL_INTERVAL_TICKS = 100;

    private StationManager() {
    }

    public static boolean isAdmin(ServerPlayer player) {
        return player.hasPermissions(CartsConfig.adminPermissionLevel());
    }

    public static boolean canDismantle(ServerPlayer player) {
        return player.isCreative() && player.isShiftKeyDown() && isAdmin(player);
    }

    public static Station create(ServerLevel level, StationLayout layout, String name, boolean global) {
        MinecraftServer server = level.getServer();
        StationData data = StationData.get(server);
        Station station = new Station(UUID.randomUUID(), name, level.dimension(), layout.anchor(), layout.yaw(), global);
        data.add(station);
        spawnCart(level, station);
        spawnCoachman(level, station, level.getRandom().nextBoolean());
        data.saveNow(server);
        syncAll(server);
        EncoreCarts.LOGGER.info("Station '{}' created in {} at {} (global={})", name, level.dimension().location(), layout.anchor(), global);
        return station;
    }

    /** Admin hit a cart or coachman while sneaking in creative. */
    public static void dismantle(ServerPlayer actor, Optional<UUID> stationId, Entity hitEntity) {
        if (stationId.isPresent() && removeStation(actor.server, stationId.get())) {
            actor.sendSystemMessage(Component.translatable("message.encorecarts.station.removed", stationName(stationId.get(), hitEntity))
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        hitEntity.discard();
    }

    private static String stationName(UUID id, Entity fallback) {
        return lastRemovedName != null ? lastRemovedName : fallback.getName().getString();
    }

    @Nullable
    private static String lastRemovedName;

    public static boolean removeStation(MinecraftServer server, UUID id) {
        StationData data = StationData.get(server);
        Station station = data.remove(id);
        if (station == null) {
            return false;
        }
        lastRemovedName = station.name();
        ServerLevel level = server.getLevel(station.dimension());
        if (level != null) {
            discardEntity(level, station.cartId());
            discardEntity(level, station.coachmanId());
        }
        data.saveNow(server);
        syncAll(server);
        EncoreCarts.LOGGER.info("Station '{}' removed", station.name());
        return true;
    }

    private static void discardEntity(ServerLevel level, @Nullable UUID entityId) {
        if (entityId != null) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    /** Removes cart/coachman entities whose station no longer exists or that were replaced by a respawned copy. */
    public static void discardIfOrphaned(Entity entity, Optional<UUID> stationId) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Station station = stationId.map(id -> StationData.get(level.getServer()).get(id)).orElse(null);
        if (station == null) {
            entity.discard();
            return;
        }
        UUID expected = entity instanceof CartEntity ? station.cartId() : station.coachmanId();
        if (expected != null && !expected.equals(entity.getUUID())) {
            entity.discard();
        }
    }

    /** Periodically respawns missing station entities in loaded chunks. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % HEAL_INTERVAL_TICKS != 0) {
            return;
        }
        StationData data = StationData.get(server);
        for (Station station : List.copyOf(data.stations())) {
            ServerLevel level = server.getLevel(station.dimension());
            if (level == null) {
                continue;
            }
            if (entitiesLoaded(level, station.anchor())
                    && (station.cartId() == null || !(level.getEntity(station.cartId()) instanceof CartEntity))) {
                spawnCart(level, station);
                data.setDirty();
            }
            Vec3 coachmanSpot = station.layout().coachmanSpot();
            if (entitiesLoaded(level, coachmanSpot)
                    && (station.coachmanId() == null || !(level.getEntity(station.coachmanId()) instanceof CoachmanEntity))) {
                spawnCoachman(level, station, level.getRandom().nextBoolean());
                data.setDirty();
            }
        }
    }

    private static boolean entitiesLoaded(ServerLevel level, Vec3 pos) {
        return level.areEntitiesLoaded(new ChunkPos(BlockPos.containing(pos)).toLong());
    }

    private static void spawnCart(ServerLevel level, Station station) {
        CartEntity cart = ModEntities.CART.get().create(level);
        if (cart == null) {
            return;
        }
        Vec3 anchor = station.anchor();
        cart.moveTo(anchor.x, anchor.y, anchor.z, station.yaw(), 0.0F);
        cart.setStationId(station.id());
        if (level.addFreshEntity(cart)) {
            station.setCartId(cart.getUUID());
        }
    }

    private static void spawnCoachman(ServerLevel level, Station station, boolean slim) {
        CoachmanEntity coachman = ModEntities.COACHMAN.get().create(level);
        if (coachman == null) {
            return;
        }
        StationLayout layout = station.layout();
        Vec3 spot = StationLayout.safeSpot(level, layout.coachmanSpot());
        float yaw = layout.coachmanYaw();
        coachman.moveTo(spot.x, spot.y, spot.z, yaw, 0.0F);
        coachman.setYHeadRot(yaw);
        coachman.setYBodyRot(yaw);
        coachman.setSlim(slim);
        coachman.setStationId(station.id());
        if (level.addFreshEntity(coachman)) {
            station.setCoachmanId(coachman.getUUID());
        }
    }

    /** Opens a hidden station for a player who talked to its coachman. */
    public static void discover(ServerPlayer player, Station station) {
        if (StationData.get(player.server).discover(player.getUUID(), station)) {
            player.sendSystemMessage(Component.translatable("message.encorecarts.station.discovered", station.name())
                    .withStyle(ChatFormatting.GOLD));
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.4F);
            syncStations(player);
        }
    }

    public static void setAccess(MinecraftServer server, UUID player, Station station, boolean open) {
        StationData data = StationData.get(server);
        data.setAccess(player, station, open);
        data.saveNow(server);
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            syncStations(online);
        }
    }

    /** Stations of the given player's current dimension that are open to them, nearest first from {@code origin}. */
    public static List<Station> openStations(ServerPlayer player) {
        StationData data = StationData.get(player.server);
        List<Station> list = new ArrayList<>();
        for (Station station : data.stations()) {
            if (data.isOpenFor(player.getUUID(), station)) {
                list.add(station);
            }
        }
        return list;
    }

    public static List<String> names(MinecraftServer server) {
        return StationData.get(server).stations().stream().map(Station::name)
                .sorted(Comparator.comparing(s -> s.toLowerCase(java.util.Locale.ROOT))).toList();
    }

    public static void syncStations(ServerPlayer player) {
        List<SyncStationsPayload.MapStation> list = new ArrayList<>();
        for (Station station : openStations(player)) {
            BlockPos pos = BlockPos.containing(station.anchor());
            list.add(new SyncStationsPayload.MapStation(station.id(), station.name(), station.dimension().location(),
                    pos.getX(), pos.getY(), pos.getZ()));
        }
        PacketDistributor.sendToPlayer(player, new SyncStationsPayload(list));
    }

    public static void syncAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(StationManager::syncStations);
    }
}
