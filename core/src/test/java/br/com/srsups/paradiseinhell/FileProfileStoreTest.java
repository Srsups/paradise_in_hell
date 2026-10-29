package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FileProfileStoreTest {
    @TempDir Path directory;
    @Test void profileRoundTripsOutsideInstallationIncludingCurrencyPurchasesAndEvents() throws Exception {
        Path file = directory.resolve("userdata/profile.json");
        FileProfileStore store = new FileProfileStore(file);
        ProfileService service = new ProfileService(store);
        service.chooseProfession(Profession.VIGILANTE);
        service.finishConversation(CampaignProfile.Event.CARONTE_MET);
        assertTrue(service.buy(PermanentAttribute.AGILITY));
        CampaignProfile loaded = new FileProfileStore(file).load();
        assertEquals(Profession.VIGILANTE, loaded.profession()); assertTrue(loaded.professionChosen());
        assertTrue(loaded.has(CampaignProfile.Event.CARONTE_MET)); assertEquals(1, loaded.bonuses().agility());
        assertEquals(0, loaded.obols()); assertTrue(Files.exists(file.resolveSibling("profile.json.bak")));
    }
    @Test void corruptedPrimaryRecoversBackupAndRepairDoesNotOverwriteGoodBackupWithCorruptData() throws Exception {
        Path file = directory.resolve("profile.json"); FileProfileStore store = new FileProfileStore(file);
        CampaignProfile original = new CampaignProfile(); original.credit(5); store.save(original);
        original.credit(7); store.save(original);
        Files.writeString(file, "broken");
        CampaignProfile recovered = store.load(); assertEquals(5, recovered.obols());
        recovered.credit(1); store.save(recovered);
        assertEquals(6, store.load().obols());
        assertEquals(5, CampaignProfile.fromJson(Files.readString(directory.resolve("profile.json.bak"))).obols());
        try (var files = Files.list(directory)) { assertEquals(2, files.count()); }
    }
    @Test void unreadableProfileIsPreservedAndGameplayStillHasASessionOnlyProfile() throws Exception {
        Path file = directory.resolve("profile.json"); Files.writeString(file, "broken");
        ProfileService service = new ProfileService(new FileProfileStore(file));
        assertFalse(service.error().isEmpty());
        assertTrue(service.chooseProfession(Profession.FERREIRO));
        service.finishConversation(CampaignProfile.Event.CARONTE_MET);
        assertFalse(service.buy(PermanentAttribute.VITALITY));
        assertFalse(service.retrySave()); assertEquals("broken", Files.readString(file));
        assertEquals(5, service.profile().obols()); assertEquals(Profession.FERREIRO, service.profile().profession());
    }
    @Test void failedPurchaseDoesNotSpendAndFailedReturnSaveKeepsEarningsForRetry() {
        class FailingStore implements ProfileStore {
            boolean failing = true;
            CampaignProfile saved = new CampaignProfile();
            public CampaignProfile load() { return saved.copy(); }
            public void save(CampaignProfile profile) throws IOException {
                if (failing) throw new IOException("Simulated full disk"); saved = profile.copy();
            }
        }
        FailingStore store = new FailingStore(); ProfileService service = new ProfileService(store);
        service.finishConversation(CampaignProfile.Event.CARONTE_MET);
        assertFalse(service.buy(PermanentAttribute.VITALITY));
        assertEquals(5, service.profile().obols()); assertEquals(0, service.profile().rank(PermanentAttribute.VITALITY));
        RunSession run = RunSession.campaign(CampaignProfileTest.region());
        for (int i = 0; i < 20; i++) run.recordKill(EnemyType.COMMON);
        run.end(); assertEquals(2, service.settle(run, RunOutcome.DEATH));
        assertEquals(7, service.profile().obols()); assertFalse(service.error().isEmpty());
        store.failing = false; assertTrue(service.retrySave());
        assertEquals(7, new ProfileService(store).profile().obols());
        assertEquals(0, service.settle(run, RunOutcome.DEATH));
    }
    @Test void aNewerProfileIsNotDowngradedEvenWhenAnOlderBackupExists() throws Exception {
        Path file = directory.resolve("profile.json"); FileProfileStore store = new FileProfileStore(file);
        store.save(new CampaignProfile()); store.save(new CampaignProfile());
        String future = new CampaignProfile().toJson().replace("\"version\": 2", "\"version\": 3");
        Files.writeString(file, future);
        ProfileService service = new ProfileService(store);
        assertFalse(service.error().isEmpty()); assertFalse(service.retrySave());
        assertEquals(future, Files.readString(file));
    }
}
