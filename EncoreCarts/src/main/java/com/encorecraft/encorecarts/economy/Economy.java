package com.encorecraft.encorecarts.economy;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.config.CartsConfig;
import com.encorecraft.encorecarts.economy.stub.TestWallet;
import java.util.Objects;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Holder of the active {@link CurrencyService}, chosen when the server starts. */
public final class Economy {
    public static final String ENCORECRAFT_MOD_ID = "encorecraftnew";

    private static final CurrencyService FREE = new CurrencyService() {
        @Override
        public String id() {
            return "free";
        }

        @Override
        public OptionalLong balance(ServerPlayer player) {
            return OptionalLong.empty();
        }

        @Override
        public PaymentResult withdraw(ServerPlayer player, long copper) {
            return PaymentResult.SUCCESS;
        }

        @Override
        public boolean deposit(ServerPlayer player, long copper) {
            return true;
        }

        @Override
        public boolean isFree() {
            return true;
        }
    };

    @Nullable
    private static CurrencyService override;
    private static CurrencyService service = FREE;

    private Economy() {
    }

    public static CurrencyService service() {
        return service;
    }

    public static boolean isEncoreCraftInstalled() {
        return ModList.get().isLoaded(ENCORECRAFT_MOD_ID);
    }

    /** Picks the backend: EncoreCraft API, otherwise the test wallet, otherwise free travel. */
    public static void select() {
        if (override != null) {
            service = override;
        } else if (isEncoreCraftInstalled()) {
            service = createEncoreCraftWallet();
        } else if (CartsConfig.TEST_WALLET.get()) {
            service = new TestWallet();
        } else {
            service = FREE;
        }
        EncoreCarts.LOGGER.info("EncoreCarts currency backend: {}", service.id());
    }

    // Separate method so the EncoreCraft class is only resolved when the mod is present.
    private static CurrencyService createEncoreCraftWallet() {
        return new com.encorecraft.encorecarts.economy.encorecraft.EncoreCraftWallet();
    }

    /** Lets another mod or a future integration force a specific backend. */
    public static void setOverride(@Nullable CurrencyService newService) {
        override = newService;
        if (newService != null) {
            service = Objects.requireNonNull(newService);
        }
    }
}
