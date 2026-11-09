package com.encorecraft.encorecarts.client.compat.xaero;

import com.encorecraft.encorecarts.EncoreCarts;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Optional Xaero's World Map support. This class has no Xaero imports; Xaero types are only touched inside
 * {@link XaeroWorldMapIntegration}, which is loaded only when the mod is present.
 */
public final class XaeroCompat {
    private static final String MOD_ID = "xaeroworldmap";
    private static boolean finished;

    private XaeroCompat() {
    }

    public static void init() {
        if (ModList.get().isLoaded(MOD_ID)) {
            NeoForge.EVENT_BUS.addListener(XaeroCompat::onClientTick);
        }
    }

    // Xaero creates its element handler during its own client setup; register as soon as it exists.
    private static void onClientTick(ClientTickEvent.Post event) {
        if (finished) {
            return;
        }
        try {
            if (XaeroWorldMapIntegration.tryRegister()) {
                finished = true;
                EncoreCarts.LOGGER.info("Xaero's World Map integration enabled: station icons on the world map");
            }
        } catch (Throwable t) {
            finished = true;
            EncoreCarts.LOGGER.warn("Xaero's World Map integration disabled (unsupported Xaero version)", t);
        }
    }
}
