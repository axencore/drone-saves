package net.mcreator.encorecraftnew.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * Compile-time stand-in for EncoreCraftNew 2.x's public economy API (wallet amounts are in bronze/copper).
 * Only the members EncoreCarts calls are declared; the real class is used at runtime.
 */
public final class EncoreCraftEconomyApi {
    private EncoreCraftEconomyApi() {
    }

    public static boolean isEnabled() {
        throw new AssertionError("compile-time stub");
    }

    public static long getWallet(ServerPlayer player) {
        throw new AssertionError("compile-time stub");
    }

    public static boolean trySpendWallet(ServerPlayer player, long amountBronze) {
        throw new AssertionError("compile-time stub");
    }

    public static boolean giveWallet(ServerPlayer player, long amountBronze) {
        throw new AssertionError("compile-time stub");
    }
}
