package com.encorecraft.encorecarts.economy.encorecraft;

import com.encorecraft.encorecarts.economy.CurrencyService;
import com.encorecraft.encorecarts.economy.PaymentResult;
import java.util.OptionalLong;
import net.mcreator.encorecraftnew.api.EncoreCraftEconomyApi;
import net.minecraft.server.level.ServerPlayer;

/**
 * Personal wallet of EncoreCraftNew 2.x through its Java API. The wallet is a single copper amount
 * (100 copper = 1 silver, 100 silver = 1 gold, 100 gold = 1 platinum), so paying with silver or gold needs no
 * change making on our side. Only loaded when the {@code encorecraftnew} mod is present.
 */
public final class EncoreCraftWallet implements CurrencyService {
    @Override
    public String id() {
        return "encorecraftnew-api";
    }

    @Override
    public OptionalLong balance(ServerPlayer player) {
        if (!EncoreCraftEconomyApi.isEnabled()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Math.max(0L, EncoreCraftEconomyApi.getWallet(player)));
    }

    @Override
    public PaymentResult withdraw(ServerPlayer player, long copper) {
        if (copper <= 0) {
            return PaymentResult.SUCCESS;
        }
        if (!EncoreCraftEconomyApi.isEnabled()) {
            return PaymentResult.UNAVAILABLE;
        }
        // Check and deduction happen in one call on the server thread.
        if (EncoreCraftEconomyApi.trySpendWallet(player, copper)) {
            return PaymentResult.SUCCESS;
        }
        return EncoreCraftEconomyApi.getWallet(player) < copper ? PaymentResult.INSUFFICIENT_FUNDS : PaymentResult.UNAVAILABLE;
    }

    @Override
    public boolean deposit(ServerPlayer player, long copper) {
        return copper <= 0 || EncoreCraftEconomyApi.giveWallet(player, copper);
    }
}
