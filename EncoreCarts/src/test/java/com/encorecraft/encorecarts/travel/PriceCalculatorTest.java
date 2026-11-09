package com.encorecraft.encorecarts.travel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PriceCalculatorTest {
    @Test
    void everyStartedHundredBlocksCostsAFullSegment() {
        assertEquals(15, PriceCalculator.price(1, 15));
        assertEquals(15, PriceCalculator.price(100, 15));
        assertEquals(30, PriceCalculator.price(100.5, 15));
        assertEquals(30, PriceCalculator.price(199.9, 15));
        assertEquals(45, PriceCalculator.price(250, 15));
        assertEquals(150, PriceCalculator.price(1000, 15));
        assertEquals(165, PriceCalculator.price(1000.5, 15));
    }

    @Test
    void floatingPointNoiseAtExactMultiplesIsIgnored() {
        assertEquals(15, PriceCalculator.price(Math.sqrt(100 * 100), 15));
        assertEquals(30, PriceCalculator.price(Math.hypot(120, 160), 15));
    }

    @Test
    void customAndZeroPrices() {
        assertEquals(60, PriceCalculator.price(350, 15));
        assertEquals(80, PriceCalculator.price(350, 20));
        assertEquals(0, PriceCalculator.price(5000, 0));
    }
}
