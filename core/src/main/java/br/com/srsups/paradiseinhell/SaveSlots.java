package br.com.srsups.paradiseinhell;

import java.nio.file.Path;

/** Four independent profiles. Slot one preserves the original profile.json. */
public final class SaveSlots {
    public static final int COUNT = 4;
    private final ProfileStore[] stores;
    public SaveSlots(ProfileStore... stores) {
        if (stores.length != COUNT) throw new IllegalArgumentException("Expected four saves");
        this.stores = stores.clone();
        for (ProfileStore store : this.stores) java.util.Objects.requireNonNull(store);
    }
    public ProfileStore store(int index) {
        if (index < 0 || index >= COUNT) throw new IllegalArgumentException("Invalid slot");
        return stores[index];
    }
    public static SaveSlots files(Path original) {
        return new SaveSlots(new FileProfileStore(original),
            new FileProfileStore(original.resolveSibling("profile-2.json")), new FileProfileStore(original.resolveSibling("profile-3.json")),
            new FileProfileStore(original.resolveSibling("profile-4.json")));
    }
    public static SaveSlots memory() { return new SaveSlots(ProfileStore.memory(), ProfileStore.memory(), ProfileStore.memory(), ProfileStore.memory()); }
}
