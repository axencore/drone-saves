package com.encorecraft.encorecarts.client.cutscene;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Straight road the cutscene cart follows from the departure station. The direction closest to the cart's
 * facing with the least obstructed, flattest ground is chosen; heights follow the terrain and are smoothed.
 */
final class CutscenePath {
    static final double STEP = 0.5;
    private static final double LENGTH = 40.0;
    private static final float[] CANDIDATE_DELTAS = {0, 15, -15, 30, -30, 45, -45, 90, -90, 180};

    private final Vec3 start;
    private final float yaw;
    private final Vec3 forward;
    private final Vec3 right;
    private final double[] heights;

    private CutscenePath(Vec3 start, float yaw, double[] heights) {
        this.start = start;
        this.yaw = yaw;
        double r = Math.toRadians(yaw);
        this.forward = new Vec3(-Math.sin(r), 0, Math.cos(r));
        this.right = new Vec3(-Math.cos(r), 0, -Math.sin(r));
        this.heights = heights;
    }

    static CutscenePath build(Level level, Vec3 origin, float stationYaw) {
        double bestCost = Double.MAX_VALUE;
        float bestYaw = stationYaw;
        double[] best = null;
        for (float delta : CANDIDATE_DELTAS) {
            float yaw = stationYaw + delta;
            double r = Math.toRadians(yaw);
            Vec3 dir = new Vec3(-Math.sin(r), 0, Math.cos(r));
            double[] heights = probe(level, origin, dir);
            double cost = cost(level, origin, dir, heights) + Math.abs(delta) * 0.04;
            if (cost < bestCost) {
                bestCost = cost;
                bestYaw = yaw;
                best = heights;
            }
        }
        smooth(best);
        smooth(best);
        return new CutscenePath(origin, Mth.wrapDegrees(bestYaw), best);
    }

    private static double[] probe(Level level, Vec3 origin, Vec3 dir) {
        int samples = (int) (LENGTH / STEP) + 1;
        double[] heights = new double[samples];
        double y = origin.y;
        for (int i = 0; i < samples; i++) {
            double s = i * STEP;
            y = groundY(level, origin.x + dir.x * s, origin.z + dir.z * s, y);
            heights[i] = y;
        }
        return heights;
    }

    private static double cost(Level level, Vec3 origin, Vec3 dir, double[] heights) {
        double cost = 0;
        int checked = Math.min(heights.length, (int) (24 / STEP));
        for (int i = 1; i < checked; i++) {
            double dh = Math.abs(heights[i] - heights[i - 1]);
            cost += dh;
            if (dh > 0.6) {
                cost += 6;
            }
            if (i % 2 == 0) {
                double s = i * STEP;
                BlockPos body = BlockPos.containing(origin.x + dir.x * s, heights[i] + 1.2, origin.z + dir.z * s);
                if (!level.getBlockState(body).getCollisionShape(level, body).isEmpty()) {
                    cost += 15;
                }
            }
        }
        return cost;
    }

    /** Top of the walkable surface near {@code refY} with ~2 blocks of headroom, or {@code refY} if none. */
    private static double groundY(Level level, double x, double z, double refY) {
        int top = Mth.floor(refY) + 3;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= Mth.floor(refY) - 4; y--) {
            pos.set(Mth.floor(x), y, Mth.floor(z));
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            double surface = y + shape.max(Direction.Axis.Y);
            if (surface > refY + 3.01) {
                continue;
            }
            BlockPos above = BlockPos.containing(x, surface + 0.5, z);
            BlockPos head = BlockPos.containing(x, surface + 1.5, z);
            if (level.getBlockState(above).getCollisionShape(level, above).isEmpty()
                    && level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
                return surface;
            }
        }
        return refY;
    }

    private static void smooth(double[] h) {
        double[] copy = h.clone();
        for (int i = 0; i < h.length; i++) {
            double sum = 0;
            int n = 0;
            for (int k = Math.max(0, i - 2); k <= Math.min(h.length - 1, i + 2); k++) {
                sum += copy[k];
                n++;
            }
            h[i] = sum / n;
        }
        h[0] = copy[0];
    }

    double height(double s) {
        double index = Mth.clamp(s / STEP, 0, heights.length - 1);
        int i = (int) Math.floor(index);
        int j = Math.min(heights.length - 1, i + 1);
        return Mth.lerp(index - i, heights[i], heights[j]);
    }

    Vec3 position(double s) {
        return new Vec3(start.x + forward.x * s, height(s), start.z + forward.z * s);
    }

    /** Nose-up pitch of a cart centred at {@code s}, in degrees. */
    float pitch(double s) {
        double half = 1.2;
        return (float) Math.toDegrees(Math.atan2(height(s + half) - height(s - half), half * 2));
    }

    float yaw() {
        return yaw;
    }

    Vec3 forward() {
        return forward;
    }

    Vec3 right() {
        return right;
    }

    Vec3 start() {
        return start;
    }
}
