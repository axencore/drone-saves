package com.encorecraft.encorecarts.economy;

/** Pure wallet arithmetic for display: splitting a copper amount into coins. */
public final class CoinMath {
    private static final Coin[] ASCENDING = Coin.values();

    private CoinMath() {
    }

    /** Smallest number of coins worth exactly {@code copper}. */
    public static CoinPurse fromCopper(long copper) {
        if (copper < 0) {
            throw new IllegalArgumentException("Negative amount " + copper);
        }
        long[] amounts = new long[ASCENDING.length];
        long rest = copper;
        for (int j = ASCENDING.length - 1; j >= 0; j--) {
            long value = ASCENDING[j].copperValue();
            amounts[j] = rest / value;
            rest %= value;
        }
        return new CoinPurse(amounts[0], amounts[1], amounts[2], amounts[3]);
    }
}
