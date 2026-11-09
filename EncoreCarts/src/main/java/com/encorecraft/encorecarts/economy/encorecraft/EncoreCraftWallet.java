package com.encorecraft.encorecarts.economy.encorecraft;

import com.encorecraft.encorecarts.economy.CurrencyService;
import com.encorecraft.encorecarts.economy.PaymentResult;
import java.util.OptionalLong;
import net.mcreator.encorecraftnew.api.EncoreCraftEconomyApi;
import net.minecraft.server.level.ServerPlayer;

/**
 * Personal wallet ("кошелёк", not the bank) of EncoreCraftNew 2.x through its public Java API
 * {@code net.mcreator.encorecraftnew.api.EncoreCraftEconomyApi}. The wallet is one copper total
 * (100 copper = 1 silver, 100 silver = 1 gold, 100 gold = 1 platinum), so no change making is needed here.
 * Only loaded when the {@code encorecraftnew} mod is present. Server thread only.
 */
public final class EncoreCraftWallet implements CurrencyService {
    @Override
    public String id() {
        return "encorecraftnew-api";
    }

    /** With EncoreCraft's economy switched off by its admin, there is no money to charge: trips are free. */
    @Override
    public boolean isFree() {
        return !EncoreCraftEconomyApi.isEnabled();
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
        if (copper <= 0 || !EncoreCraftEconomyApi.isEnabled()) {
            return PaymentResult.SUCCESS;
        }
        // trySpendWallet checks and deducts in one call (EconomyApiSystem.takeWallet).
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
