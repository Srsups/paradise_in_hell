package br.com.srsups.paradiseinhell;

import java.nio.file.Path;

/** Eight independent profiles. Existing slots keep their original paths. */
public final class SaveSlots {
    public static final int COUNT = 8;
    private final ProfileStore[] stores;
    private final SharedProgressStore shared;
    public SaveSlots(ProfileStore... supplied) { this(SharedProgressStore.memory(), supplied); }
    public SaveSlots(SharedProgressStore shared, ProfileStore... supplied) {
        this.shared=java.util.Objects.requireNonNull(shared);
        if (supplied.length != COUNT && supplied.length != 4 && supplied.length != 1)
            throw new IllegalArgumentException("Expected one, four or eight save stores");
        stores = new ProfileStore[COUNT];
        for (int i = 0; i < COUNT; i++) stores[i] = i < supplied.length
            ? java.util.Objects.requireNonNull(supplied[i]) : ProfileStore.memory();
    }
    public SharedProgressStore sharedStore() { return shared; }
    public ProfileStore store(int index) {
        if (index < 0 || index >= COUNT) throw new IllegalArgumentException("Invalid slot");
        return stores[index];
    }
    public static SaveSlots files(Path original) {
        ProfileStore[] stores = new ProfileStore[COUNT];
        for (int i = 0; i < COUNT; i++) stores[i] = new FileProfileStore(i == 0 ? original
            : original.resolveSibling("profile-" + (i + 1) + ".json"));
        return new SaveSlots(new FileSharedProgressStore(original.resolveSibling("shared-progress.json")),stores);
    }
    public static SaveSlots memory() { return new SaveSlots(ProfileStore.memory()); }
}
