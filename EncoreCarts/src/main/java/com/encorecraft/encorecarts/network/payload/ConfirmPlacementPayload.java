package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.station.StationNames;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the admin confirmed the station name and the "global" checkbox. */
public record ConfirmPlacementPayload(int requestId, String name, boolean global) implements CustomPacketPayload {
    public static final Type<ConfirmPlacementPayload> TYPE = new Type<>(EncoreCarts.id("confirm_placement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfirmPlacementPayload> CODEC =
            StreamCodec.ofMember(ConfirmPlacementPayload::write, ConfirmPlacementPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(requestId);
        buf.writeUtf(name, StationNames.MAX_WIRE_LENGTH);
        buf.writeBoolean(global);
    }

    private static ConfirmPlacementPayload read(RegistryFriendlyByteBuf buf) {
        return new ConfirmPlacementPayload(buf.readVarInt(), buf.readUtf(StationNames.MAX_WIRE_LENGTH), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
