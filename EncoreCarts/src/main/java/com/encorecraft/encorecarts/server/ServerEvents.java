package com.encorecraft.encorecarts.server;

import com.encorecraft.encorecarts.command.CartsCommand;
import com.encorecraft.encorecarts.economy.stub.TestWalletCommands;
import com.encorecraft.encorecarts.station.PlacementRequests;
import com.encorecraft.encorecarts.station.StationManager;
import com.encorecraft.encorecarts.travel.TravelManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Game-bus listeners that run on the logical server (dedicated or integrated). */
public final class ServerEvents {
    private ServerEvents() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ServerEvents::onRegisterCommands);
        // Last, so a real EncoreCraft registered earlier in the same event is detected and never shadowed.
        bus.addListener(EventPriority.LOWEST, ServerEvents::onRegisterCommandsLast);
        bus.addListener(ServerEvents::onServerTick);
        bus.addListener(ServerEvents::onLogin);
        bus.addListener(ServerEvents::onLogout);
        bus.addListener(ServerEvents::onChangedDimension);
        bus.addListener(ServerEvents::onIncomingDamage);
        bus.addListener(ServerEvents::onDeath);
        bus.addListener(ServerEvents::onEntityInteract);
        bus.addListener(ServerEvents::onEntityInteractSpecific);
        bus.addListener(ServerEvents::onRightClickBlock);
        bus.addListener(ServerEvents::onRightClickItem);
        bus.addListener(ServerEvents::onLeftClickBlock);
        bus.addListener(ServerEvents::onAttack);
        bus.addListener(ServerEvents::onBreak);
        bus.addListener(ServerEvents::onServerStopped);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        CartsCommand.register(event.getDispatcher());
    }

    private static void onRegisterCommandsLast(RegisterCommandsEvent event) {
        TestWalletCommands.registerIfNeeded(event.getDispatcher());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        TravelManager.tick(event.getServer());
        StationManager.tick(event.getServer());
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StationManager.syncStations(player);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TravelManager.onLogout(player);
            PlacementRequests.forget(player.getUUID());
        }
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TravelManager.cancel(player);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && TravelManager.isProtected(player)) {
            event.setCanceled(true);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TravelManager.cancel(player);
        }
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        blockIfTraveling(event.getEntity(), event);
    }

    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        blockIfTraveling(event.getEntity(), event);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        blockIfTraveling(event.getEntity(), event);
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        blockIfTraveling(event.getEntity(), event);
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        blockIfTraveling(event.getEntity(), event);
    }

    private static void onAttack(AttackEntityEvent event) {
        blockIfTraveling(event.getEntity(), event);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        blockIfTraveling(event.getPlayer(), event);
    }

    private static void blockIfTraveling(Object entity, ICancellableEvent event) {
        if (entity instanceof ServerPlayer player && TravelManager.isInCutscene(player)) {
            event.setCanceled(true);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TravelManager.reset();
        PlacementRequests.reset();
    }
}
