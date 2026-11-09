package com.encorecraft.encorecarts.station;

import com.encorecraft.encorecarts.network.payload.ConfirmPlacementPayload;
import com.encorecraft.encorecarts.network.payload.OpenPlacementScreenPayload;
import com.encorecraft.encorecarts.network.payload.PlacementResultPayload;
import com.encorecraft.encorecarts.registry.ModItems;
import com.encorecraft.encorecarts.travel.TravelManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Two-step placement: using the cart item makes the server compute and validate the layout and remember it;
 * the station is created only when the admin confirms a valid unique name for that exact request.
 */
public final class PlacementRequests {
    private static final long TTL_TICKS = 20L * 60 * 5;
    private static final double MAX_CONFIRM_DISTANCE = 24.0;

    private record Pending(int id, ResourceKey<Level> dimension, StationLayout layout, long expiresAt) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static int nextId = 1;

    private PlacementRequests() {
    }

    public static void begin(ServerPlayer player, BlockPos clicked) {
        if (!StationManager.isAdmin(player)) {
            player.displayClientMessage(Component.translatable("message.encorecarts.place.no_permission").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!player.isCreative()) {
            player.displayClientMessage(Component.translatable("message.encorecarts.place.creative_only").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (TravelManager.isTraveling(player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        VoxelShape shape = level.getBlockState(clicked).getCollisionShape(level, clicked);
        double top = clicked.getY() + (shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y));
        StationLayout layout = new StationLayout(new Vec3(clicked.getX() + 0.5, top, clicked.getZ() + 0.5),
                StationLayout.snapYaw(player.getYRot()));
        Optional<Component> problem = layout.validate(level);
        if (problem.isPresent()) {
            player.sendSystemMessage(problem.get().copy().withStyle(ChatFormatting.RED));
            return;
        }
        Pending pending = new Pending(nextId++, level.dimension(), layout, level.getGameTime() + TTL_TICKS);
        PENDING.put(player.getUUID(), pending);
        PacketDistributor.sendToPlayer(player, new OpenPlacementScreenPayload(pending.id()));
    }

    public static void confirm(ServerPlayer player, ConfirmPlacementPayload message) {
        Pending pending = PENDING.get(player.getUUID());
        ServerLevel level = player.serverLevel();
        if (pending == null || pending.id() != message.requestId()) {
            reply(player, message.requestId(), false, Component.translatable("message.encorecarts.place.expired"));
            return;
        }
        if (!StationManager.isAdmin(player) || !player.isCreative()) {
            PENDING.remove(player.getUUID());
            reply(player, message.requestId(), false, Component.translatable("message.encorecarts.place.no_permission"));
            return;
        }
        if (level.dimension() != pending.dimension() || level.getGameTime() > pending.expiresAt()
                || player.position().distanceTo(pending.layout().anchor()) > MAX_CONFIRM_DISTANCE
                || !(player.getMainHandItem().is(ModItems.CART.get()) || player.getOffhandItem().is(ModItems.CART.get()))) {
            PENDING.remove(player.getUUID());
            reply(player, message.requestId(), false, Component.translatable("message.encorecarts.place.expired"));
            return;
        }
        String name = StationNames.normalize(message.name());
        Optional<Component> nameProblem = StationNames.validate(name);
        if (nameProblem.isPresent()) {
            reply(player, message.requestId(), false, nameProblem.get());
            return;
        }
        if (StationData.get(player.server).byName(name) != null) {
            reply(player, message.requestId(), false, Component.translatable("message.encorecarts.name.taken", name));
            return;
        }
        Optional<Component> problem = pending.layout().validate(level);
        if (problem.isPresent()) {
            PENDING.remove(player.getUUID());
            reply(player, message.requestId(), false, problem.get());
            return;
        }
        PENDING.remove(player.getUUID());
        StationManager.create(level, pending.layout(), name, message.global());
        Component mode = Component.translatable(message.global()
                ? "message.encorecarts.place.mode.global" : "message.encorecarts.place.mode.hidden");
        Component created = Component.translatable("message.encorecarts.place.created", name, mode);
        player.sendSystemMessage(created.copy().withStyle(ChatFormatting.GREEN));
        reply(player, message.requestId(), true, created);
    }

    private static void reply(ServerPlayer player, int requestId, boolean success, Component message) {
        PacketDistributor.sendToPlayer(player, new PlacementResultPayload(requestId, success, message));
    }

    public static void forget(UUID player) {
        PENDING.remove(player);
    }

    public static void reset() {
        PENDING.clear();
    }
}
