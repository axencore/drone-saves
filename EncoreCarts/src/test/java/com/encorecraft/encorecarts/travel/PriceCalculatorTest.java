package com.encorecraft.encorecarts.travel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PriceCalculatorTest {
    @Test
    void everyStartedSegmentCostsFullPrice() {
        assertEquals(15, PriceCalculator.price(1, 15));
        assertEquals(15, PriceCalculator.price(100, 15));
        assertEquals(30, PriceCalculator.price(100.01, 15));
        assertEquals(30, PriceCalculator.price(200, 15));
        assertEquals(165, PriceCalculator.price(1050, 15));
    }

    @Test
    void zeroDistanceStillCostsOneSegment() {
        assertEquals(15, PriceCalculator.price(0, 15));
    }

    @Test
    void freeWhenCostIsZero() {
        assertEquals(0, PriceCalculator.price(5000, 0));
    }
}
