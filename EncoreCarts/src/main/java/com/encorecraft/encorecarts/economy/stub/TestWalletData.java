package com.encorecraft.encorecarts.economy.stub;

import com.encorecraft.encorecarts.economy.CoinPurse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Storage of the TEST wallet stand-in. Not used when the real EncoreCraft mod is installed. */
public final class TestWalletData extends SavedData {
    private static final String FILE = "encorecarts_test_wallets";
    private final Map<UUID, CoinPurse> wallets = new HashMap<>();

    public static TestWalletData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TestWalletData::new, TestWalletData::load, null), FILE);
    }

    public CoinPurse wallet(UUID player) {
        return wallets.getOrDefault(player, CoinPurse.EMPTY);
    }

    public void setWallet(UUID player, CoinPurse purse) {
        wallets.put(player, purse);
        setDirty();
    }

    private static TestWalletData load(CompoundTag tag, HolderLookup.Provider registries) {
        TestWalletData data = new TestWalletData();
        CompoundTag players = tag.getCompound("Wallets");
        for (String key : players.getAllKeys()) {
            CompoundTag w = players.getCompound(key);
            try {
                data.wallets.put(UUID.fromString(key), new CoinPurse(
                        Math.max(0, w.getLong("c")), Math.max(0, w.getLong("i")),
                        Math.max(0, w.getLong("g")), Math.max(0, w.getLong("p"))));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag players = new CompoundTag();
        wallets.forEach((id, purse) -> {
            CompoundTag w = new CompoundTag();
            w.putLong("c", purse.copper());
            w.putLong("i", purse.silver());
            w.putLong("g", purse.gold());
            w.putLong("p", purse.platinum());
            players.put(id.toString(), w);
        });
        tag.put("Wallets", players);
        return tag;
    }
}
