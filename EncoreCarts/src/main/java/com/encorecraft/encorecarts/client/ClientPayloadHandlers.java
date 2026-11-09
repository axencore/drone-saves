package com.encorecraft.encorecarts.client;

import com.encorecraft.encorecarts.client.cutscene.CutsceneController;
import com.encorecraft.encorecarts.client.screen.CartPlacementScreen;
import com.encorecraft.encorecarts.client.screen.TravelMenuScreen;
import com.encorecraft.encorecarts.network.ClientPacketHandler;
import com.encorecraft.encorecarts.network.payload.OpenPlacementScreenPayload;
import com.encorecraft.encorecarts.network.payload.PlacementResultPayload;
import com.encorecraft.encorecarts.network.payload.StartCutscenePayload;
import com.encorecraft.encorecarts.network.payload.SyncStationsPayload;
import com.encorecraft.encorecarts.network.payload.TravelAbortedPayload;
import com.encorecraft.encorecarts.network.payload.TravelArrivedPayload;
import com.encorecraft.encorecarts.network.payload.TravelMenuPayload;
import net.minecraft.client.Minecraft;

/** Runs on the client main thread (payload handlers are registered for the main thread). */
final class ClientPayloadHandlers implements ClientPacketHandler {
    @Override
    public void openPlacementScreen(OpenPlacementScreenPayload payload) {
        if (!CutsceneController.isActive()) {
            Minecraft.getInstance().setScreen(new CartPlacementScreen(payload.requestId()));
        }
    }

    @Override
    public void placementResult(PlacementResultPayload payload) {
        if (Minecraft.getInstance().screen instanceof CartPlacementScreen screen && screen.requestId() == payload.requestId()) {
            screen.onResult(payload);
        }
    }

    @Override
    public void openTravelMenu(TravelMenuPayload payload) {
        if (CutsceneController.isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TravelMenuScreen screen && screen.originId().equals(payload.originId())) {
            screen.update(payload);
        } else {
            minecraft.setScreen(new TravelMenuScreen(payload));
        }
    }

    @Override
    public void startCutscene(StartCutscenePayload payload) {
        CutsceneController.start(payload);
    }

    @Override
    public void travelArrived(TravelArrivedPayload payload) {
        CutsceneController.onArrived(payload.sessionId());
    }

    @Override
    public void travelAborted(TravelAbortedPayload payload) {
        CutsceneController.abort();
    }

    @Override
    public void syncStations(SyncStationsPayload payload) {
        ClientStations.set(payload.stations());
    }
}
