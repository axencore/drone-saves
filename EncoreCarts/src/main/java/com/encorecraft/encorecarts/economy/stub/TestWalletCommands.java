package com.encorecraft.encorecarts.economy.stub;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.economy.Coin;
import com.encorecraft.encorecarts.economy.CoinPurse;
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
import net.neoforged.fml.ModList;

/**
 * TEST stand-in for the EncoreCraft wallet commands so the whole payment pipeline (command execution,
 * output parsing, change making) can be exercised without EncoreCraft. Registered only when EncoreCraft
 * is absent and {@code economy.stubWalletCommands} is enabled.
 */
public final class TestWalletCommands {
    private static final SuggestionProvider<CommandSourceStack> COIN_TYPES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Coin.values()).map(Coin::code), builder);

    private enum Op { ADD, DEL, SET }

    private TestWalletCommands() {
    }

    public static boolean isEncoreCraftInstalled() {
        return CartsConfig.encoreCraftModIds().stream().anyMatch(ModList.get()::isLoaded);
    }

    public static void registerIfNeeded(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!CartsConfig.STUB_WALLET_COMMANDS.get() || isEncoreCraftInstalled()
                || dispatcher.getRoot().getChild("infoCoinPlayer") != null) {
            return;
        }
        EncoreCarts.LOGGER.info("EncoreCraft not found: registering TEST wallet commands (economy.stubWalletCommands)");
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
                                .then(Commands.argument("coin", LongArgumentType.longArg(0))
                                        .executes(ctx -> change(ctx, op))))));
    }

    private static int change(CommandContext<CommandSourceStack> ctx, Op op) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        Optional<Coin> coin = Coin.byCode(StringArgumentType.getString(ctx, "coin_type"));
        long amount = LongArgumentType.getLong(ctx, "coin");
        if (coin.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Unknown coin type (c, i, g, p)"));
            return 0;
        }
        TestWalletData data = TestWalletData.get(ctx.getSource().getServer());
        CoinPurse purse = data.wallet(player.getUUID());
        long current = purse.get(coin.get());
        long next = switch (op) {
            case ADD -> current + amount;
            case DEL -> current - amount;
            case SET -> amount;
        };
        if (next < 0) {
            ctx.getSource().sendFailure(Component.literal("Not enough " + coin.get().code() + " coins: " + current));
            return 0;
        }
        data.setWallet(player.getUUID(), purse.with(coin.get(), next));
        ctx.getSource().sendSuccess(() -> Component.literal("[EncoreCarts test wallet] " + player.getGameProfile().getName()
                + ": " + coin.get().code() + "=" + next), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        CoinPurse purse = TestWalletData.get(ctx.getSource().getServer()).wallet(player.getUUID());
        String text = "[EncoreCarts test wallet] " + player.getGameProfile().getName()
                + ": c=" + purse.copper() + ", i=" + purse.silver() + ", g=" + purse.gold() + ", p=" + purse.platinum();
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
