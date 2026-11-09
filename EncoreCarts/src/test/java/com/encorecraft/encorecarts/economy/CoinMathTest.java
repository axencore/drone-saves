package com.encorecraft.encorecarts.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CoinMathTest {
    private static CoinPurse purse(long c, long i, long g, long p) {
        return new CoinPurse(c, i, g, p);
    }

    @Test
    void paysExactCopper() {
        assertEquals(Optional.of(purse(5, 0, 0, 0)), CoinMath.pay(purse(20, 0, 0, 0), 15));
    }

    @Test
    void breaksSilverWhenCopperRunsOut() {
        assertEquals(Optional.of(purse(85, 0, 0, 0)), CoinMath.pay(purse(0, 1, 0, 0), 15));
        // 10 copper + 140 from two silver coins, 60 copper back.
        assertEquals(Optional.of(purse(60, 0, 0, 0)), CoinMath.pay(purse(10, 2, 0, 0), 150));
    }

    @Test
    void breaksGoldIntoSilverAndCopper() {
        // 5 copper spent, one gold (10 000) broken for the remaining 40: 9 960 back as 99 silver + 60 copper.
        assertEquals(Optional.of(purse(60, 99, 0, 0)), CoinMath.pay(purse(5, 0, 1, 0), 45));
    }

    @Test
    void breaksPlatinum() {
        assertEquals(Optional.of(purse(99, 99, 99, 0)), CoinMath.pay(purse(0, 0, 0, 1), 1));
    }

    @Test
    void keepsHigherCoinsWhenLowerOnesSuffice() {
        assertEquals(Optional.of(purse(0, 1, 3, 2)), CoinMath.pay(purse(0, 2, 3, 2), 100));
    }

    @Test
    void refusesWhenWalletIsTooSmall() {
        assertEquals(Optional.empty(), CoinMath.pay(purse(14, 0, 0, 0), 15));
        assertEquals(Optional.empty(), CoinMath.pay(purse(99, 0, 0, 0), 100));
    }

    @Test
    void zeroCostLeavesWalletUntouched() {
        CoinPurse wallet = purse(1, 2, 3, 4);
        assertEquals(Optional.of(wallet), CoinMath.pay(wallet, 0));
    }

    @Test
    void paymentAlwaysRemovesExactlyTheCost() {
        Random random = new Random(1234);
        for (int n = 0; n < 20_000; n++) {
            CoinPurse wallet = purse(random.nextInt(250), random.nextInt(250), random.nextInt(5), random.nextInt(3));
            long cost = random.nextInt(60_000);
            Optional<CoinPurse> after = CoinMath.pay(wallet, cost);
            if (wallet.totalCopper() < cost) {
                assertTrue(after.isEmpty(), () -> wallet + " cannot pay " + cost);
            } else {
                assertTrue(after.isPresent(), () -> wallet + " should pay " + cost);
                assertEquals(wallet.totalCopper() - cost, after.get().totalCopper(), () -> wallet + " - " + cost);
            }
        }
    }

    @Test
    void fromCopperUsesLargestCoins() {
        assertEquals(purse(5, 4, 3, 2), CoinMath.fromCopper(2_030_405));
        assertEquals(purse(0, 0, 0, 0), CoinMath.fromCopper(0));
    }

    @Test
    void totalUsesExchangeRate() {
        assertEquals(1_010_101, purse(1, 1, 1, 1).totalCopper());
    }
}
