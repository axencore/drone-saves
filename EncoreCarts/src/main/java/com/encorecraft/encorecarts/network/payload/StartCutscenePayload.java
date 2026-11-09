package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.station.StationNames;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → traveller only: start the travel cutscene. {@code route} is a {@code RouteKind} id and {@code seed}
 * makes the client-side scene (decorations, camera cuts) reproducible.
 */
public record StartCutscenePayload(int sessionId, int route, long seed, String originName, String destinationName)
        implements CustomPacketPayload {
    public static final Type<StartCutscenePayload> TYPE = new Type<>(EncoreCarts.id("start_cutscene"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StartCutscenePayload> CODEC =
            StreamCodec.ofMember(StartCutscenePayload::write, StartCutscenePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeVarInt(route);
        buf.writeLong(seed);
        buf.writeUtf(originName, StationNames.MAX_WIRE_LENGTH);
        buf.writeUtf(destinationName, StationNames.MAX_WIRE_LENGTH);
    }

    private static StartCutscenePayload read(RegistryFriendlyByteBuf buf) {
        return new StartCutscenePayload(buf.readVarInt(), buf.readVarInt(), buf.readLong(),
                buf.readUtf(StationNames.MAX_WIRE_LENGTH), buf.readUtf(StationNames.MAX_WIRE_LENGTH));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
