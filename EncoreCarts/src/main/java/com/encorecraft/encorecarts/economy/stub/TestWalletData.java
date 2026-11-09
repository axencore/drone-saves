package com.encorecraft.encorecarts.economy.stub;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Storage of the TEST wallet ({@code data/encorecarts_test_wallets.dat}); unused when EncoreCraft is installed. */
public final class TestWalletData extends SavedData {
    private static final String FILE = "encorecarts_test_wallets";
    private final Map<UUID, Long> wallets = new HashMap<>();

    public static TestWalletData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TestWalletData::new, TestWalletData::load, null), FILE);
    }

    public long wallet(UUID player) {
        return wallets.getOrDefault(player, 0L);
    }

    public void setWallet(UUID player, long copper) {
        wallets.put(player, Math.max(0L, copper));
        setDirty();
    }

    private static TestWalletData load(CompoundTag tag, HolderLookup.Provider registries) {
        TestWalletData data = new TestWalletData();
        CompoundTag players = tag.getCompound("Wallets");
        for (String key : players.getAllKeys()) {
            try {
                data.wallets.put(UUID.fromString(key), Math.max(0L, players.getLong(key)));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag players = new CompoundTag();
        wallets.forEach((id, copper) -> players.putLong(id.toString(), copper));
        tag.put("Wallets", players);
        return tag;
    }
}
