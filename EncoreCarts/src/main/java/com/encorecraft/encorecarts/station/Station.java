package com.encorecraft.encorecarts.station;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A travel point: one cart + one coachman at a fixed anchor in one dimension. */
public final class Station {
    private final UUID id;
    private final String name;
    private final ResourceKey<Level> dimension;
    private final Vec3 anchor;
    private final float yaw;
    private final boolean global;
    @Nullable
    private UUID cartId;
    @Nullable
    private UUID coachmanId;

    public Station(UUID id, String name, ResourceKey<Level> dimension, Vec3 anchor, float yaw, boolean global) {
        this.id = id;
        this.name = name;
        this.dimension = dimension;
        this.anchor = anchor;
        this.yaw = yaw;
        this.global = global;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public Vec3 anchor() {
        return anchor;
    }

    public float yaw() {
        return yaw;
    }

    /** Global stations are open to everybody unless closed for a player by an admin. */
    public boolean global() {
        return global;
    }

    @Nullable
    public UUID cartId() {
        return cartId;
    }

    @Nullable
    public UUID coachmanId() {
        return coachmanId;
    }

    void setCartId(@Nullable UUID cartId) {
        this.cartId = cartId;
    }

    void setCoachmanId(@Nullable UUID coachmanId) {
        this.coachmanId = coachmanId;
    }

    public StationLayout layout() {
        return new StationLayout(anchor, yaw);
    }

    /** Horizontal distance between two station anchors, the basis of the price. */
    public double distanceTo(Station other) {
        double dx = other.anchor.x - anchor.x;
        double dz = other.anchor.z - anchor.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putString("Dimension", dimension.location().toString());
        tag.putDouble("X", anchor.x);
        tag.putDouble("Y", anchor.y);
        tag.putDouble("Z", anchor.z);
        tag.putFloat("Yaw", yaw);
        tag.putBoolean("Global", global);
        if (cartId != null) {
            tag.putUUID("Cart", cartId);
        }
        if (coachmanId != null) {
            tag.putUUID("Coachman", coachmanId);
        }
        return tag;
    }

    @Nullable
    static Station load(CompoundTag tag) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (!tag.hasUUID("Id") || dimensionId == null) {
            return null;
        }
        Station station = new Station(tag.getUUID("Id"), tag.getString("Name"),
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")),
                tag.getFloat("Yaw"), tag.getBoolean("Global"));
        if (tag.hasUUID("Cart")) {
            station.cartId = tag.getUUID("Cart");
        }
        if (tag.hasUUID("Coachman")) {
            station.coachmanId = tag.getUUID("Coachman");
        }
        return station;
    }
}
