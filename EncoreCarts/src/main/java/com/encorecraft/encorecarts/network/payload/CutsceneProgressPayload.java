package com.encorecraft.encorecarts.network.payload;

import com.encorecraft.encorecarts.EncoreCarts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server progress of the travel cutscene:
 * {@link #COVERED} the scene now hides the world, so the traveller may be moved to the destination;
 * {@link #FINISHED} the destination is loaded and the scene is fading out.
 */
public record CutsceneProgressPayload(int sessionId, int stage) implements CustomPacketPayload {
    public static final int COVERED = 0;
    public static final int FINISHED = 1;

    public static final Type<CutsceneProgressPayload> TYPE = new Type<>(EncoreCarts.id("cutscene_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CutsceneProgressPayload> CODEC =
            StreamCodec.ofMember(CutsceneProgressPayload::write, CutsceneProgressPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeVarInt(stage);
    }

    private static CutsceneProgressPayload read(RegistryFriendlyByteBuf buf) {
        return new CutsceneProgressPayload(buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
