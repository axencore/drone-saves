package com.encorecraft.encorecarts.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CoinMathTest {
    private static CoinPurse purse(long c, long i, long g, long p) {
        return new CoinPurse(c, i, g, p);
    }

    @Test
    void fromCopperUsesLargestCoins() {
        assertEquals(purse(5, 4, 3, 2), CoinMath.fromCopper(2_030_405));
        assertEquals(purse(0, 0, 0, 0), CoinMath.fromCopper(0));
        assertEquals(purse(0, 0, 0, 150), CoinMath.fromCopper(150_000_000));
    }

    @Test
    void totalUsesExchangeRate() {
        assertEquals(1_010_101, purse(1, 1, 1, 1).totalCopper());
    }

    @Test
    void setCoinReplacesOnlyThatDenomination() {
        long wallet = purse(7, 42, 3, 0).totalCopper();
        assertEquals(purse(7, 5, 3, 0).totalCopper(), CoinMath.setCoin(wallet, Coin.SILVER, CoinMath.toCopper(5, Coin.SILVER)));
        assertEquals(purse(0, 42, 3, 0).totalCopper(), CoinMath.setCoin(wallet, Coin.COPPER, 0));
        assertEquals(purse(7, 42, 3, 12).totalCopper(), CoinMath.setCoin(wallet, Coin.PLATINUM, CoinMath.toCopper(12, Coin.PLATINUM)));
        // More than 99 of a lower coin carries over, exactly like EncoreCraft's own command.
        assertEquals(purse(7, 50, 4, 0).totalCopper(), CoinMath.setCoin(wallet, Coin.SILVER, CoinMath.toCopper(150, Coin.SILVER)));
    }

    @Test
    void setCoinMatchesEncoreCraftFormula() {
        Random random = new Random(42);
        for (int n = 0; n < 10_000; n++) {
            long wallet = random.nextLong(0, 5_000_000_000L);
            Coin coin = Coin.values()[random.nextInt(4)];
            long amount = random.nextInt(300) * coin.copperValue();
            long[] split = {wallet % 100, wallet / 100 % 100, wallet / 10_000 % 100, wallet / 1_000_000};
            long expected = wallet - split[coin.ordinal()] * coin.copperValue() + amount;
            assertEquals(expected, CoinMath.setCoin(wallet, coin, amount));
        }
    }

    @Test
    void debitClampsAndCreditSaturates() {
        assertEquals(0, CoinMath.subtractClamped(50, 80));
        assertEquals(30, CoinMath.subtractClamped(80, 50));
        assertEquals(Long.MAX_VALUE, CoinMath.add(Long.MAX_VALUE - 5, 10));
        assertEquals(15, CoinMath.add(5, 10));
    }

    @Test
    void toCopperRoundsLikeEncoreCraft() {
        assertEquals(150, CoinMath.toCopper(1.5, Coin.SILVER));
        assertEquals(0, CoinMath.toCopper(-1, Coin.GOLD));
        assertEquals(0, CoinMath.toCopper(Double.NaN, Coin.GOLD));
        assertEquals(1, CoinMath.toCopper(0.6, Coin.COPPER));
        assertEquals(Long.MAX_VALUE, CoinMath.toCopper(1e300, Coin.PLATINUM));
    }

    @Test
    void parsesCoinNamesLettersAndLegacyAlias() {
        assertEquals(Optional.of(Coin.SILVER), Coin.parse("i"));
        assertEquals(Optional.of(Coin.SILVER), Coin.parse(" Silver "));
        assertEquals(Optional.of(Coin.COPPER), Coin.parse("cooper"));
        assertEquals(Optional.of(Coin.PLATINUM), Coin.parse("p"));
        assertEquals(Optional.empty(), Coin.parse("bronze"));
    }
}
