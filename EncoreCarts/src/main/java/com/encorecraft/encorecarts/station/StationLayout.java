package com.encorecraft.encorecarts.station;

import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Geometry of a station around its anchor (centre of the cart on the ground) and yaw (direction the cart faces).
 * The coachman stands at the cart's right side near the front, travellers arrive at the right side near the rear.
 */
public record StationLayout(Vec3 anchor, float yaw) {
    private static final double SIDE_OFFSET = 1.8;
    private static final double COACHMAN_FORWARD = 0.9;
    private static final double ARRIVAL_FORWARD = -1.1;
    private static final double PLAYER_HALF_WIDTH = 0.3;
    private static final double PLAYER_HEIGHT = 1.8;

    /** Snaps a player's look direction to the nearest 45° so stations line up with roads. */
    public static float snapYaw(float yaw) {
        return Mth.wrapDegrees(Math.round(yaw / 45.0F) * 45.0F);
    }

    public Vec3 forward() {
        double r = Math.toRadians(yaw);
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    public Vec3 right() {
        double r = Math.toRadians(yaw);
        return new Vec3(-Math.cos(r), 0, -Math.sin(r));
    }

    /** Point relative to the anchor in the cart frame: {@code right}, {@code up}, {@code forward} in blocks. */
    public Vec3 local(double right, double up, double forward) {
        Vec3 f = forward();
        Vec3 r = right();
        return new Vec3(anchor.x + r.x * right + f.x * forward, anchor.y + up, anchor.z + r.z * right + f.z * forward);
    }

    public Vec3 coachmanSpot() {
        return local(SIDE_OFFSET, 0, COACHMAN_FORWARD);
    }

    public float coachmanYaw() {
        return Mth.wrapDegrees(yaw + 90.0F);
    }

    public Vec3 arrivalSpot() {
        return local(SIDE_OFFSET, 0, ARRIVAL_FORWARD);
    }

    /** Travellers step out facing away from the cart. */
    public float arrivalYaw() {
        return Mth.wrapDegrees(yaw + 90.0F);
    }

    /** Checks that the cart, its horse and both standing spots fit. */
    public Optional<Component> validate(CollisionGetter level) {
        for (double f = -1.8; f <= 4.01; f += 0.5) {
            for (double r = -0.9; r <= 0.91; r += 0.45) {
                for (double up = 0.25; up <= 2.3; up += 1.0) {
                    BlockPos pos = BlockPos.containing(local(r, up, f));
                    if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                        return Optional.of(Component.translatable("message.encorecarts.place.blocked"));
                    }
                }
            }
        }
        if (standingHeight(level, coachmanSpot()).isEmpty()) {
            return Optional.of(Component.translatable("message.encorecarts.place.no_coachman_spot"));
        }
        if (standingHeight(level, arrivalSpot()).isEmpty()) {
            return Optional.of(Component.translatable("message.encorecarts.place.no_arrival_spot"));
        }
        return Optional.empty();
    }

    /** Height a player-sized entity can stand at near {@code pos} (±2 blocks), if any. */
    public static OptionalDouble standingHeight(CollisionGetter level, Vec3 pos) {
        int baseY = Mth.floor(pos.y);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy : new int[]{0, -1, 1, -2, 2}) {
            cursor.set(Mth.floor(pos.x), baseY + dy - 1, Mth.floor(pos.z));
            BlockState ground = level.getBlockState(cursor);
            VoxelShape shape = ground.getCollisionShape(level, cursor);
            if (shape.isEmpty()) {
                continue;
            }
            double top = cursor.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
            AABB body = new AABB(pos.x - PLAYER_HALF_WIDTH, top + 0.001, pos.z - PLAYER_HALF_WIDTH,
                    pos.x + PLAYER_HALF_WIDTH, top + PLAYER_HEIGHT, pos.z + PLAYER_HALF_WIDTH);
            if (level.noCollision(body)) {
                return OptionalDouble.of(top);
            }
        }
        return OptionalDouble.empty();
    }

    /** {@code preferred} snapped to the ground, or the nearest standable spot within 3 blocks. */
    public static Vec3 safeSpot(CollisionGetter level, Vec3 preferred) {
        OptionalDouble y = standingHeight(level, preferred);
        if (y.isPresent()) {
            return new Vec3(preferred.x, y.getAsDouble(), preferred.z);
        }
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    Vec3 candidate = preferred.add(dx, 0, dz);
                    OptionalDouble cy = standingHeight(level, candidate);
                    if (cy.isPresent()) {
                        return new Vec3(candidate.x, cy.getAsDouble(), candidate.z);
                    }
                }
            }
        }
        return preferred;
    }
}
