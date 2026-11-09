package com.encorecraft.encorecarts.economy.command;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.economy.Coin;
import com.encorecraft.encorecarts.economy.CoinMath;
import com.encorecraft.encorecarts.economy.CoinPurse;
import com.encorecraft.encorecarts.economy.CurrencyService;
import com.encorecraft.encorecarts.economy.PaymentResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerPlayer;

/**
 * Wallet backend that drives EncoreCraft through its server commands:
 * {@code infoCoinPlayer} to read, {@code delCoinPlayer}/{@code addCoinPlayer} to pay and give change.
 * Removals are applied first and rolled back on failure, so a failing command never creates money.
 */
public final class EncoreCraftCommandWallet implements CurrencyService {
    private record Applied(Coin coin, long amount, boolean removed) {
    }

    @Override
    public String id() {
        return "encorecraft-commands";
    }

    @Override
    public OptionalLong balance(ServerPlayer player) {
        return readPurse(player).map(purse -> OptionalLong.of(purse.totalCopper())).orElse(OptionalLong.empty());
    }

    public Optional<CoinPurse> readPurse(ServerPlayer player) {
        String command = format(CartsConfig.CMD_INFO.get(), player, null, 0);
        CommandRunner.Result result = CommandRunner.run(player.server, command);
        if (!result.success()) {
            EncoreCarts.LOGGER.warn("Wallet info command '{}' failed: {}", command, result.text());
            return Optional.empty();
        }
        Optional<CoinPurse> purse = new WalletInfoParser(CartsConfig.coinAliases()).parse(result.text());
        if (purse.isEmpty()) {
            EncoreCarts.LOGGER.warn("Could not parse wallet of {} from '{}'. Adjust [economy.infoAliases] in encorecarts-common.toml.",
                    player.getGameProfile().getName(), result.text());
        }
        return purse;
    }

    @Override
    public PaymentResult withdraw(ServerPlayer player, long copper) {
        if (copper <= 0) {
            return PaymentResult.SUCCESS;
        }
        Optional<CoinPurse> before = readPurse(player);
        if (before.isEmpty()) {
            return PaymentResult.UNAVAILABLE;
        }
        Optional<CoinPurse> after = CoinMath.pay(before.get(), copper);
        if (after.isEmpty()) {
            return PaymentResult.INSUFFICIENT_FUNDS;
        }
        Map<Coin, Long> delta = CoinMath.delta(before.get(), after.get());
        List<Applied> applied = new ArrayList<>();
        for (Map.Entry<Coin, Long> entry : delta.entrySet()) {
            if (entry.getValue() < 0) {
                if (!change(player, entry.getKey(), -entry.getValue(), true)) {
                    rollback(player, applied);
                    return PaymentResult.UNAVAILABLE;
                }
                applied.add(new Applied(entry.getKey(), -entry.getValue(), true));
            }
        }
        for (Map.Entry<Coin, Long> entry : delta.entrySet()) {
            if (entry.getValue() > 0) {
                if (!change(player, entry.getKey(), entry.getValue(), false)) {
                    rollback(player, applied);
                    return PaymentResult.UNAVAILABLE;
                }
                applied.add(new Applied(entry.getKey(), entry.getValue(), false));
            }
        }
        readPurse(player).ifPresent(check -> {
            if (check.totalCopper() != before.get().totalCopper() - copper) {
                EncoreCarts.LOGGER.warn("Wallet of {} is {} after paying {} (expected {}). Check the EncoreCraft command semantics.",
                        player.getGameProfile().getName(), check, copper, after.get());
            }
        });
        return PaymentResult.SUCCESS;
    }

    @Override
    public boolean deposit(ServerPlayer player, long copper) {
        if (copper <= 0) {
            return true;
        }
        CoinPurse coins = CoinMath.fromCopper(copper);
        boolean ok = true;
        for (Coin coin : Coin.values()) {
            long amount = coins.get(coin);
            if (amount > 0) {
                ok &= change(player, coin, amount, false);
            }
        }
        return ok;
    }

    private void rollback(ServerPlayer player, List<Applied> applied) {
        for (int i = applied.size() - 1; i >= 0; i--) {
            Applied step = applied.get(i);
            if (!change(player, step.coin(), step.amount(), !step.removed())) {
                EncoreCarts.LOGGER.error("Could not roll back wallet change {} for {}", step, player.getGameProfile().getName());
            }
        }
    }

    private boolean change(ServerPlayer player, Coin coin, long amount, boolean remove) {
        String template = remove ? CartsConfig.CMD_DEL.get() : CartsConfig.CMD_ADD.get();
        String command = format(template, player, coin, amount);
        CommandRunner.Result result = CommandRunner.run(player.server, command);
        if (!result.success()) {
            EncoreCarts.LOGGER.warn("Wallet command '{}' failed: {}", command, result.text());
        }
        return result.success();
    }

    private static String format(String template, ServerPlayer player, Coin coin, long amount) {
        String command = template
                .replace("{player}", player.getGameProfile().getName())
                .replace("{type}", coin == null ? "" : coin.code())
                .replace("{amount}", Long.toString(amount))
                .trim();
        return command.startsWith("/") ? command.substring(1) : command;
    }
}
