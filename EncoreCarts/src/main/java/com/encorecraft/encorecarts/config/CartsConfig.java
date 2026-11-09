package com.encorecraft.encorecarts.config;

import com.encorecraft.encorecarts.economy.Coin;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    public static final ModConfigSpec.IntValue CUTSCENE_TIMEOUT_TICKS;

    public static final ModConfigSpec.BooleanValue STUB_WALLET_COMMANDS;
    public static final ModConfigSpec.ConfigValue<String> ENCORECRAFT_MOD_IDS;
    public static final ModConfigSpec.ConfigValue<String> CMD_ADD;
    public static final ModConfigSpec.ConfigValue<String> CMD_DEL;
    public static final ModConfigSpec.ConfigValue<String> CMD_INFO;
    private static final Map<Coin, ModConfigSpec.ConfigValue<String>> COIN_ALIASES = new EnumMap<>(Coin.class);

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
        CUTSCENE_TIMEOUT_TICKS = b
                .comment("The server teleports the traveller after this many ticks even if the client never confirms the end of the cutscene.")
                .defineInRange("cutsceneTimeoutTicks", 200, 80, 1200);
        b.pop();

        b.comment("Currency (EncoreCraft wallet). All wallet access goes through these server commands until the EncoreCraft Java API is wired in.")
                .push("economy");
        STUB_WALLET_COMMANDS = b
                .comment("Register a TEST stand-in for addCoinPlayer/delCoinPlayer/setCoinPlayer/infoCoinPlayer when EncoreCraft is not installed.",
                        "Balances of the stand-in are stored in the world (data/encorecarts_test_wallets.dat).")
                .define("stubWalletCommands", true);
        ENCORECRAFT_MOD_IDS = b
                .comment("Comma separated mod ids that identify an installed EncoreCraft (the stand-in is disabled when one of them is loaded).")
                .define("encoreCraftModIds", "encorecraftnew,encorecraft");
        CMD_ADD = b.comment("Command adding coins. Placeholders: {player} {type} {amount}")
                .define("addCommand", "addCoinPlayer {player} {type} {amount}");
        CMD_DEL = b.comment("Command removing coins. Placeholders: {player} {type} {amount}")
                .define("delCommand", "delCoinPlayer {player} {type} {amount}");
        CMD_INFO = b.comment("Command printing the wallet. Placeholder: {player}")
                .define("infoCommand", "infoCoinPlayer {player}");
        b.comment("Words used to find each coin amount in the output of the info command (case-insensitive, prefix match).",
                "Both '<word> 12' / '<word>: 12' / '<word>=12' and '12 <word>' forms are recognised.").push("infoAliases");
        COIN_ALIASES.put(Coin.COPPER, b.define("copper", "c, медн, медь, бронз, copper, bronze"));
        COIN_ALIASES.put(Coin.SILVER, b.define("silver", "i, серебр, silver"));
        COIN_ALIASES.put(Coin.GOLD, b.define("gold", "g, золот, gold"));
        COIN_ALIASES.put(Coin.PLATINUM, b.define("platinum", "p, платин, platinum"));
        b.pop();
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

    public static List<String> encoreCraftModIds() {
        return splitList(ENCORECRAFT_MOD_IDS.get());
    }

    public static Map<Coin, List<String>> coinAliases() {
        Map<Coin, List<String>> result = new EnumMap<>(Coin.class);
        COIN_ALIASES.forEach((coin, value) -> result.put(coin, splitList(value.get())));
        return result;
    }

    private static List<String> splitList(String raw) {
        List<String> list = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }
}
