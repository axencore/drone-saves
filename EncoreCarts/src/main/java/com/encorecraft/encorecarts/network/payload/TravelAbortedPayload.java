package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → traveller: the trip was cancelled (death, dimension change...), stop the cutscene immediately. */
public record TravelAbortedPayload(int sessionId) implements CustomPacketPayload {
    public static final Type<TravelAbortedPayload> TYPE = new Type<>(EncoreCarts.id("travel_aborted"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TravelAbortedPayload> CODEC =
            StreamCodec.ofMember(TravelAbortedPayload::write, TravelAbortedPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
    }

    private static TravelAbortedPayload read(RegistryFriendlyByteBuf buf) {
        return new TravelAbortedPayload(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
