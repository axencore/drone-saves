package com.encorecraft.encorecarts.economy;

/** Amounts of each denomination in a wallet. Never negative. */
public record CoinPurse(long copper, long silver, long gold, long platinum) {
    public static final CoinPurse EMPTY = new CoinPurse(0, 0, 0, 0);

    public CoinPurse {
        if (copper < 0 || silver < 0 || gold < 0 || platinum < 0) {
            throw new IllegalArgumentException("Negative coin amount: " + copper + "/" + silver + "/" + gold + "/" + platinum);
        }
    }

    public long get(Coin coin) {
        return switch (coin) {
            case COPPER -> copper;
            case SILVER -> silver;
            case GOLD -> gold;
            case PLATINUM -> platinum;
        };
    }

    public CoinPurse with(Coin coin, long amount) {
        return switch (coin) {
            case COPPER -> new CoinPurse(amount, silver, gold, platinum);
            case SILVER -> new CoinPurse(copper, amount, gold, platinum);
            case GOLD -> new CoinPurse(copper, silver, amount, platinum);
            case PLATINUM -> new CoinPurse(copper, silver, gold, amount);
        };
    }

    /** Total value in copper (saturating, wallets of absurd size do not overflow into negatives). */
    public long totalCopper() {
        long total = 0;
        for (Coin coin : Coin.values()) {
            long part = get(coin);
            if (part > (Long.MAX_VALUE - total) / coin.copperValue()) {
                return Long.MAX_VALUE;
            }
            total += part * coin.copperValue();
        }
        return total;
    }
}
