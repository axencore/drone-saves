package com.encorecraft.encorecarts.client.model.bb;

import java.util.List;
import java.util.Locale;

/**
 * A Blockbench animation with keyframes normalised to Blockbench 5 axis conventions (values are added to the
 * bone's Euler ZYX rotation in degrees / position in pixels). Sampling mirrors Blockbench's own interpolation.
 */
public final class BbAnimation {
    public enum Loop { LOOP, ONCE, HOLD }

    public enum Interpolation { LINEAR, CATMULLROM, BEZIER, STEP }

    public enum Channel { ROTATION, POSITION, SCALE }

    public static final class Keyframe {
        final float time;
        final Interpolation interpolation;
        /** data_points × xyz; Blockbench allows one or two (pre/post) points. */
        final Molang.Expr[][] points;
        final float[] bezierLeftTime;
        final float[] bezierLeftValue;
        final float[] bezierRightTime;
        final float[] bezierRightValue;

        public Keyframe(float time, Interpolation interpolation, Molang.Expr[][] points, float[] bezierLeftTime,
                        float[] bezierLeftValue, float[] bezierRightTime, float[] bezierRightValue) {
            this.time = time;
            this.interpolation = interpolation;
            this.points = points;
            this.bezierLeftTime = bezierLeftTime;
            this.bezierLeftValue = bezierLeftValue;
            this.bezierRightTime = bezierRightTime;
            this.bezierRightValue = bezierRightValue;
        }

        double value(int dataPoint, int axis, Molang.Context ctx) {
            int dp = Math.max(0, Math.min(points.length - 1, dataPoint));
            return points[dp][axis].eval(ctx);
        }

        int last() {
            return points.length - 1;
        }
    }

    /** Keyframes of one bone, each channel sorted by time. */
    public record BoneTrack(String boneUuid, String boneName, Keyframe[] rotation, Keyframe[] position, Keyframe[] scale) {
        Keyframe[] channel(Channel channel) {
            return switch (channel) {
                case ROTATION -> rotation;
                case POSITION -> position;
                case SCALE -> scale;
            };
        }
    }

    private static final double EPSILON = 1.0 / 1200.0;

    public final String name;
    public final Loop loop;
    public final float length;
    private final List<BoneTrack> tracks;
    private int[] boneIndices;

    public BbAnimation(String name, Loop loop, float length, List<BoneTrack> tracks) {
        this.name = name;
        this.loop = loop;
        this.length = length;
        this.tracks = tracks;
    }

    public List<BoneTrack> tracks() {
        return tracks;
    }

    /** Resolves bone references against a model (by uuid, then by case-insensitive name). */
    void bind(BbModel model) {
        boneIndices = new int[tracks.size()];
        for (int i = 0; i < tracks.size(); i++) {
            BoneTrack track = tracks.get(i);
            BbBone bone = model.boneByUuid(track.boneUuid());
            if (bone == null) {
                bone = model.bone(track.boneName());
            }
            boneIndices[i] = bone == null ? -1 : bone.index;
        }
    }

    /** Maps elapsed seconds to the animation's own time according to its loop mode. */
    public float localTime(float elapsedSeconds) {
        return switch (loop) {
            case LOOP -> length > 0 ? elapsedSeconds % length : 0;
            case ONCE, HOLD -> Math.min(Math.max(elapsedSeconds, 0), length);
        };
    }

    public boolean finished(float elapsedSeconds) {
        return loop == Loop.ONCE && elapsedSeconds > length;
    }

    /** Adds this animation (at local time {@code time}, weight {@code weight}) on top of {@code pose}. */
    public void apply(BbPose pose, float time, float weight, double lifeTime) {
        if (boneIndices == null || weight <= 0) {
            return;
        }
        Molang.Context ctx = new Molang.Context(time, lifeTime);
        double[] v = new double[3];
        boolean looping = loop == Loop.LOOP;
        for (int i = 0; i < tracks.size(); i++) {
            int bone = boneIndices[i];
            if (bone < 0 || bone >= pose.boneCount()) {
                continue;
            }
            BoneTrack track = tracks.get(i);
            if (sample(track.rotation(), time, looping, ctx, v)) {
                pose.addRotation(bone, (float) Math.toRadians(v[0]) * weight, (float) Math.toRadians(v[1]) * weight,
                        (float) Math.toRadians(v[2]) * weight);
            }
            if (sample(track.position(), time, looping, ctx, v)) {
                pose.addPosition(bone, (float) v[0] * weight, (float) v[1] * weight, (float) v[2] * weight);
            }
            if (sample(track.scale(), time, looping, ctx, v)) {
                pose.multiplyScale(bone, 1 + ((float) v[0] - 1) * weight, 1 + ((float) v[1] - 1) * weight,
                        1 + ((float) v[2] - 1) * weight);
            }
        }
    }

    /** Blockbench's BoneAnimator#interpolate for one channel. {@code kfs} must be sorted by time. */
    public static boolean sample(Keyframe[] kfs, float time, boolean looping, Molang.Context ctx, double[] out) {
        if (kfs.length == 0) {
            return false;
        }
        int beforeIndex = -1;
        int afterIndex = -1;
        for (int i = 0; i < kfs.length; i++) {
            if (kfs[i].time < time) {
                if (beforeIndex < 0 || kfs[i].time > kfs[beforeIndex].time) {
                    beforeIndex = i;
                }
            } else if (afterIndex < 0 || kfs[i].time < kfs[afterIndex].time) {
                afterIndex = i;
            }
        }
        Keyframe before = beforeIndex < 0 ? null : kfs[beforeIndex];
        Keyframe after = afterIndex < 0 ? null : kfs[afterIndex];
        Keyframe exact = null;
        if (before != null && Math.abs(before.time - time) < EPSILON) {
            exact = before;
        } else if (after != null && Math.abs(after.time - time) < EPSILON) {
            exact = after;
        } else if (before != null && before.interpolation == Interpolation.STEP) {
            exact = before;
        } else if (before != null && after == null) {
            exact = before;
        } else if (after != null && before == null) {
            exact = after;
        }
        if (exact != null) {
            int dp = (exact.time > time || Math.abs(exact.time - time) < EPSILON) ? 0 : exact.last();
            for (int axis = 0; axis < 3; axis++) {
                out[axis] = exact.value(dp, axis, ctx);
            }
            return true;
        }
        double alpha = (time - before.time) / (double) (after.time - before.time);
        if (before.interpolation == Interpolation.LINEAR
                && (after.interpolation == Interpolation.LINEAR || after.interpolation == Interpolation.STEP)) {
            for (int axis = 0; axis < 3; axis++) {
                double a = before.value(before.last(), axis, ctx);
                double b = after.value(0, axis, ctx);
                out[axis] = a + (b - a) * alpha;
            }
        } else if (before.interpolation == Interpolation.CATMULLROM || after.interpolation == Interpolation.CATMULLROM) {
            Keyframe beforePlus = beforeIndex > 0 ? kfs[beforeIndex - 1] : null;
            Keyframe afterPlus = beforeIndex + 2 < kfs.length ? kfs[beforeIndex + 2] : null;
            if (looping && kfs.length >= 3) {
                if (beforePlus == null) {
                    beforePlus = kfs[kfs.length - 2];
                }
                if (afterPlus == null) {
                    afterPlus = kfs[1];
                }
            }
            for (int axis = 0; axis < 3; axis++) {
                double p1 = before.value(before.last(), axis, ctx);
                double p2 = after.value(0, axis, ctx);
                double p0 = beforePlus != null && before.points.length == 1 ? beforePlus.value(beforePlus.last(), axis, ctx) : p1;
                double p3 = afterPlus != null && after.points.length == 1 ? afterPlus.value(0, axis, ctx) : p2;
                out[axis] = catmullRom(alpha, p0, p1, p2, p3);
            }
        } else {
            for (int axis = 0; axis < 3; axis++) {
                out[axis] = bezier(before, after, axis, time, ctx);
            }
        }
        return true;
    }

    static double catmullRom(double t, double p0, double p1, double p2, double p3) {
        double v0 = (p2 - p0) * 0.5;
        double v1 = (p3 - p1) * 0.5;
        double t2 = t * t;
        double t3 = t * t2;
        return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * t + p1;
    }

    private static double bezier(Keyframe before, Keyframe after, int axis, float time, Molang.Context ctx) {
        double v0 = before.value(before.last(), axis, ctx);
        double v1 = after.value(0, axis, ctx);
        double gap = after.time - before.time;
        double handleBefore = clamp(before.bezierRightTime[axis], 0, gap);
        double handleAfter = clamp(after.bezierLeftTime[axis], -gap, 0);
        double x0 = before.time;
        double x1 = before.time + handleBefore;
        double x2 = after.time + handleAfter;
        double x3 = after.time;
        double y0 = v0;
        double y1 = v0 + before.bezierRightValue[axis];
        double y2 = v1 + after.bezierLeftValue[axis];
        double y3 = v1;
        // x(s) is monotonic because the handles are clamped inside the segment: bisect for x(s) == time.
        double lo = 0;
        double hi = 1;
        for (int i = 0; i < 40; i++) {
            double mid = (lo + hi) * 0.5;
            if (cubic(mid, x0, x1, x2, x3) < time) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return cubic((lo + hi) * 0.5, y0, y1, y2, y3);
    }

    private static double cubic(double s, double a, double b, double c, double d) {
        double u = 1 - s;
        return u * u * u * a + 3 * u * u * s * b + 3 * u * s * s * c + s * s * s * d;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static Loop parseLoop(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase(Locale.ROOT)) {
            case "loop" -> Loop.LOOP;
            case "hold" -> Loop.HOLD;
            default -> Loop.ONCE;
        };
    }

    public static Interpolation parseInterpolation(String raw) {
        return switch (raw == null ? "" : raw.toLowerCase(Locale.ROOT)) {
            case "catmullrom" -> Interpolation.CATMULLROM;
            case "bezier" -> Interpolation.BEZIER;
            case "step" -> Interpolation.STEP;
            default -> Interpolation.LINEAR;
        };
    }
}
