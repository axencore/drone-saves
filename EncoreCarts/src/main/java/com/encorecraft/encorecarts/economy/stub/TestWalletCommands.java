package com.encorecraft.encorecarts.economy.stub;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.economy.Coin;
import com.encorecraft.encorecarts.economy.CoinMath;
import com.encorecraft.encorecarts.economy.CoinText;
import com.encorecraft.encorecarts.economy.Economy;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * TEST stand-ins for EncoreCraftNew's wallet commands ({@code addCoinPlayer <player> <coin_type> <coin>} etc.)
 * with the same arguments and arithmetic, registered only when EncoreCraftNew is absent and
 * {@code economy.testWallet} is enabled.
 */
public final class TestWalletCommands {
    private static final SuggestionProvider<CommandSourceStack> COIN_TYPES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Coin.values()).map(Coin::id), builder);

    private enum Op { ADD, DEL, SET }

    private TestWalletCommands() {
    }

    public static void registerIfNeeded(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!CartsConfig.TEST_WALLET.get() || Economy.isEncoreCraftInstalled()
                || dispatcher.getRoot().getChild("infoCoinPlayer") != null) {
            return;
        }
        EncoreCarts.LOGGER.info("EncoreCraftNew not found: registering TEST wallet commands (economy.testWallet)");
        register(dispatcher, "addCoinPlayer", Op.ADD);
        register(dispatcher, "delCoinPlayer", Op.DEL);
        register(dispatcher, "setCoinPlayer", Op.SET);
        dispatcher.register(Commands.literal("infoCoinPlayer")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TestWalletCommands::info)));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String name, Op op) {
        dispatcher.register(Commands.literal(name)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("coin_type", StringArgumentType.word()).suggests(COIN_TYPES)
                                .then(Commands.argument("coin", DoubleArgumentType.doubleArg(0.0))
                                        .executes(ctx -> change(ctx, op))))));
    }

    private static int change(CommandContext<CommandSourceStack> ctx, Op op) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Optional<Coin> coin = Coin.parse(StringArgumentType.getString(ctx, "coin_type"));
        if (coin.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Unknown coin type. Use: copper, silver, gold, platinum"));
            return 0;
        }
        long amount = CoinMath.toCopper(DoubleArgumentType.getDouble(ctx, "coin"), coin.get());
        TestWalletData data = TestWalletData.get(ctx.getSource().getServer());
        long current = data.wallet(player.getUUID());
        long next = switch (op) {
            case ADD -> CoinMath.add(current, amount);
            case DEL -> CoinMath.subtractClamped(current, amount);
            case SET -> CoinMath.setCoin(current, coin.get(), amount);
        };
        data.setWallet(player.getUUID(), next);
        ctx.getSource().sendSuccess(() -> Component.literal("[EncoreCarts test wallet] " + player.getGameProfile().getName() + ": ")
                .append(CoinText.format(next)), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        long copper = TestWalletData.get(ctx.getSource().getServer()).wallet(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("[EncoreCarts test wallet] " + player.getGameProfile().getName() + ": ")
                .append(CoinText.format(copper)), false);
        return (int) Math.min(Integer.MAX_VALUE, copper);
    }
}
