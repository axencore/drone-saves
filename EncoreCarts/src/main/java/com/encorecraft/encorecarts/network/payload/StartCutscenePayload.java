package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.station.StationNames;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → traveller only: play the travel cutscene at the departure station.
 * {@code cartEntityId}/{@code coachmanEntityId} are hidden on this client while the cutscene actors are shown (-1 if unloaded).
 */
public record StartCutscenePayload(int sessionId, int shot, int cartEntityId, int coachmanEntityId,
                                   double x, double y, double z, float yaw, String destinationName)
        implements CustomPacketPayload {
    public static final Type<StartCutscenePayload> TYPE = new Type<>(EncoreCarts.id("start_cutscene"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StartCutscenePayload> CODEC =
            StreamCodec.ofMember(StartCutscenePayload::write, StartCutscenePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeVarInt(shot);
        buf.writeVarInt(cartEntityId);
        buf.writeVarInt(coachmanEntityId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(yaw);
        buf.writeUtf(destinationName, StationNames.MAX_WIRE_LENGTH);
    }

    private static StartCutscenePayload read(RegistryFriendlyByteBuf buf) {
        return new StartCutscenePayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readUtf(StationNames.MAX_WIRE_LENGTH));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
