package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HudSettingsTest {
    @TempDir Path directory;
    private static final class Store implements HudSettingsStore {
        HudSettings saved=HudSettings.DEFAULT;
        boolean failLoad,failSave;
        int writes;
        public HudSettings load() throws IOException {
            if(failLoad)throw new IOException("locked");
            return saved;
        }
        public void save(HudSettings value) throws IOException {
            writes++;
            if(failSave)throw new IOException("locked");
            saved=value;
        }
    }
    @Test void everySupportedIntegerPercentRoundTripsAndHasTheExpectedScale() {
        assertEquals(new HudSettings(100),HudSettings.DEFAULT);
        assertEquals(1f,HudSettings.DEFAULT.scale());
        for(int percent=HudSettings.MIN_PERCENT;percent<=HudSettings.MAX_PERCENT;percent++) {
            HudSettings value=HudSettings.DEFAULT.withPercent(percent);
            assertEquals(percent,value.percent());
            assertEquals(percent/100f,value.scale(),.00001f);
            assertEquals(value,HudSettings.fromJson(value.toJson()));
        }
        assertThrows(IllegalArgumentException.class,()->new HudSettings(79));
        assertThrows(IllegalArgumentException.class,()->new HudSettings(126));
        assertThrows(IllegalArgumentException.class,()->HudSettings.DEFAULT.withPercent(Integer.MAX_VALUE));
        assertEquals(100,HudSettings.DEFAULT.percent());
    }
    @Test void schemaRejectsMalformedMissingUnknownDuplicateAndNonIntegerValues() {
        for(String json:new String[]{"null","[]","{}","{\"version\":1}",
            "{\"version\":1,\"percent\":100,\"other\":true}",
            "{\"version\":1,\"version\":1,\"percent\":100}",
            "{\"version\":1,\"percent\":100,\"percent\":110}",
            "{\"version\":\"1\",\"percent\":100}",
            "{\"version\":1.0,\"percent\":100}",
            "{\"version\":1,\"percent\":100.0}",
            "{\"version\":1,\"percent\":\"100\"}",
            "{\"version\":1,\"percent\":true}",
            "{\"version\":1,\"percent\":null}",
            "{\"version\":1,\"percent\":79}",
            "{\"version\":1,\"percent\":126}",
            "{\"version\":1,\"percent\":4294967396}"})
            assertThrows(RuntimeException.class,()->HudSettings.fromJson(json),json);
    }
    @Test void futureVersionIsDetectedBeforeRequiringThisVersionsFields() {
        for(String json:new String[]{"{\"version\":2,\"futureScale\":1.5}",
            "{\"version\":4294967296,\"futureField\":true}"})
            assertThrows(HudSettings.UnsupportedVersion.class,()->HudSettings.fromJson(json));
    }
    @Test void memoryStoresRetainTheirOwnConfirmedPreference() throws Exception {
        HudSettingsStore first=HudSettingsStore.memory(),second=HudSettingsStore.memory();
        first.save(new HudSettings(125));
        assertEquals(new HudSettings(125),first.load());
        assertEquals(HudSettings.DEFAULT,second.load());
        assertThrows(NullPointerException.class,()->first.save(null));
    }
    @Test void confirmedChangesWriteOnceAndReloadWithoutTouchingUnchangedValues() {
        Store store=new Store();HudSettingsService service=new HudSettingsService(store);
        assertTrue(service.apply(HudSettings.DEFAULT));assertEquals(0,store.writes);
        assertTrue(service.apply(new HudSettings(120)));assertEquals(1,store.writes);
        assertTrue(service.apply(new HudSettings(120)));assertEquals(1,store.writes);
        assertEquals(new HudSettings(120),new HudSettingsService(store).value());
        assertFalse(service.pendingSave());assertTrue(service.writable());assertEquals("",service.error());
    }
    @Test void failedWriteKeepsTheSelectedSizeInMemoryAndRetriesTheSamePendingValue() {
        Store store=new Store();HudSettingsService service=new HudSettingsService(store);store.failSave=true;
        HudSettings next=new HudSettings(125);
        assertFalse(service.apply(next));assertEquals(next,service.value());
        assertEquals(HudSettings.DEFAULT,store.saved);assertTrue(service.pendingSave());
        assertFalse(service.error().isEmpty());
        store.failSave=false;
        assertTrue(service.apply(next));assertEquals(next,store.saved);assertEquals(2,store.writes);
        assertTrue(service.retrySave());assertEquals(2,store.writes);assertFalse(service.pendingSave());assertEquals("",service.error());
    }
    @Test void unreadablePreferencesUseDefaultsAndAllowOnlySessionChangesWithoutOverwriting() {
        Store store=new Store();store.failLoad=true;HudSettingsService service=new HudSettingsService(store);
        assertEquals(HudSettings.DEFAULT,service.value());assertFalse(service.writable());
        assertFalse(service.apply(new HudSettings(80)));assertEquals(new HudSettings(80),service.value());
        assertTrue(service.pendingSave());assertFalse(service.retrySave());assertEquals(0,store.writes);
        assertFalse(service.error().isEmpty());
    }
    @Test void missingFileUsesDefaultAndAtomicWritesKeepOneValidatedBackupWithoutTemporaryFiles() throws Exception {
        Path file=directory.resolve("prefs").resolve("hud-settings.json");FileHudSettingsStore store=new FileHudSettingsStore(file);
        assertEquals(HudSettings.DEFAULT,store.load());assertFalse(Files.exists(file));
        store.save(HudSettings.DEFAULT);store.save(new HudSettings(125));
        assertEquals(new HudSettings(125),store.load());
        assertEquals(HudSettings.DEFAULT,HudSettings.fromJson(Files.readString(file.resolveSibling("hud-settings.json.bak"))));
        try(var entries=Files.list(file.getParent())){assertEquals(2,entries.count());}
    }
    @Test void damagedPrimaryRecoversBackupAndDoesNotReplaceThatBackupWithDamagedData() throws Exception {
        Path file=directory.resolve("hud-settings.json"),backup=directory.resolve("hud-settings.json.bak");
        FileHudSettingsStore store=new FileHudSettingsStore(file);
        store.save(new HudSettings(90));store.save(new HudSettings(125));Files.writeString(file,"broken");
        assertEquals(new HudSettings(90),store.load());
        store.save(new HudSettings(110));assertEquals(new HudSettings(110),store.load());
        assertEquals(new HudSettings(90),HudSettings.fromJson(Files.readString(backup)));
        try(var entries=Files.list(directory)){assertEquals(2,entries.count());}
    }
    @Test void futurePrimaryPreservesItsBytesAndBackupWithoutFallingBack() throws Exception {
        Path file=directory.resolve("hud-settings.json"),backup=directory.resolve("hud-settings.json.bak");
        FileHudSettingsStore store=new FileHudSettingsStore(file);
        store.save(HudSettings.DEFAULT);store.save(new HudSettings(125));
        String beforeBackup=Files.readString(backup),future="{\"version\":2,\"futureScale\":1.5}";
        Files.writeString(file,future);
        assertThrows(IOException.class,store::load);
        assertThrows(IOException.class,()->store.save(HudSettings.DEFAULT));
        HudSettingsService service=new HudSettingsService(store);
        assertFalse(service.apply(new HudSettings(80)));assertFalse(service.writable());
        assertEquals(future,Files.readString(file));assertEquals(beforeBackup,Files.readString(backup));
    }
    @Test void futureBackupRemainsProtectedEvenWhenPrimaryIsValid() throws Exception {
        Path file=directory.resolve("hud-settings.json"),backup=directory.resolve("hud-settings.json.bak");
        FileHudSettingsStore store=new FileHudSettingsStore(file);store.save(new HudSettings(90));
        String future="{\"version\":2,\"futureScale\":1.5}";Files.writeString(backup,future);
        assertEquals(new HudSettings(90),store.load());
        assertThrows(IOException.class,()->store.save(new HudSettings(110)));
        assertEquals(new HudSettings(90),HudSettings.fromJson(Files.readString(file)));assertEquals(future,Files.readString(backup));
    }
    @Test void globalHudPreferenceSurvivesCreationAndDeletionOfCharactersInOtherStores() {
        HudSettingsStore preferences=HudSettingsStore.memory();HudSettingsService service=new HudSettingsService(preferences);
        SaveSlots slots=SaveSlots.memory();
        ProfileService first=new ProfileService(slots.store(0)),second=new ProfileService(slots.store(7));
        assertTrue(first.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM,false));
        assertTrue(second.createCharacter(Profession.GUERREIRO,GameDifficulty.HARD,false));
        assertTrue(service.apply(new HudSettings(125)));
        assertTrue(first.reset());
        assertFalse(first.profile().professionChosen());assertTrue(second.profile().professionChosen());
        assertEquals(new HudSettings(125),new HudSettingsService(preferences).value());
        assertTrue(second.reset());assertEquals(new HudSettings(125),service.value());
    }
}
