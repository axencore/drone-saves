package com.encorecraft.encorecarts.economy;

/** Amounts of each denomination, used to display a copper total as coins. */
public record CoinPurse(long copper, long silver, long gold, long platinum) {
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

    /** Total value in copper (saturating). */
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
