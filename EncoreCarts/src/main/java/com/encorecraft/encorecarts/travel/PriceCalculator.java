package com.encorecraft.encorecarts.travel;

/** Price rule: every started 100-block segment of horizontal distance costs the full segment price. */
public final class PriceCalculator {
    public static final double SEGMENT_BLOCKS = 100.0;

    private PriceCalculator() {
    }

    public static long segments(double distance) {
        if (!(distance > 0)) {
            return 1;
        }
        // Tiny epsilon absorbs floating point noise at exact multiples of 100.
        return Math.max(1L, (long) Math.ceil(distance / SEGMENT_BLOCKS - 1e-9));
    }

    public static long price(double distance, long copperPerSegment) {
        long segments = segments(distance);
        if (copperPerSegment <= 0) {
            return 0;
        }
        if (segments > Long.MAX_VALUE / copperPerSegment) {
            return Long.MAX_VALUE;
        }
        return segments * copperPerSegment;
    }
}
