package com.encorecraft.encorecarts.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Personal client preferences ({@code config/encorecarts-client.toml}). */
public final class CartsClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue FORCED_SHOT;
    public static final ModConfigSpec.BooleanValue LETTERBOX;
    public static final ModConfigSpec.BooleanValue CUTSCENE_SOUNDS;
    public static final ModConfigSpec.BooleanValue MAP_ICONS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("cutscene");
        FORCED_SHOT = b.comment("Camera shot for every trip: -1 = random (chosen by the server), 0..9 = always this shot.",
                        "0 side, 1 inside the cart, 2 road behind, 3 head-on, 4 roadside pass, 5 crane, 6 orbit, 7 wheels, 8 coachman's shoulder, 9 departure")
                .defineInRange("forcedShot", -1, -1, 9);
        LETTERBOX = b.comment("Cinematic black bars during the cart shot.").define("letterbox", true);
        CUTSCENE_SOUNDS = b.comment("Hoof and wheel sounds during the cart shot.").define("sounds", true);
        b.pop();
        b.push("map");
        MAP_ICONS = b.comment("Show open stations on Xaero's World Map (if installed).").define("stationIcons", true);
        b.pop();
        SPEC = b.build();
    }

    private CartsClientConfig() {
    }
}
