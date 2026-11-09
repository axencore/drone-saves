package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: travel from the station the player is standing at to {@code destinationId}. */
public record RequestTravelPayload(UUID originId, UUID destinationId) implements CustomPacketPayload {
    public static final Type<RequestTravelPayload> TYPE = new Type<>(EncoreCarts.id("request_travel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestTravelPayload> CODEC =
            StreamCodec.ofMember(RequestTravelPayload::write, RequestTravelPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(originId);
        buf.writeUUID(destinationId);
    }

    private static RequestTravelPayload read(RegistryFriendlyByteBuf buf) {
        return new RequestTravelPayload(buf.readUUID(), buf.readUUID());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
