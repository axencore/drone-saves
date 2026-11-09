package com.encorecraft.encorecarts.entity;

import com.encorecraft.encorecarts.station.StationManager;
import com.encorecraft.encorecarts.travel.TravelService;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;

/**
 * The stationary cart of a station. It carries no gameplay state of its own besides the station id;
 * rendering is fully data driven (see {@code assets/encorecarts/encorecarts_models/cart.json}).
 */
public class CartEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_STATION =
            SynchedEntityData.defineId(CartEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final int ORPHAN_CHECK_INTERVAL = 100;

    // Client-side render state. Only cutscene actors change these; plain fields keep this class server-safe.
    @Nullable
    public Horse renderHorse;
    public float wheelSpin;
    public float wheelSpinO;
    public float renderPitch;
    public float renderPitchO;
    public boolean cutsceneActor;

    public CartEntity(EntityType<? extends CartEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STATION, Optional.empty());
    }

    public Optional<UUID> stationId() {
        return this.entityData.get(DATA_STATION);
    }

    public void setStationId(@Nullable UUID id) {
        this.entityData.set(DATA_STATION, Optional.ofNullable(id));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % ORPHAN_CHECK_INTERVAL == 0) {
            StationManager.discardIfOrphaned(this, stationId());
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            stationId().ifPresent(id -> TravelService.openMenu(serverPlayer, id, null));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player
                && StationManager.canDismantle(player)) {
            StationManager.dismantle(player, stationId(), this);
            return true;
        }
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide() && reason.shouldDestroy() && level() instanceof ServerLevel serverLevel) {
            stationId().ifPresent(id -> StationManager.onStationEntityDestroyed(serverLevel.getServer(), id));
        }
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // The model (and its horse) reaches well beyond the collision box.
        return getBoundingBox().inflate(3.5, 1.5, 3.5);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Station")) {
            setStationId(tag.getUUID("Station"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        stationId().ifPresent(id -> tag.putUUID("Station", id));
    }
}
