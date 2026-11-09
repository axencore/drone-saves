package com.encorecraft.encorecarts.economy.stub;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.economy.Coin;
import com.encorecraft.encorecarts.economy.CoinText;
import com.encorecraft.encorecarts.economy.Economy;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
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
 * TEST stand-ins for EncoreCraft's wallet commands ({@code addCoinPlayer <player> <c|i|g|p> <amount>} etc.),
 * registered only when EncoreCraft is absent and {@code economy.testWallet} is enabled.
 */
public final class TestWalletCommands {
    private static final SuggestionProvider<CommandSourceStack> COIN_TYPES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Coin.values()).map(Coin::code), builder);

    private enum Op { ADD, DEL, SET }

    private TestWalletCommands() {
    }

    public static void registerIfNeeded(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!CartsConfig.TEST_WALLET.get() || Economy.isEncoreCraftInstalled()
                || dispatcher.getRoot().getChild("infoCoinPlayer") != null) {
            return;
        }
        EncoreCarts.LOGGER.info("EncoreCraft not found: registering TEST wallet commands (economy.testWallet)");
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
                                .then(Commands.argument("coin", LongArgumentType.longArg(0, 1_000_000_000L))
                                        .executes(ctx -> change(ctx, op))))));
    }

    private static int change(CommandContext<CommandSourceStack> ctx, Op op) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Optional<Coin> coin = Coin.byCode(StringArgumentType.getString(ctx, "coin_type"));
        if (coin.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Unknown coin type (c, i, g, p)"));
            return 0;
        }
        long amount = LongArgumentType.getLong(ctx, "coin") * coin.get().copperValue();
        TestWalletData data = TestWalletData.get(ctx.getSource().getServer());
        long current = data.wallet(player.getUUID());
        long next = switch (op) {
            case ADD -> current + amount;
            case DEL -> current - amount;
            case SET -> current - current % (coin.get().copperValue() * 100) / coin.get().copperValue() * coin.get().copperValue() * 0 + amount;
        };
        if (next < 0) {
            ctx.getSource().sendFailure(Component.literal("Not enough coins: " + CoinText.format(current).getString()));
            return 0;
        }
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
