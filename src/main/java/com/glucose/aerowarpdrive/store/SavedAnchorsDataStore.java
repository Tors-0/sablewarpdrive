package com.glucose.aerowarpdrive.store;

import com.glucose.aerowarpdrive.core.WarpAnchor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SavedAnchorsDataStore extends SavedData {
    private static final String nbtKey = "aerowarpdrive_anchors";
    private final Map<UUID, WarpAnchor> warpAnchors = new ConcurrentHashMap<>();

    public static SavedAnchorsDataStore create() {
        return new SavedAnchorsDataStore();
    }

    public void addAnchor(WarpAnchor anchor) {
        warpAnchors.put(anchor.getUId(), anchor);
        setDirty();
    }

    public void updateAnchor(WarpAnchor anchor) {
        warpAnchors.put(anchor.getUId(), anchor);
        setDirty();
    }

    public void removeAnchor(WarpAnchor anchor) {
        warpAnchors.remove(anchor.getUId());
        setDirty();
    }

    public Collection<WarpAnchor> getAnchors() {
        return warpAnchors.values();
    }

    public Optional<WarpAnchor> getAnchorByUid(UUID uuid) {
        return Optional.ofNullable(warpAnchors.get(uuid));
    }

    public static SavedAnchorsDataStore load(CompoundTag compoundTag, HolderLookup.Provider provider) {
        SavedAnchorsDataStore data = SavedAnchorsDataStore.create();
        ListTag listTag = compoundTag.getList(nbtKey, Tag.TAG_COMPOUND);
        for (Tag tag : listTag) {
            CompoundTag compound = (CompoundTag) tag;
            WarpAnchor anchor = WarpAnchor.read(compound);
            if (anchor != null) data.warpAnchors.put(anchor.getUId(), anchor);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        ListTag listTag = new ListTag();
        for (WarpAnchor anchor : warpAnchors.values()) {
            listTag.add(WarpAnchor.write(anchor, new CompoundTag()));
        }
        compoundTag.put(nbtKey, listTag);
        return compoundTag;
    }

    public static SavedAnchorsDataStore getDimensionAnchorStore(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(SavedAnchorsDataStore::create, SavedAnchorsDataStore::load), "warp_anchors");
    }
}
