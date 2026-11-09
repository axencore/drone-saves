package com.encorecraft.encorecarts.client.compat.xaero;

import xaero.map.WorldMap;
import xaero.map.element.MapElementRenderHandler;

/** Touches Xaero classes; only loaded when Xaero's World Map is installed. */
final class XaeroWorldMapIntegration {
    private XaeroWorldMapIntegration() {
    }

    static boolean tryRegister() {
        MapElementRenderHandler handler = WorldMap.mapElementRenderHandler;
        if (handler == null) {
            return false;
        }
        handler.add(StationMapElementRenderer.create());
        return true;
    }
}
