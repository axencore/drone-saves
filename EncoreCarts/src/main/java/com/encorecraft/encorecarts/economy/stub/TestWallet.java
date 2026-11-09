package com.encorecraft.encorecarts.economy.stub;

import com.encorecraft.encorecarts.economy.CoinMath;
import com.encorecraft.encorecarts.economy.CurrencyService;
import com.encorecraft.encorecarts.economy.PaymentResult;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerPlayer;

/** TEST wallet used when EncoreCraftNew is not installed; one copper total per player, like EncoreCraft. */
public final class TestWallet implements CurrencyService {
    @Override
    public String id() {
        return "test-wallet";
    }

    @Override
    public OptionalLong balance(ServerPlayer player) {
        return OptionalLong.of(TestWalletData.get(player.server).wallet(player.getUUID()));
    }

    @Override
    public PaymentResult withdraw(ServerPlayer player, long copper) {
        if (copper <= 0) {
            return PaymentResult.SUCCESS;
        }
        TestWalletData data = TestWalletData.get(player.server);
        long balance = data.wallet(player.getUUID());
        if (balance < copper) {
            return PaymentResult.INSUFFICIENT_FUNDS;
        }
        data.setWallet(player.getUUID(), balance - copper);
        return PaymentResult.SUCCESS;
    }

    @Override
    public boolean deposit(ServerPlayer player, long copper) {
        if (copper > 0) {
            TestWalletData data = TestWalletData.get(player.server);
            data.setWallet(player.getUUID(), CoinMath.add(data.wallet(player.getUUID()), copper));
        }
        return true;
    }
}
