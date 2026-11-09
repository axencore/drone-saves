package com.encorecraft.encorecarts.economy;

import java.util.Locale;
import java.util.Optional;

/** EncoreCraft wallet denominations. Exchange rate: 100 of a coin = 1 of the next one. */
public enum Coin {
    COPPER("copper", "c", 1L),
    SILVER("silver", "i", 100L),
    GOLD("gold", "g", 10_000L),
    PLATINUM("platinum", "p", 1_000_000L);

    private final String id;
    private final String letter;
    private final long copperValue;

    Coin(String id, String letter, long copperValue) {
        this.id = id;
        this.letter = letter;
        this.copperValue = copperValue;
    }

    /** Name used by EncoreCraft commands and suggestions (copper / silver / gold / platinum). */
    public String id() {
        return id;
    }

    public long copperValue() {
        return copperValue;
    }

    public String translationKey() {
        return "encorecarts.coin." + id;
    }

    /** Same rules as EncoreCraft's {@code CoinType.parse}: id, legacy letter (c/i/g/p) or "cooper". */
    public static Optional<Coin> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.equals("cooper")) {
            return Optional.of(COPPER);
        }
        for (Coin coin : values()) {
            if (coin.id.equals(s) || coin.letter.equals(s)) {
                return Optional.of(coin);
            }
        }
        return Optional.empty();
    }
}
