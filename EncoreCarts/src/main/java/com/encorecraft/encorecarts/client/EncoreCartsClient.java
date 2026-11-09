package com.encorecraft.encorecarts.client;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.client.compat.xaero.XaeroCompat;
import com.encorecraft.encorecarts.client.cutscene.CutsceneController;
import com.encorecraft.encorecarts.client.model.CartsModels;
import com.encorecraft.encorecarts.client.render.CartRenderer;
import com.encorecraft.encorecarts.client.render.CoachmanRenderer;
import com.encorecraft.encorecarts.network.ModNetwork;
import com.encorecraft.encorecarts.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only entry point; never constructed on a dedicated server. */
@Mod(value = EncoreCarts.MOD_ID, dist = Dist.CLIENT)
public final class EncoreCartsClient {
    public EncoreCartsClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, CartsClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(EncoreCartsClient::registerRenderers);
        modBus.addListener(EncoreCartsClient::registerGuiLayers);
        modBus.addListener(EncoreCartsClient::registerReloadListeners);

        ModNetwork.setClientHandler(new ClientPayloadHandlers());
        CutsceneController.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(EncoreCartsClient::onLoggingOut);
        XaeroCompat.init();
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CART.get(), CartRenderer::new);
        event.registerEntityRenderer(ModEntities.COACHMAN.get(), CoachmanRenderer::new);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(EncoreCarts.id("travel_cutscene"), CutsceneController::renderOverlay);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(CartsModels.INSTANCE);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStations.clear();
        CutsceneController.abort();
    }
}
