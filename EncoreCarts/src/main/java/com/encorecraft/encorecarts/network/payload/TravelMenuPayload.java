package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.station.StationNames;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: open (or refresh) the coachman menu. Prices and affordability are computed by the server;
 * the client only displays them, the server re-checks everything when a destination is requested.
 */
public record TravelMenuPayload(UUID originId, String originName, boolean balanceKnown, long balance,
                                long copperPer100Blocks, List<Destination> destinations,
                                Optional<Component> notice) implements CustomPacketPayload {
    public static final Type<TravelMenuPayload> TYPE = new Type<>(EncoreCarts.id("travel_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TravelMenuPayload> CODEC =
            StreamCodec.ofMember(TravelMenuPayload::write, TravelMenuPayload::read);
    private static final int MAX_DESTINATIONS = 1024;

    public record Destination(UUID id, String name, int distance, long price, boolean affordable) {
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(originId);
        buf.writeUtf(originName, StationNames.MAX_WIRE_LENGTH);
        buf.writeBoolean(balanceKnown);
        buf.writeVarLong(balance);
        buf.writeVarLong(copperPer100Blocks);
        buf.writeVarInt(destinations.size());
        for (Destination d : destinations) {
            buf.writeUUID(d.id());
            buf.writeUtf(d.name(), StationNames.MAX_WIRE_LENGTH);
            buf.writeVarInt(d.distance());
            buf.writeVarLong(d.price());
            buf.writeBoolean(d.affordable());
        }
        buf.writeBoolean(notice.isPresent());
        notice.ifPresent(component -> ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, component));
    }

    private static TravelMenuPayload read(RegistryFriendlyByteBuf buf) {
        UUID originId = buf.readUUID();
        String originName = buf.readUtf(StationNames.MAX_WIRE_LENGTH);
        boolean balanceKnown = buf.readBoolean();
        long balance = buf.readVarLong();
        long cost = buf.readVarLong();
        int count = Math.min(buf.readVarInt(), MAX_DESTINATIONS);
        List<Destination> destinations = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            destinations.add(new Destination(buf.readUUID(), buf.readUtf(StationNames.MAX_WIRE_LENGTH), buf.readVarInt(),
                    buf.readVarLong(), buf.readBoolean()));
        }
        Optional<Component> notice = buf.readBoolean()
                ? Optional.of(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf))
                : Optional.empty();
        return new TravelMenuPayload(originId, originName, balanceKnown, balance, cost, destinations, notice);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
