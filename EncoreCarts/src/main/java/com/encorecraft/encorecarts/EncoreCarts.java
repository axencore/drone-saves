package com.encorecraft.encorecarts;

import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.network.ModNetwork;
import com.encorecraft.encorecarts.registry.ModEntities;
import com.encorecraft.encorecarts.registry.ModItems;
import com.encorecraft.encorecarts.server.ServerEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Common entry point. Everything registered here is safe on a dedicated server;
 * client-only wiring lives in {@code com.encorecraft.encorecarts.client.EncoreCartsClient}.
 */
@Mod(EncoreCarts.MOD_ID)
public final class EncoreCarts {
    public static final String MOD_ID = "encorecarts";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EncoreCarts(IEventBus modBus, ModContainer container) {
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        modBus.addListener(ModEntities::onCreateAttributes);
        modBus.addListener(ModItems::onBuildCreativeTabs);
        modBus.addListener(ModNetwork::register);

        container.registerConfig(ModConfig.Type.COMMON, CartsConfig.SPEC);

        ServerEvents.register(NeoForge.EVENT_BUS);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
