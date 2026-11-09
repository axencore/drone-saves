package com.encorecraft.encorecarts.economy;

/**
 * Pure arithmetic on a wallet stored as one copper total, matching EncoreCraftNew 2.x
 * ({@code CurrencyAmounts}): saturating credit, debit clamped at zero, per-coin "digits" for display and
 * for {@code setCoinPlayer}.
 */
public final class CoinMath {
    private static final Coin[] ASCENDING = Coin.values();

    private CoinMath() {
    }

    /** Smallest number of coins worth exactly {@code copper}; platinum takes everything above 99 gold. */
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

    /** {@code amount} coins of {@code coin} in copper, rounded like EncoreCraft; non-positive or NaN → 0. */
    public static long toCopper(double amount, Coin coin) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return 0;
        }
        double copper = amount * coin.copperValue();
        return copper >= 9.223372036854776E18 ? Long.MAX_VALUE : Math.max(0L, Math.round(copper));
    }

    public static long add(long wallet, long copper) {
        long w = Math.max(0, wallet);
        long c = Math.max(0, copper);
        return Long.MAX_VALUE - w < c ? Long.MAX_VALUE : w + c;
    }

    /** Debit that never goes below zero (EncoreCraft's {@code delCoinPlayer} behaviour). */
    public static long subtractClamped(long wallet, long copper) {
        return Math.max(0L, Math.max(0, wallet) - Math.max(0, copper));
    }

    /**
     * Replaces the amount of one denomination and keeps the others, e.g. setting silver to 5 in
     * "3 gold 42 silver 7 copper" gives "3 gold 5 silver 7 copper" ({@code setCoinPlayer}).
     */
    public static long setCoin(long wallet, Coin coin, long newAmountCopper) {
        long w = Math.max(0, wallet);
        long current = fromCopper(w).get(coin) * coin.copperValue();
        return add(w - current, newAmountCopper);
    }
}
