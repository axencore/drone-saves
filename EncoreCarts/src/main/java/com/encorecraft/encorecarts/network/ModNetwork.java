package com.encorecraft.encorecarts.network;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.network.payload.ConfirmPlacementPayload;
import com.encorecraft.encorecarts.network.payload.CutsceneReadyPayload;
import com.encorecraft.encorecarts.network.payload.OpenPlacementScreenPayload;
import com.encorecraft.encorecarts.network.payload.PlacementResultPayload;
import com.encorecraft.encorecarts.network.payload.RequestTravelPayload;
import com.encorecraft.encorecarts.network.payload.StartCutscenePayload;
import com.encorecraft.encorecarts.network.payload.SyncStationsPayload;
import com.encorecraft.encorecarts.network.payload.TravelAbortedPayload;
import com.encorecraft.encorecarts.network.payload.TravelArrivedPayload;
import com.encorecraft.encorecarts.network.payload.TravelMenuPayload;
import com.encorecraft.encorecarts.station.PlacementRequests;
import com.encorecraft.encorecarts.travel.TravelManager;
import com.encorecraft.encorecarts.travel.TravelService;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Payload registration. Serverbound handlers run on the server thread and re-validate everything. */
public final class ModNetwork {
    private static final String PROTOCOL = "1";
    @Nullable
    private static volatile ClientPacketHandler clientHandler;

    private ModNetwork() {
    }

    public static void setClientHandler(ClientPacketHandler handler) {
        clientHandler = handler;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(EncoreCarts.MOD_ID).versioned(PROTOCOL);

        registrar.playToServer(ConfirmPlacementPayload.TYPE, ConfirmPlacementPayload.CODEC,
                (payload, ctx) -> asServerPlayer(ctx, player -> PlacementRequests.confirm(player, payload)));
        registrar.playToServer(RequestTravelPayload.TYPE, RequestTravelPayload.CODEC,
                (payload, ctx) -> asServerPlayer(ctx, player -> TravelService.requestTravel(player, payload.originId(), payload.destinationId())));
        registrar.playToServer(CutsceneReadyPayload.TYPE, CutsceneReadyPayload.CODEC,
                (payload, ctx) -> asServerPlayer(ctx, player -> TravelManager.onClientReady(player, payload.sessionId())));

        registrar.playToClient(OpenPlacementScreenPayload.TYPE, OpenPlacementScreenPayload.CODEC,
                (payload, ctx) -> client(handler -> handler.openPlacementScreen(payload)));
        registrar.playToClient(PlacementResultPayload.TYPE, PlacementResultPayload.CODEC,
                (payload, ctx) -> client(handler -> handler.placementResult(payload)));
        registrar.playToClient(TravelMenuPayload.TYPE, TravelMenuPayload.CODEC,
                (payload, ctx) -> client(handler -> handler.openTravelMenu(payload)));
        registrar.playToClient(StartCutscenePayload.TYPE, StartCutscenePayload.CODEC,
                (payload, ctx) -> client(handler -> handler.startCutscene(payload)));
        registrar.playToClient(TravelArrivedPayload.TYPE, TravelArrivedPayload.CODEC,
                (payload, ctx) -> client(handler -> handler.travelArrived(payload)));
        registrar.playToClient(TravelAbortedPayload.TYPE, TravelAbortedPayload.CODEC,
                (payload, ctx) -> client(handler -> handler.travelAborted(payload)));
        registrar.playToClient(SyncStationsPayload.TYPE, SyncStationsPayload.CODEC,
                (payload, ctx) -> client(handler -> handler.syncStations(payload)));
    }

    private static void asServerPlayer(IPayloadContext ctx, Consumer<ServerPlayer> action) {
        if (ctx.player() instanceof ServerPlayer player) {
            action.accept(player);
        }
    }

    private static void client(Consumer<ClientPacketHandler> action) {
        ClientPacketHandler handler = clientHandler;
        if (handler != null) {
            action.accept(handler);
        }
    }
}
