package com.encorecraft.encorecarts.economy;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Pure wallet arithmetic: paying a copper price from a multi-denomination purse with change. */
public final class CoinMath {
    private static final Coin[] ASCENDING = Coin.values();

    private CoinMath() {
    }

    /**
     * Pays {@code cost} copper. Lower denominations are spent first; when they run out, the smallest
     * sufficient higher coins are broken and the change is returned as lower denominations.
     *
     * @return the purse after payment, or empty if the purse is worth less than {@code cost}
     */
    public static Optional<CoinPurse> pay(CoinPurse purse, long cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("Negative cost " + cost);
        }
        if (purse.totalCopper() < cost) {
            return Optional.empty();
        }
        long[] amounts = new long[ASCENDING.length];
        for (int i = 0; i < ASCENDING.length; i++) {
            amounts[i] = purse.get(ASCENDING[i]);
        }
        long remaining = cost;
        for (int i = 0; i < ASCENDING.length && remaining > 0; i++) {
            long value = ASCENDING[i].copperValue();
            long needed = ceilDiv(remaining, value);
            if (amounts[i] >= needed) {
                amounts[i] -= needed;
                long change = needed * value - remaining;
                remaining = 0;
                distributeChange(amounts, i, change);
            } else {
                remaining -= amounts[i] * value;
                amounts[i] = 0;
            }
        }
        if (remaining > 0) {
            return Optional.empty();
        }
        return Optional.of(new CoinPurse(amounts[0], amounts[1], amounts[2], amounts[3]));
    }

    /** Smallest number of coins worth exactly {@code copper}, used for refunds. */
    public static CoinPurse fromCopper(long copper) {
        if (copper < 0) {
            throw new IllegalArgumentException("Negative amount " + copper);
        }
        long[] amounts = new long[ASCENDING.length];
        distributeChange(amounts, ASCENDING.length, copper);
        return new CoinPurse(amounts[0], amounts[1], amounts[2], amounts[3]);
    }

    /** Per-coin difference {@code after - before}; zero entries are omitted. */
    public static Map<Coin, Long> delta(CoinPurse before, CoinPurse after) {
        Map<Coin, Long> result = new EnumMap<>(Coin.class);
        for (Coin coin : ASCENDING) {
            long diff = after.get(coin) - before.get(coin);
            if (diff != 0) {
                result.put(coin, diff);
            }
        }
        return result;
    }

    /** Adds {@code change} copper to the denominations strictly below index {@code above}, largest first. */
    private static void distributeChange(long[] amounts, int above, long change) {
        for (int j = above - 1; j >= 0 && change > 0; j--) {
            long value = ASCENDING[j].copperValue();
            amounts[j] += change / value;
            change %= value;
        }
    }

    private static long ceilDiv(long a, long b) {
        return -Math.floorDiv(-a, b);
    }
}
