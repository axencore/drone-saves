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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;

/**
 * The coachman standing next to a station cart. Talking to him opens the travel menu.
 * He never moves, never despawns and can only be removed by an admin (or commands such as /kill).
 */
public class CoachmanEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> DATA_STATION =
            SynchedEntityData.defineId(CoachmanEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_SLIM =
            SynchedEntityData.defineId(CoachmanEntity.class, EntityDataSerializers.BOOLEAN);
    public static final byte EVENT_GREET = 101;
    private static final int ORPHAN_CHECK_INTERVAL = 100;

    // Client-side animation state; plain fields keep the class loadable on a dedicated server.
    public int greetStartTick = Integer.MIN_VALUE / 2;
    public boolean cutsceneSeated;

    public CoachmanEntity(EntityType<? extends CoachmanEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 1.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATION, Optional.empty());
        builder.define(DATA_SLIM, false);
    }

    public Optional<UUID> stationId() {
        return this.entityData.get(DATA_STATION);
    }

    public void setStationId(@Nullable UUID id) {
        this.entityData.set(DATA_STATION, Optional.ofNullable(id));
    }

    public boolean isSlim() {
        return this.entityData.get(DATA_SLIM);
    }

    public void setSlim(boolean slim) {
        this.entityData.set(DATA_SLIM, slim);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % ORPHAN_CHECK_INTERVAL == 0) {
            StationManager.discardIfOrphaned(this, stationId());
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            getLookControl().setLookAt(player, 180.0F, 180.0F);
            level().broadcastEntityEvent(this, EVENT_GREET);
            stationId().ifPresent(id -> TravelService.openMenu(serverPlayer, id, null));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_GREET) {
            this.greetStartTick = this.tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && source.getEntity() instanceof ServerPlayer player
                && source.getDirectEntity() == player && StationManager.canDismantle(player)) {
            StationManager.dismantle(player, stationId(), this);
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide() && reason.shouldDestroy() && level() instanceof ServerLevel serverLevel) {
            stationId().ifPresent(id -> StationManager.onStationEntityDestroyed(serverLevel.getServer(), id));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
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
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Station")) {
            setStationId(tag.getUUID("Station"));
        }
        setSlim(tag.getBoolean("Slim"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        stationId().ifPresent(id -> tag.putUUID("Station", id));
        tag.putBoolean("Slim", isSlim());
    }
}
