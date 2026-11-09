package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: outcome of a placement request (errors keep the naming screen open). */
public record PlacementResultPayload(int requestId, boolean success, Component message) implements CustomPacketPayload {
    public static final Type<PlacementResultPayload> TYPE = new Type<>(EncoreCarts.id("placement_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlacementResultPayload> CODEC =
            StreamCodec.ofMember(PlacementResultPayload::write, PlacementResultPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(requestId);
        buf.writeBoolean(success);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, message);
    }

    private static PlacementResultPayload read(RegistryFriendlyByteBuf buf) {
        return new PlacementResultPayload(buf.readVarInt(), buf.readBoolean(), ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
