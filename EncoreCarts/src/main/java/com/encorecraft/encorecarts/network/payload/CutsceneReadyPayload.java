package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the screen is fully black after the cart shot, the server may teleport now. */
public record CutsceneReadyPayload(int sessionId) implements CustomPacketPayload {
    public static final Type<CutsceneReadyPayload> TYPE = new Type<>(EncoreCarts.id("cutscene_ready"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CutsceneReadyPayload> CODEC =
            StreamCodec.ofMember(CutsceneReadyPayload::write, CutsceneReadyPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
    }

    private static CutsceneReadyPayload read(RegistryFriendlyByteBuf buf) {
        return new CutsceneReadyPayload(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
