package com.encorecraft.encorecarts.travel;

import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.economy.CoinText;
import com.encorecraft.encorecarts.economy.Economy;
import com.encorecraft.encorecarts.economy.PaymentResult;
import com.encorecraft.encorecarts.network.payload.TravelMenuPayload;
import com.encorecraft.encorecarts.station.Station;
import com.encorecraft.encorecarts.station.StationData;
import com.encorecraft.encorecarts.station.StationManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Coachman menu and travel requests. Every rule (access, dimension, distance, price, payment) is enforced here. */
public final class TravelService {
    private TravelService() {
    }

    public static void openMenu(ServerPlayer player, UUID originId, @Nullable Component notice) {
        if (TravelManager.isTraveling(player) || player.isSpectator()) {
            return;
        }
        StationData data = StationData.get(player.server);
        Station origin = data.get(originId);
        if (origin == null || !isAtStation(player, origin)) {
            return;
        }
        StationManager.discover(player, origin);

        long cost = CartsConfig.copperPer100Blocks();
        OptionalLong balance = Economy.service().balance(player);
        List<TravelMenuPayload.Destination> destinations = new ArrayList<>();
        for (Station station : data.stations()) {
            if (station == origin || station.dimension() != origin.dimension() || !data.isOpenFor(player.getUUID(), station)) {
                continue;
            }
            double distance = origin.distanceTo(station);
            long price = PriceCalculator.price(distance, cost);
            boolean affordable = balance.isPresent() && balance.getAsLong() >= price;
            destinations.add(new TravelMenuPayload.Destination(station.id(), station.name(),
                    (int) Math.round(distance), price, affordable));
        }
        destinations.sort(Comparator.comparingInt(TravelMenuPayload.Destination::distance));
        PacketDistributor.sendToPlayer(player, new TravelMenuPayload(origin.id(), origin.name(), balance.isPresent(),
                balance.orElse(0), cost, destinations, Optional.ofNullable(notice)));
    }

    public static void requestTravel(ServerPlayer player, UUID originId, UUID destinationId) {
        if (TravelManager.isTraveling(player) || player.isSpectator() || !player.isAlive()) {
            return;
        }
        StationData data = StationData.get(player.server);
        Station origin = data.get(originId);
        Station destination = data.get(destinationId);
        if (origin == null || !isAtStation(player, origin)) {
            return;
        }
        if (destination == null || destination == origin || !data.isOpenFor(player.getUUID(), destination)) {
            openMenu(player, originId, error("message.encorecarts.travel.unknown_destination"));
            return;
        }
        if (destination.dimension() != origin.dimension()) {
            openMenu(player, originId, error("message.encorecarts.travel.other_dimension"));
            return;
        }
        if (player.isPassenger()) {
            openMenu(player, originId, error("message.encorecarts.travel.dismount"));
            return;
        }
        if (!player.onGround()) {
            openMenu(player, originId, error("message.encorecarts.travel.on_ground"));
            return;
        }
        long price = PriceCalculator.price(origin.distanceTo(destination), CartsConfig.copperPer100Blocks());
        PaymentResult result = Economy.service().withdraw(player, price);
        switch (result) {
            case SUCCESS -> {
                if (price > 0) {
                    player.sendSystemMessage(Component.translatable("message.encorecarts.travel.paid", CoinText.format(price),
                            destination.name()).withStyle(ChatFormatting.GRAY));
                }
                TravelManager.start(player, origin, destination, price);
            }
            case INSUFFICIENT_FUNDS -> openMenu(player, originId,
                    error("message.encorecarts.travel.not_enough", CoinText.format(price)));
            case UNAVAILABLE -> openMenu(player, originId, error("message.encorecarts.travel.wallet_unavailable"));
        }
    }

    static boolean isAtStation(ServerPlayer player, Station station) {
        return player.level().dimension() == station.dimension()
                && player.position().distanceTo(station.anchor()) <= CartsConfig.INTERACTION_DISTANCE.get();
    }

    private static Component error(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.RED);
    }
}
