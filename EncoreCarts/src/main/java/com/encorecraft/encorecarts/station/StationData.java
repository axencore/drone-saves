package com.encorecraft.encorecarts.station;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-bound storage ({@code data/encorecarts_stations.dat}): stations and per-player access overrides.
 * An override wins over the station default: {@code true} = open (discovered or opened by an admin),
 * {@code false} = closed by an admin (rediscovering does not reopen it).
 */
public final class StationData extends SavedData {
    private static final String FILE = "encorecarts_stations";

    private final Map<UUID, Station> stations = new LinkedHashMap<>();
    private final Map<String, Station> byName = new HashMap<>();
    private final Map<UUID, Map<UUID, Boolean>> access = new HashMap<>();

    public static StationData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(StationData::new, StationData::load, null), FILE);
    }

    /** Marks dirty and writes the file right away so admin changes survive a crash. */
    public void saveNow(MinecraftServer server) {
        setDirty();
        server.overworld().getDataStorage().save();
    }

    public Collection<Station> stations() {
        return Collections.unmodifiableCollection(stations.values());
    }

    @Nullable
    public Station get(UUID id) {
        return stations.get(id);
    }

    @Nullable
    public Station byName(String name) {
        return byName.get(StationNames.key(name));
    }

    public boolean contains(UUID id) {
        return stations.containsKey(id);
    }

    void add(Station station) {
        stations.put(station.id(), station);
        byName.put(StationNames.key(station.name()), station);
        setDirty();
    }

    @Nullable
    Station remove(UUID id) {
        Station removed = stations.remove(id);
        if (removed != null) {
            byName.remove(StationNames.key(removed.name()));
            access.values().forEach(map -> map.remove(id));
            access.values().removeIf(Map::isEmpty);
            setDirty();
        }
        return removed;
    }

    public boolean isOpenFor(UUID player, Station station) {
        Boolean override = access.getOrDefault(player, Map.of()).get(station.id());
        return override != null ? override : station.global();
    }

    /** Opens a hidden station for a player who reached it, unless an admin closed it. @return true if newly opened */
    boolean discover(UUID player, Station station) {
        if (station.global()) {
            return false;
        }
        Map<UUID, Boolean> map = access.computeIfAbsent(player, k -> new HashMap<>());
        if (map.containsKey(station.id())) {
            return false;
        }
        map.put(station.id(), Boolean.TRUE);
        setDirty();
        return true;
    }

    void setAccess(UUID player, Station station, boolean open) {
        access.computeIfAbsent(player, k -> new HashMap<>()).put(station.id(), open);
        setDirty();
    }

    private static StationData load(CompoundTag tag, HolderLookup.Provider registries) {
        StationData data = new StationData();
        ListTag list = tag.getList("Stations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Station station = Station.load(list.getCompound(i));
            if (station != null) {
                data.stations.put(station.id(), station);
                data.byName.put(StationNames.key(station.name()), station);
            }
        }
        CompoundTag accessTag = tag.getCompound("Access");
        for (String playerKey : accessTag.getAllKeys()) {
            try {
                UUID player = UUID.fromString(playerKey);
                CompoundTag entries = accessTag.getCompound(playerKey);
                Map<UUID, Boolean> map = new HashMap<>();
                for (String stationKey : entries.getAllKeys()) {
                    UUID stationId = UUID.fromString(stationKey);
                    if (data.stations.containsKey(stationId)) {
                        map.put(stationId, entries.getBoolean(stationKey));
                    }
                }
                if (!map.isEmpty()) {
                    data.access.put(player, map);
                }
            } catch (IllegalArgumentException ignored) {
                // malformed UUID key
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        stations.values().forEach(station -> list.add(station.save()));
        tag.put("Stations", list);
        CompoundTag accessTag = new CompoundTag();
        access.forEach((player, map) -> {
            CompoundTag entries = new CompoundTag();
            map.forEach((stationId, open) -> entries.putBoolean(stationId.toString(), open));
            accessTag.put(player.toString(), entries);
        });
        tag.put("Access", accessTag);
        return tag;
    }
}
