package com.encorecraft.encorecarts.client;

import com.encorecraft.encorecarts.network.payload.SyncStationsPayload;
import java.util.List;

/** Stations open to the local player, as last sent by the server (used by map integrations). */
public final class ClientStations {
    private static volatile List<SyncStationsPayload.MapStation> stations = List.of();

    private ClientStations() {
    }

    public static List<SyncStationsPayload.MapStation> all() {
        return stations;
    }

    static void set(List<SyncStationsPayload.MapStation> list) {
        stations = List.copyOf(list);
    }

    static void clear() {
        stations = List.of();
    }
}
