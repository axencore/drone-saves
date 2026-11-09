package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → traveller: the teleport to the destination has been performed. */
public record TravelArrivedPayload(int sessionId) implements CustomPacketPayload {
    public static final Type<TravelArrivedPayload> TYPE = new Type<>(EncoreCarts.id("travel_arrived"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TravelArrivedPayload> CODEC =
            StreamCodec.ofMember(TravelArrivedPayload::write, TravelArrivedPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
    }

    private static TravelArrivedPayload read(RegistryFriendlyByteBuf buf) {
        return new TravelArrivedPayload(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
