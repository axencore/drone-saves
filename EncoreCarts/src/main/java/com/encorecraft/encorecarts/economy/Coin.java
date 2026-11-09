package com.encorecraft.encorecarts.economy;

import java.util.Locale;
import java.util.Optional;

/** EncoreCraft wallet denominations. Exchange rate: 100 of a coin = 1 of the next one. */
public enum Coin {
    COPPER("c", 1L),
    SILVER("i", 100L),
    GOLD("g", 10_000L),
    PLATINUM("p", 1_000_000L);

    private final String code;
    private final long copperValue;

    Coin(String code, long copperValue) {
        this.code = code;
        this.copperValue = copperValue;
    }

    /** Coin type argument used by the EncoreCraft commands (c / i / g / p). */
    public String code() {
        return code;
    }

    public long copperValue() {
        return copperValue;
    }

    public String translationKey() {
        return "encorecarts.coin." + name().toLowerCase(Locale.ROOT);
    }

    public static Optional<Coin> byCode(String code) {
        for (Coin coin : values()) {
            if (coin.code.equalsIgnoreCase(code)) {
                return Optional.of(coin);
            }
        }
        return Optional.empty();
    }
}
