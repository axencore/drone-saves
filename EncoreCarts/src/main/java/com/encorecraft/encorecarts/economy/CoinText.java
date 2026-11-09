package com.encorecraft.encorecarts.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Human readable coin amounts for chat messages, e.g. "1 зол. 25 сер. 3 мед.". */
public final class CoinText {
    private CoinText() {
    }

    public static MutableComponent format(long copper) {
        CoinPurse coins = CoinMath.fromCopper(Math.max(0, copper));
        MutableComponent result = Component.empty();
        boolean any = false;
        Coin[] order = {Coin.PLATINUM, Coin.GOLD, Coin.SILVER, Coin.COPPER};
        for (Coin coin : order) {
            long amount = coins.get(coin);
            if (amount > 0 || (!any && coin == Coin.COPPER)) {
                if (any) {
                    result.append(" ");
                }
                result.append(Component.translatable(coin.translationKey() + ".amount", amount));
                any = true;
            }
        }
        return result;
    }
}
