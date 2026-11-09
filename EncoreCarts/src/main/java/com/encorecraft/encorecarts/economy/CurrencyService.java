package com.encorecraft.encorecarts.economy;

import java.util.OptionalLong;
import net.minecraft.server.level.ServerPlayer;

/**
 * The only gateway between EncoreCarts and a wallet implementation. Travel logic talks to
 * {@link Economy#service()} and never to a concrete backend. Called on the server thread only.
 */
public interface CurrencyService {
    /** Short identifier for logs. */
    String id();

    /** Total spendable value in copper, or empty if the wallet cannot be read right now. */
    OptionalLong balance(ServerPlayer player);

    /** Removes {@code copper} from the wallet, atomically refusing when there is not enough. */
    PaymentResult withdraw(ServerPlayer player, long copper);

    /** Adds {@code copper} to the wallet (refunds). */
    boolean deposit(ServerPlayer player, long copper);

    /** True when travel costs nothing because no currency is configured at all. */
    default boolean isFree() {
        return false;
    }
}
