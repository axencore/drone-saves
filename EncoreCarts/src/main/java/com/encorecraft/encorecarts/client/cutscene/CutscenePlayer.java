package com.encorecraft.encorecarts.client.cutscene;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Look-alike of the local player sitting in the cutscene cart: same skin, skin layers and equipment.
 * Never added to the level; its vehicle makes the vanilla renderer use the sitting pose.
 */
final class CutscenePlayer extends RemotePlayer {
    private final AbstractClientPlayer source;
    @Nullable
    private Entity seat;

    CutscenePlayer(ClientLevel level, AbstractClientPlayer source) {
        super(level, new GameProfile(UUID.randomUUID(), source.getGameProfile().getName()));
        this.source = source;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = source.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                setItemSlot(slot, stack.copy());
            }
        }
        getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, source.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
    }

    void setSeat(@Nullable Entity seat) {
        this.seat = seat;
    }

    @Override
    @Nullable
    protected PlayerInfo getPlayerInfo() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getPlayerInfo(source.getUUID());
    }

    @Override
    public boolean isPassenger() {
        return seat != null;
    }

    @Override
    @Nullable
    public Entity getVehicle() {
        return seat;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }
}
