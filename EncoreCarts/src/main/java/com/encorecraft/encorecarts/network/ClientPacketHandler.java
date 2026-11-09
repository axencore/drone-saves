package com.encorecraft.encorecarts.network;

import com.encorecraft.encorecarts.network.payload.OpenPlacementScreenPayload;
import com.encorecraft.encorecarts.network.payload.PlacementResultPayload;
import com.encorecraft.encorecarts.network.payload.StartCutscenePayload;
import com.encorecraft.encorecarts.network.payload.SyncStationsPayload;
import com.encorecraft.encorecarts.network.payload.TravelAbortedPayload;
import com.encorecraft.encorecarts.network.payload.TravelArrivedPayload;
import com.encorecraft.encorecarts.network.payload.TravelMenuPayload;

/**
 * Client-side receiver of clientbound payloads. The implementation is installed by the client entry point,
 * so common code never links against client classes (dedicated servers never install one).
 */
public interface ClientPacketHandler {
    void openPlacementScreen(OpenPlacementScreenPayload payload);

    void placementResult(PlacementResultPayload payload);

    void openTravelMenu(TravelMenuPayload payload);

    void startCutscene(StartCutscenePayload payload);

    void travelArrived(TravelArrivedPayload payload);

    void travelAborted(TravelAbortedPayload payload);

    void syncStations(SyncStationsPayload payload);
}
