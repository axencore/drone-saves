package com.encorecraft.encorecarts.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side gameplay settings ({@code config/encorecarts-common.toml}).
 * Values changed by {@code /carts} commands are written back to this file immediately.
 */
public final class CartsConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue COPPER_PER_100_BLOCKS;
    public static final ModConfigSpec.IntValue ADMIN_PERMISSION_LEVEL;
    public static final ModConfigSpec.DoubleValue INTERACTION_DISTANCE;
    public static final ModConfigSpec.IntValue MAX_TRIP_SECONDS;
    public static final ModConfigSpec.BooleanValue TEST_WALLET;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Travel price").push("price");
        COPPER_PER_100_BLOCKS = b
                .comment("Price in copper coins for every started 100 blocks of distance. Changed by /carts setcost <count>.")
                .defineInRange("copperPer100Blocks", 15, 0, 1_000_000_000);
        b.pop();

        b.comment("Permissions and travel rules").push("travel");
        ADMIN_PERMISSION_LEVEL = b
                .comment("Operator permission level required to place/remove carts and to use /carts.")
                .defineInRange("adminPermissionLevel", 2, 0, 4);
        INTERACTION_DISTANCE = b
                .comment("Maximum distance (blocks) between a player and the station anchor to use the coachman.")
                .defineInRange("interactionDistance", 8.0, 2.0, 32.0);
        MAX_TRIP_SECONDS = b
                .comment("The traveller is moved to the destination as soon as the cutscene starts; the cutscene lasts until",
                        "their client has loaded the destination. If the client never reports that (crash, modified client),",
                        "the server ends the trip after this many seconds anyway.")
                .defineInRange("maxTripSeconds", 90, 20, 600);
        b.pop();

        b.comment("Currency. With EncoreCraftNew (mod id encorecraftnew) installed, the personal wallet of its Java API",
                "(EncoreCraftEconomyApi) is used and trips are free while EncoreCraft's economy is switched off.").push("economy");
        TEST_WALLET = b
                .comment("Without EncoreCraftNew: true = TEST wallet stored in the world (data/encorecarts_test_wallets.dat)",
                        "with stand-ins of addCoinPlayer/delCoinPlayer/setCoinPlayer/infoCoinPlayer; false = trips are free.")
                .define("testWallet", true);
        b.pop();

        SPEC = b.build();
    }

    private CartsConfig() {
    }

    public static long copperPer100Blocks() {
        return COPPER_PER_100_BLOCKS.get();
    }

    /** Persists a new price to the config file so it survives restarts. */
    public static void setCopperPer100Blocks(int value) {
        COPPER_PER_100_BLOCKS.set(value);
        SPEC.save();
    }

    public static int adminPermissionLevel() {
        return ADMIN_PERMISSION_LEVEL.get();
    }
}
