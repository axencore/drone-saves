package com.encorecraft.encorecarts.client.model.bb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class BbAnimationTest {
    private static BbAnimation.Keyframe kf(float time, BbAnimation.Interpolation interpolation, double x) {
        Molang.Expr[][] points = {{Molang.constant(x), Molang.ZERO, Molang.ZERO}};
        float[] zero = {0, 0, 0};
        return new BbAnimation.Keyframe(time, interpolation, points, new float[]{-0.1F, -0.1F, -0.1F}, zero,
                new float[]{0.1F, 0.1F, 0.1F}, zero);
    }

    private static double sample(BbAnimation.Keyframe[] kfs, float time, boolean loop) {
        double[] out = new double[3];
        BbAnimation.sample(kfs, time, loop, Molang.Context.ZERO, out);
        return out[0];
    }

    @Test
    void linearInterpolatesAndClampsOutsideTheKeys() {
        BbAnimation.Keyframe[] kfs = {kf(0, BbAnimation.Interpolation.LINEAR, 0), kf(1, BbAnimation.Interpolation.LINEAR, 10)};
        assertEquals(0, sample(kfs, 0, false), 1e-9);
        assertEquals(2.5, sample(kfs, 0.25F, false), 1e-6);
        assertEquals(10, sample(kfs, 1, false), 1e-9);
        assertEquals(10, sample(kfs, 3, false), 1e-9);
    }

    @Test
    void stepHoldsThePreviousValue() {
        BbAnimation.Keyframe[] kfs = {kf(0, BbAnimation.Interpolation.STEP, 3), kf(1, BbAnimation.Interpolation.LINEAR, 7)};
        assertEquals(3, sample(kfs, 0.99F, false), 1e-9);
        assertEquals(7, sample(kfs, 1, false), 1e-9);
    }

    @Test
    void catmullRomPassesThroughKeysAndMatchesThreeJs() {
        BbAnimation.Keyframe[] kfs = {
                kf(0, BbAnimation.Interpolation.CATMULLROM, 0),
                kf(1, BbAnimation.Interpolation.CATMULLROM, 10),
                kf(2, BbAnimation.Interpolation.CATMULLROM, 0)};
        assertEquals(10, sample(kfs, 1, false), 1e-9);
        // Segment 0..1 without a key before: p0 = p1 = 0, p2 = 10, p3 = 0 (three.js SplineCurve semantics).
        assertEquals(BbAnimation.catmullRom(0.5, 0, 0, 10, 0), sample(kfs, 0.5F, false), 1e-6);
        assertEquals(5.625, BbAnimation.catmullRom(0.5, 0, 0, 10, 0), 1e-9);
    }

    @Test
    void bezierWithFlatHandlesEasesInAndOut() {
        BbAnimation.Keyframe[] kfs = {kf(0, BbAnimation.Interpolation.BEZIER, 0), kf(1, BbAnimation.Interpolation.BEZIER, 10)};
        assertEquals(5, sample(kfs, 0.5F, false), 1e-3);
        double early = sample(kfs, 0.1F, false);
        assertEquals(true, early > 0 && early < 1.0, "eased start: " + early);
    }

    @Test
    void emptyChannelProducesNothing() {
        assertFalse(BbAnimation.sample(new BbAnimation.Keyframe[0], 0.5F, false, Molang.Context.ZERO, new double[3]));
    }

    @Test
    void molangSubset() {
        assertEquals(1000, Molang.compile("1e3").eval(Molang.Context.ZERO), 1e-9);
        assertEquals(-8, Molang.compile("-2*(3+1)").eval(Molang.Context.ZERO), 1e-9);
        assertEquals(1, Molang.compile("math.sin(90)").eval(Molang.Context.ZERO), 1e-9);
        assertEquals(3, Molang.compile("q.anim_time * 2").eval(new Molang.Context(1.5, 0)), 1e-9);
        assertEquals(0, Molang.compile("this is not molang ((").eval(Molang.Context.ZERO), 1e-9);
        assertArrayEquals(new double[]{4}, new double[]{Molang.compile(" 4 ").eval(Molang.Context.ZERO)}, 1e-9);
    }
}
