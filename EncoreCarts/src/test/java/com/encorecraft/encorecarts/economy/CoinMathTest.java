package com.encorecraft.encorecarts.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoinMathTest {
    @Test
    void paysWithCopperFirst() {
        assertEquals(new CoinPurse(5, 1, 0, 0), CoinMath.pay(new CoinPurse(20, 1, 0, 0), 15).orElseThrow());
    }

    @Test
    void breaksSilverForChange() {
        assertEquals(new CoinPurse(85, 0, 0, 0), CoinMath.pay(new CoinPurse(0, 1, 0, 0), 15).orElseThrow());
    }

    @Test
    void breaksGoldAcrossTwoDenominations() {
        // 1 gold = 10000 copper; 10000 - 165 = 9835 = 98 silver + 35 copper
        assertEquals(new CoinPurse(35, 98, 0, 0), CoinMath.pay(new CoinPurse(0, 0, 1, 0), 165).orElseThrow());
    }

    @Test
    void combinesLowerCoinsBeforeBreakingHigherOnes() {
        assertEquals(new CoinPurse(90, 0, 1, 0), CoinMath.pay(new CoinPurse(5, 1, 1, 0), 15).orElseThrow());
    }

    @Test
    void refusesWhenTotalIsTooLow() {
        assertTrue(CoinMath.pay(new CoinPurse(99, 0, 0, 0), 100).isEmpty());
    }

    @Test
    void fromCopperUsesLargestCoins() {
        assertEquals(new CoinPurse(5, 3, 2, 1), CoinMath.fromCopper(1_020_305));
    }
}
