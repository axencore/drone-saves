package com.encorecraft.encorecarts.travel;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.economy.Economy;
import com.encorecraft.encorecarts.network.payload.StartCutscenePayload;
import com.encorecraft.encorecarts.network.payload.TravelAbortedPayload;
import com.encorecraft.encorecarts.network.payload.TravelArrivedPayload;
import com.encorecraft.encorecarts.station.Station;
import com.encorecraft.encorecarts.station.StationLayout;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Active trips. Timeline (server ticks):
 * start → traveller hidden from others, frozen, invulnerable, destination chunks preloaded, cutscene sent;
 * client reports "screen black" (or timeout) → teleport, traveller visible again at the destination;
 * a short grace period keeps the traveller invulnerable while their screen fades in.
 */
public final class TravelManager {
    public static final int CUTSCENE_SHOTS = 10;
    /** Earliest teleport, so a modified client cannot skip the whole trip. */
    private static final int MIN_TELEPORT_TICKS = 60;
    private static final int ARRIVAL_GRACE_TICKS = 40;
    private static final int PRELOAD_RADIUS = 2;
    private static final TicketType<ChunkPos> PRELOAD_TICKET =
            TicketType.create("encorecarts_travel", Comparator.comparingLong(ChunkPos::toLong), 20 * 20);

    private enum State { CUTSCENE, ARRIVED }

    private static final class Session {
        final int id;
        final UUID playerId;
        final String destinationName;
        final Vec3 arrival;
        final float arrivalYaw;
        final Vec3 startPos;
        final long startTick;
        final long paid;
        State state = State.CUTSCENE;
        boolean clientReady;
        long arrivedTick;

        Session(int id, UUID playerId, String destinationName, Vec3 arrival, float arrivalYaw, Vec3 startPos, long startTick, long paid) {
            this.id = id;
            this.playerId = playerId;
            this.destinationName = destinationName;
            this.arrival = arrival;
            this.arrivalYaw = arrivalYaw;
            this.startPos = startPos;
            this.startTick = startTick;
            this.paid = paid;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static int nextSessionId = 1;

    private TravelManager() {
    }

    public static boolean isTraveling(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    static void start(ServerPlayer player, Station origin, Station destination, long paid) {
        ServerLevel level = player.serverLevel();
        StationLayout target = destination.layout();
        Session session = new Session(nextSessionId++, player.getUUID(), destination.name(), target.arrivalSpot(),
                target.arrivalYaw(), player.position(), player.server.getTickCount(), paid);
        SESSIONS.put(player.getUUID(), session);

        ChunkPos targetChunk = new ChunkPos(BlockPos.containing(session.arrival));
        level.getChunkSource().addRegionTicket(PRELOAD_TICKET, targetChunk, PRELOAD_RADIUS, targetChunk);

        player.stopUsingItem();
        player.setDeltaMovement(Vec3.ZERO);
        TravelVisibility.hide(player);

        int shot = player.getRandom().nextInt(CUTSCENE_SHOTS);
        Vec3 anchor = origin.anchor();
        PacketDistributor.sendToPlayer(player, new StartCutscenePayload(session.id, shot,
                entityId(level, origin.cartId()), entityId(level, origin.coachmanId()),
                anchor.x, anchor.y, anchor.z, origin.yaw(), destination.name()));
        EncoreCarts.LOGGER.debug("{} travels {} -> {} (session {}, shot {})",
                player.getGameProfile().getName(), origin.name(), destination.name(), session.id, shot);
    }

    private static int entityId(ServerLevel level, @Nullable UUID uuid) {
        Entity entity = uuid == null ? null : level.getEntity(uuid);
        return entity == null ? -1 : entity.getId();
    }

    public static void onClientReady(ServerPlayer player, int sessionId) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null && session.id == sessionId) {
            session.clientReady = true;
        }
    }

    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        int timeout = CartsConfig.CUTSCENE_TIMEOUT_TICKS.get();
        Iterator<Session> it = SESSIONS.values().iterator();
        while (it.hasNext()) {
            Session session = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            if (player == null) {
                TravelVisibility.forget(session.playerId);
                it.remove();
                continue;
            }
            long age = now - session.startTick;
            if (session.state == State.CUTSCENE) {
                if (player.position().distanceToSqr(session.startPos) > 0.04) {
                    player.connection.teleport(session.startPos.x, session.startPos.y, session.startPos.z, player.getYRot(), player.getXRot());
                }
                if ((session.clientReady && age >= MIN_TELEPORT_TICKS) || age >= timeout) {
                    arrive(player, session);
                    session.state = State.ARRIVED;
                    session.arrivedTick = now;
                }
            } else if (now - session.arrivedTick >= ARRIVAL_GRACE_TICKS) {
                it.remove();
            }
        }
    }

    private static void arrive(ServerPlayer player, Session session) {
        ServerLevel level = player.serverLevel();
        Vec3 spot = StationLayout.safeSpot(level, session.arrival);
        player.stopRiding();
        player.teleportTo(level, spot.x, spot.y, spot.z, session.arrivalYaw, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        TravelVisibility.unhide(player);
        PacketDistributor.sendToPlayer(player, new TravelArrivedPayload(session.id));
    }

    /** True while the player must not be hurt (cutscene and the fade-in right after the arrival). */
    public static boolean isProtected(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** True while the player is still at the departure station watching the cutscene. */
    public static boolean isInCutscene(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.state == State.CUTSCENE;
    }

    /** A disconnecting traveller is saved at the destination: the trip was paid for. */
    public static void onLogout(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        TravelVisibility.forget(player.getUUID());
        if (session != null && session.state == State.CUTSCENE) {
            Vec3 spot = StationLayout.safeSpot(player.serverLevel(), session.arrival);
            player.moveTo(spot.x, spot.y, spot.z, session.arrivalYaw, 0.0F);
        }
    }

    /** Death or a forced dimension change before arrival cancels the trip and refunds it. */
    public static void cancel(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        TravelVisibility.unhide(player);
        PacketDistributor.sendToPlayer(player, new TravelAbortedPayload(session.id));
        if (session.state == State.CUTSCENE && session.paid > 0 && !Economy.service().deposit(player, session.paid)) {
            EncoreCarts.LOGGER.error("Could not refund {} copper to {}", session.paid, player.getGameProfile().getName());
        }
    }

    public static void reset() {
        SESSIONS.clear();
        TravelVisibility.reset();
    }
}
