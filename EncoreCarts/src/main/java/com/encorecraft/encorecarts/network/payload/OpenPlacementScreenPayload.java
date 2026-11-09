package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: ask the admin for the name of the station being placed. */
public record OpenPlacementScreenPayload(int requestId) implements CustomPacketPayload {
    public static final Type<OpenPlacementScreenPayload> TYPE = new Type<>(EncoreCarts.id("open_placement_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPlacementScreenPayload> CODEC =
            StreamCodec.ofMember(OpenPlacementScreenPayload::write, OpenPlacementScreenPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(requestId);
    }

    private static OpenPlacementScreenPayload read(RegistryFriendlyByteBuf buf) {
        return new OpenPlacementScreenPayload(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
