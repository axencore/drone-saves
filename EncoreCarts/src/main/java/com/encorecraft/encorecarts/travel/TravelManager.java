package com.encorecraft.encorecarts.travel;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.economy.Economy;
import com.encorecraft.encorecarts.network.payload.CutsceneProgressPayload;
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
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Active trips. The cutscene is a local scene on the traveller's client; the real cart never moves.
 * <ol>
 * <li>DEPARTING: traveller hidden from others, frozen and protected; destination chunks preloaded; the client
 * fades into the scene and reports {@code COVERED}.</li>
 * <li>TRAVELLING: the traveller is moved to the destination right away, so their client loads it (chunks, LoD)
 * behind the scene. The scene runs until the client reports {@code FINISHED} (or {@code maxTripSeconds}).</li>
 * <li>ARRIVED: visible again; a short grace period keeps them protected while the screen fades in.</li>
 * </ol>
 */
public final class TravelManager {
    /** Teleport even without the client's COVERED report (its fade to black takes 0.7 s). */
    private static final int DEPART_TIMEOUT_TICKS = 60;
    private static final int ARRIVAL_GRACE_TICKS = 40;
    private static final int PRELOAD_RADIUS = 3;
    private static final double FREEZE_TOLERANCE_SQR = 0.25;
    private static final TicketType<ChunkPos> PRELOAD_TICKET =
            TicketType.create("encorecarts_travel", Comparator.comparingLong(ChunkPos::toLong), 20 * 30);

    private enum State { DEPARTING, TRAVELLING, ARRIVED }

    private static final class Session {
        final int id;
        final UUID playerId;
        final Vec3 arrival;
        final float arrivalYaw;
        final Vec3 startPos;
        final long startTick;
        final long paid;
        State state = State.DEPARTING;
        Vec3 holdPos;
        long stateTick;
        boolean covered;
        boolean finished;

        Session(int id, UUID playerId, Vec3 arrival, float arrivalYaw, Vec3 startPos, long startTick, long paid) {
            this.id = id;
            this.playerId = playerId;
            this.arrival = arrival;
            this.arrivalYaw = arrivalYaw;
            this.startPos = startPos;
            this.holdPos = startPos;
            this.startTick = startTick;
            this.stateTick = startTick;
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
        Session session = new Session(nextSessionId++, player.getUUID(), target.arrivalSpot(), target.arrivalYaw(),
                player.position(), player.server.getTickCount(), paid);
        SESSIONS.put(player.getUUID(), session);

        ChunkPos targetChunk = new ChunkPos(BlockPos.containing(session.arrival));
        level.getChunkSource().addRegionTicket(PRELOAD_TICKET, targetChunk, PRELOAD_RADIUS, targetChunk);

        player.stopUsingItem();
        player.setDeltaMovement(Vec3.ZERO);
        TravelVisibility.hide(player);

        RouteKind route = RouteKind.forBiome(level.getBiome(BlockPos.containing(origin.anchor())));
        long seed = player.getRandom().nextLong();
        PacketDistributor.sendToPlayer(player, new StartCutscenePayload(session.id, route.ordinal(), seed,
                origin.name(), destination.name()));
        EncoreCarts.LOGGER.debug("{} travels {} -> {} (session {}, route {})",
                player.getGameProfile().getName(), origin.name(), destination.name(), session.id, route);
    }

    public static void onProgress(ServerPlayer player, int sessionId, int stage) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.id != sessionId) {
            return;
        }
        if (stage == CutsceneProgressPayload.COVERED) {
            session.covered = true;
        } else if (stage == CutsceneProgressPayload.FINISHED) {
            session.finished = true;
        }
    }

    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        long maxTripTicks = CartsConfig.MAX_TRIP_SECONDS.get() * 20L;
        Iterator<Session> it = SESSIONS.values().iterator();
        while (it.hasNext()) {
            Session session = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            if (player == null) {
                TravelVisibility.forget(session.playerId);
                it.remove();
                continue;
            }
            long inState = now - session.stateTick;
            switch (session.state) {
                case DEPARTING -> {
                    hold(player, session);
                    if (session.covered || inState >= DEPART_TIMEOUT_TICKS) {
                        teleport(player, session);
                        enter(session, State.TRAVELLING, now);
                    }
                }
                case TRAVELLING -> {
                    hold(player, session);
                    boolean timedOut = now - session.startTick >= maxTripTicks;
                    if (session.finished || timedOut) {
                        if (timedOut && !session.finished) {
                            PacketDistributor.sendToPlayer(player, new TravelAbortedPayload(session.id));
                        }
                        TravelVisibility.unhide(player);
                        enter(session, State.ARRIVED, now);
                    }
                }
                case ARRIVED -> {
                    if (inState >= ARRIVAL_GRACE_TICKS) {
                        it.remove();
                    }
                }
            }
        }
    }

    private static void enter(Session session, State state, long now) {
        session.state = state;
        session.stateTick = now;
    }

    /** Server-side freeze, so a modified client cannot walk around during the cutscene. */
    private static void hold(ServerPlayer player, Session session) {
        if (player.position().distanceToSqr(session.holdPos) > FREEZE_TOLERANCE_SQR) {
            player.connection.teleport(session.holdPos.x, session.holdPos.y, session.holdPos.z, player.getYRot(), player.getXRot());
        }
    }

    private static void teleport(ServerPlayer player, Session session) {
        ServerLevel level = player.serverLevel();
        Vec3 spot = StationLayout.safeSpot(level, session.arrival);
        player.stopRiding();
        player.teleportTo(level, spot.x, spot.y, spot.z, session.arrivalYaw, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        session.holdPos = spot;
        PacketDistributor.sendToPlayer(player, new TravelArrivedPayload(session.id));
    }

    /** True while the player must not be hurt or targeted (whole trip and the fade-in after it). */
    public static boolean isProtected(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** True while the cutscene is playing: the traveller cannot act. */
    public static boolean isInCutscene(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.state != State.ARRIVED;
    }

    /** A disconnecting traveller is saved at the destination: the trip was paid for. */
    public static void onLogout(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        TravelVisibility.forget(player.getUUID());
        if (session != null && session.state == State.DEPARTING) {
            Vec3 spot = StationLayout.safeSpot(player.serverLevel(), session.arrival);
            player.moveTo(spot.x, spot.y, spot.z, session.arrivalYaw, 0.0F);
        }
    }

    /** Death or a forced dimension change ends the trip; it is refunded if the traveller had not been moved yet. */
    public static void cancel(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        TravelVisibility.unhide(player);
        PacketDistributor.sendToPlayer(player, new TravelAbortedPayload(session.id));
        if (session.state == State.DEPARTING && session.paid > 0 && !Economy.service().deposit(player, session.paid)) {
            EncoreCarts.LOGGER.error("Could not refund {} copper to {}", session.paid, player.getGameProfile().getName());
        }
    }

    public static void reset() {
        SESSIONS.clear();
        TravelVisibility.reset();
    }
}
