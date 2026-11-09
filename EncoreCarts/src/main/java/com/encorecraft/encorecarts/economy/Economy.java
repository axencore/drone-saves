package com.encorecraft.encorecarts.economy;

import com.encorecraft.encorecarts.economy.command.EncoreCraftCommandWallet;
import java.util.Objects;

/** Holder of the active {@link CurrencyService}. */
public final class Economy {
    private static CurrencyService service = new EncoreCraftCommandWallet();

    private Economy() {
    }

    public static CurrencyService service() {
        return service;
    }

    /** Swap point for a future direct EncoreCraft API backend. */
    public static void setService(CurrencyService newService) {
        service = Objects.requireNonNull(newService);
    }
}
