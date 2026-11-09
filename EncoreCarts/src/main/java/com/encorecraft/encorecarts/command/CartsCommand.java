package com.encorecraft.encorecarts.command;

import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.economy.CoinText;
import com.encorecraft.encorecarts.station.Station;
import com.encorecraft.encorecarts.station.StationData;
import com.encorecraft.encorecarts.station.StationManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /carts setcost <count>}, {@code /carts open|close <user> <location>}, plus {@code list} and {@code remove}.
 * Every change is persisted immediately (price → config file, access/stations → world data).
 */
public final class CartsCommand {
    private static final DynamicCommandExceptionType UNKNOWN_STATION = new DynamicCommandExceptionType(
            name -> Component.translatable("command.encorecarts.unknown_station", name));

    private static final SuggestionProvider<CommandSourceStack> STATIONS = (ctx, builder) -> {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String name : StationManager.names(ctx.getSource().getServer())) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.startsWith(typed) || lower.contains(" " + typed)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    };

    private CartsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("carts")
                .requires(source -> source.hasPermission(CartsConfig.adminPermissionLevel()))
                .then(Commands.literal("setcost")
                        .then(Commands.argument("count", IntegerArgumentType.integer(0, 1_000_000_000))
                                .executes(CartsCommand::setCost)))
                .then(Commands.literal("open")
                        .then(Commands.argument("user", GameProfileArgument.gameProfile())
                                .then(Commands.argument("location", StringArgumentType.greedyString()).suggests(STATIONS)
                                        .executes(ctx -> setAccess(ctx, true)))))
                .then(Commands.literal("close")
                        .then(Commands.argument("user", GameProfileArgument.gameProfile())
                                .then(Commands.argument("location", StringArgumentType.greedyString()).suggests(STATIONS)
                                        .executes(ctx -> setAccess(ctx, false)))))
                .then(Commands.literal("list").executes(CartsCommand::list))
                .then(Commands.literal("remove")
                        .then(Commands.argument("location", StringArgumentType.greedyString()).suggests(STATIONS)
                                .executes(CartsCommand::remove))));
    }

    private static int setCost(CommandContext<CommandSourceStack> ctx) {
        int count = IntegerArgumentType.getInteger(ctx, "count");
        CartsConfig.setCopperPer100Blocks(count);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.encorecarts.setcost", CoinText.format(count)), true);
        return count;
    }

    private static int setAccess(CommandContext<CommandSourceStack> ctx, boolean open) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "user");
        Station station = station(ctx);
        MinecraftServer server = ctx.getSource().getServer();
        for (GameProfile profile : profiles) {
            StationManager.setAccess(server, profile.getId(), station, open);
            ServerPlayer online = server.getPlayerList().getPlayer(profile.getId());
            if (open && online != null) {
                online.sendSystemMessage(Component.translatable("message.encorecarts.station.opened_by_admin", station.name())
                        .withStyle(ChatFormatting.GOLD));
            }
        }
        String key = open ? "command.encorecarts.open" : "command.encorecarts.close";
        String users = String.join(", ", profiles.stream().map(GameProfile::getName).toList());
        ctx.getSource().sendSuccess(() -> Component.translatable(key, station.name(), users), true);
        return profiles.size();
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        Collection<Station> stations = StationData.get(ctx.getSource().getServer()).stations();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.encorecarts.list.header", stations.size(),
                CoinText.format(CartsConfig.copperPer100Blocks())), false);
        for (Station station : stations) {
            BlockPos pos = BlockPos.containing(station.anchor());
            Component mode = Component.translatable(station.global()
                    ? "message.encorecarts.place.mode.global" : "message.encorecarts.place.mode.hidden");
            ctx.getSource().sendSuccess(() -> Component.translatable("command.encorecarts.list.entry", station.name(),
                    station.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ(), mode), false);
        }
        return stations.size();
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Station station = station(ctx);
        StationManager.removeStation(ctx.getSource().getServer(), station.id());
        ctx.getSource().sendSuccess(() -> Component.translatable("message.encorecarts.station.removed", station.name()), true);
        return 1;
    }

    private static Station station(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "location");
        Station station = StationData.get(ctx.getSource().getServer()).byName(name);
        if (station == null) {
            throw UNKNOWN_STATION.create(name);
        }
        return station;
    }
}
