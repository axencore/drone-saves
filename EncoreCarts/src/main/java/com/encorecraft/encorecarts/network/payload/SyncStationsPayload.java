package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.station.StationNames;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server → client: every station currently open to this player (all dimensions), used for map icons. */
public record SyncStationsPayload(List<MapStation> stations) implements CustomPacketPayload {
    public static final Type<SyncStationsPayload> TYPE = new Type<>(EncoreCarts.id("sync_stations"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncStationsPayload> CODEC =
            StreamCodec.ofMember(SyncStationsPayload::write, SyncStationsPayload::read);
    private static final int MAX_STATIONS = 4096;

    public record MapStation(UUID id, String name, ResourceLocation dimension, int x, int y, int z) {
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(stations.size());
        for (MapStation s : stations) {
            buf.writeUUID(s.id());
            buf.writeUtf(s.name(), StationNames.MAX_WIRE_LENGTH);
            buf.writeResourceLocation(s.dimension());
            buf.writeVarInt(s.x());
            buf.writeVarInt(s.y());
            buf.writeVarInt(s.z());
        }
    }

    private static SyncStationsPayload read(RegistryFriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), MAX_STATIONS);
        List<MapStation> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new MapStation(buf.readUUID(), buf.readUtf(StationNames.MAX_WIRE_LENGTH), buf.readResourceLocation(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return new SyncStationsPayload(list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
