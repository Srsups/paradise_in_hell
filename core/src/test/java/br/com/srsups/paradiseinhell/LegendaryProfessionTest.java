package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LegendaryProfessionTest {
    @TempDir Path directory;
    @Test void unlockDebitsExactlyOnceAndSurvivesReloadWithoutChangingProfession() {
        SaveSlots slots=SaveSlots.memory(); Main game=new Main(slots);
        game.chooseProfession(Profession.CACADOR); game.profiles().profile().credit(7);
        game.shared().progress().credit(1007);
        assertTrue(game.unlockNecromancer()); assertEquals(7,game.shared().progress().souls());
        assertFalse(game.unlockNecromancer()); assertEquals(7,game.shared().progress().souls());
        assertEquals(Profession.CACADOR,game.profiles().profile().profession()); assertEquals(7,game.profiles().profile().obols());
        Main loaded=new Main(slots);
        assertTrue(loaded.necromancerUnlocked()); assertEquals(7,loaded.shared().progress().souls());
    }
    @Test void unaffordableOrFailedPurchaseDoesNotLoseMoneyOrUnlock() {
        SharedProgressService poor=new SharedProgressService(SharedProgressStore.memory()); poor.progress().credit(999);
        assertFalse(poor.unlockNecromancer()); assertEquals(999,poor.progress().souls());
        SharedProgress rich=new SharedProgress(); rich.credit(1000);
        SharedProgressService failing=new SharedProgressService(new SharedProgressStore() {
            public SharedProgress load() { return rich.copy(); }
            public void save(SharedProgress p) throws IOException { throw new IOException("test"); }
        });
        assertFalse(failing.unlockNecromancer()); assertEquals(1000,failing.progress().souls());
        assertFalse(failing.progress().necromancerUnlocked());
    }
    @Test void globalUnlockAllowsNewSaveButNeverReplacesAnExistingCharacter() {
        Main game=new Main(SaveSlots.memory());
        assertFalse(game.necromancerUnlocked()); assertFalse(game.chooseProfession(Profession.NECROMANTE));
        assertTrue(game.chooseProfession(Profession.FERREIRO)); game.profiles().profile().credit(10000);
        assertFalse(game.unlockNecromancer(),"Personal Obols cannot pay for a shared legendary unlock");
        game.shared().progress().credit(1000); assertTrue(game.unlockNecromancer());
        assertTrue(game.necromancerUnlocked()); assertFalse(game.chooseProfession(Profession.NECROMANTE));
        ProfileService fresh=game.profileAt(6);
        assertTrue(fresh.chooseProfession(Profession.NECROMANTE,game.necromancerUnlocked()));
        assertEquals(Profession.NECROMANTE,fresh.profile().profession());
        assertEquals(10000,game.profiles().profile().obols()); assertEquals(0,fresh.profile().obols());
    }
    @Test void allEightSlotsUseSeparateFilesAndLegacyFieldsDefaultToLocked() throws Exception {
        SaveSlots slots=SaveSlots.files(directory.resolve("profile.json"));
        SharedProgressService shared=new SharedProgressService(slots.sharedStore());
        shared.progress().credit(1000); assertTrue(shared.unlockNecromancer());
        for(int i=0;i<8;i++) {
            ProfileService service=new ProfileService(slots.store(i)); Profession profession=Profession.values()[i];
            assertTrue(service.chooseProfession(profession,shared.progress().necromancerUnlocked()));
            assertEquals(profession,new ProfileService(slots.store(i)).profile().profession());
        }
        assertTrue(java.nio.file.Files.exists(directory.resolve("profile-8.json")));
        assertTrue(java.nio.file.Files.exists(directory.resolve("shared-progress.json")));
        String legacy=new CampaignProfile().toJson().replace("\"necromancerUnlocked\": false,","");
        assertFalse(CampaignProfile.fromJson(legacy).necromancerUnlocked());
    }
}
