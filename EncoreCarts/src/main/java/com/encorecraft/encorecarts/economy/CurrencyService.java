package com.encorecraft.encorecarts.economy;

import java.util.OptionalLong;
import net.minecraft.server.level.ServerPlayer;

/**
 * The only gateway between EncoreCarts and a wallet implementation. Everything else in the mod talks
 * to {@link Economy#service()} so that the command based backend can later be replaced by direct
 * EncoreCraft Java API calls without touching travel logic. Called on the server thread only.
 */
public interface CurrencyService {
    /** Short identifier for logs. */
    String id();

    /** Total wallet value in copper, or empty if the wallet cannot be read right now. */
    OptionalLong balance(ServerPlayer player);

    /** Removes {@code copper} worth of coins, breaking larger coins into change when needed. */
    PaymentResult withdraw(ServerPlayer player, long copper);

    /** Adds {@code copper} worth of coins (used for refunds). */
    boolean deposit(ServerPlayer player, long copper);
}
