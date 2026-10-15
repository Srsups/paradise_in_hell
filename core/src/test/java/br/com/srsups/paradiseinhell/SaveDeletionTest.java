package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SaveDeletionTest {
    @TempDir Path directory;
    @Test void resetClearsAllPersonalProgressAndChangesTheCharacterIdentity() throws Exception {
        SaveSlots slots=SaveSlots.files(directory.resolve("profile.json"));
        ProfileService profile=new ProfileService(slots.store(6));
        assertTrue(profile.chooseProfession(Profession.SACERDOTE));
        CampaignProfile old=profile.profile();
        old.finishConversation(CampaignProfile.Event.CARONTE_MET); old.credit(40);
        assertTrue(old.buy(PermanentAttribute.STRENGTH));
        old.claimForgeMaterial("submundo"); old.forgeWeapon(); old.recordReturn(RunOutcome.DEATH,3);
        assertTrue(profile.retrySave()); String id=old.characterId();
        assertTrue(Files.exists(directory.resolve("profile-7.json.bak")));
        assertTrue(profile.reset());
        CampaignProfile empty=slots.store(6).load();
        assertFalse(empty.professionChosen()); assertEquals(0,empty.obols()); assertEquals(0,empty.attempts());
        assertEquals(0,empty.forgeMaterials()); assertEquals(WeaponRarity.COMMON,empty.weaponRarity());
        assertEquals(0,empty.rank(PermanentAttribute.STRENGTH)); assertFalse(empty.has(CampaignProfile.Event.CARONTE_MET));
        assertTrue(empty.canClaimForgeMaterial("submundo")); assertNotEquals(id,empty.characterId());
        assertFalse(Files.exists(directory.resolve("profile-7.json.bak")));
        ProfileService recreated=new ProfileService(slots.store(6)); assertTrue(recreated.chooseProfession(Profession.GUERREIRO));
        // Backup recovery after reuse must restore an empty/new character, never the deleted Priest.
        Files.writeString(directory.resolve("profile-7.json"),"invalid");
        CampaignProfile recovered=slots.store(6).load();
        assertFalse(recovered.professionChosen()); assertEquals(empty.characterId(),recovered.characterId());
    }
    @Test void resetLeavesOtherSlotsSharedSoulsAndLegendaryAccessUntouched() throws Exception {
        SaveSlots slots=SaveSlots.memory(); Main game=new Main(slots);
        assertTrue(game.profileAt(0).chooseProfession(Profession.NECROMANTE,true));
        assertTrue(game.profileAt(1).chooseProfession(Profession.CACADOR));
        game.shared().progress().credit(77); game.shared().retrySave();
        String other=game.profileAt(1).profile().toJson();
        assertTrue(game.deleteSave(0));
        assertFalse(game.profileAt(0).profile().professionChosen());
        assertEquals(other,game.profileAt(1).profile().toJson());
        Main loaded=new Main(slots);
        assertEquals(77,loaded.shared().progress().souls()); assertTrue(loaded.necromancerUnlocked());
        assertFalse(loaded.profileAt(0).profile().professionChosen());
    }
    @Test void failedResetPreservesTheCurrentCharacterInMemoryAndOnDisk() {
        ProfileStore memory=ProfileStore.memory(); CampaignProfile old=new CampaignProfile();
        old.chooseProfession(Profession.FERREIRO); old.credit(55);
        try {memory.save(old);} catch(IOException e){throw new AssertionError(e);}
        ProfileService service=new ProfileService(new ProfileStore(){
            public CampaignProfile load() throws IOException {return memory.load();}
            public void save(CampaignProfile p) throws IOException {memory.save(p);}
            public void reset(CampaignProfile p) throws IOException {throw new IOException("locked");}
        });
        assertFalse(service.reset()); assertEquals(old.toJson(),service.profile().toJson());
        assertFalse(service.error().isEmpty());
        assertEquals(55,new ProfileService(memory).profile().obols());
    }
    @Test void backupAccessFailureCannotResetTheMainFile() throws Exception {
        Path path=directory.resolve("profile.json"); FileProfileStore store=new FileProfileStore(path);
        CampaignProfile old=new CampaignProfile(); old.chooseProfession(Profession.VIGILANTE); store.save(old);
        Files.createDirectory(directory.resolve("profile.json.bak"));
        ProfileService service=new ProfileService(store);
        assertFalse(service.reset()); assertEquals(old.toJson(),Files.readString(path));
        assertEquals(old.characterId(),service.profile().characterId());
    }
    @Test void failedPrimaryCommitRestoresTheBackupAndFailedReadsCanBeExplicitlyReset() throws Exception {
        Path path=directory.resolve("profile.json"); Files.createDirectory(path);
        Files.writeString(path.resolve("keep.txt"),"force a failed replacement");
        CampaignProfile old=new CampaignProfile(); old.chooseProfession(Profession.APOSTADOR);
        Path backup=directory.resolve("profile.json.bak"); Files.writeString(backup,old.toJson());
        FileProfileStore store=new FileProfileStore(path);
        assertThrows(IOException.class,()->store.reset(new CampaignProfile()));
        assertEquals(old.toJson(),Files.readString(backup));
        assertEquals(Profession.APOSTADOR,store.load().profession());
        Path corrupt=directory.resolve("profile-2.json"); Files.writeString(corrupt,"broken");
        ProfileService broken=new ProfileService(new FileProfileStore(corrupt));
        assertFalse(broken.error().isEmpty()); assertTrue(broken.reset());
        assertTrue(broken.error().isEmpty()); assertFalse(new FileProfileStore(corrupt).load().professionChosen());
    }
    @Test void newCharacterCannotSeeTheDeletedCharactersReportsButGlobalHistoryAndCreditRemain() {
        SaveSlots slots=SaveSlots.memory(); Main game=new Main(slots);
        ProfileService service=game.profileAt(0); service.chooseProfession(Profession.CACADOR);
        RunSession run=RunSession.demo(Profession.CACADOR); service.prepareRun(run); run.end();
        RunReport old=game.shared().settle(run,RunOutcome.ABANDONED,0);
        assertEquals(old,game.latestReport()); assertTrue(game.deleteSave(0));
        assertNull(game.latestReport());
        game.chooseProfession(Profession.VIGILANTE);
        assertNull(game.latestReport());
        RunSession fresh=RunSession.demo(Profession.VIGILANTE); game.profiles().prepareRun(fresh); fresh.end();
        RunReport next=game.shared().settle(fresh,RunOutcome.ABANDONED,0);
        assertEquals(next,game.latestReport());
        assertEquals(java.util.List.of(next),game.shared().progress().history(0,next.characterId()));
        assertEquals(2,game.shared().progress().history().size());
        assertNotEquals(old.characterId(),next.characterId());
        Main loaded=new Main(slots); assertEquals(next,loaded.latestReport());
    }
    @Test void legacyProfilesAndReportsKeepTheirHistoryUntilTheSlotIsReset() {
        CampaignProfile legacy=CampaignProfile.fromJson(new CampaignProfile().toJson().replaceAll(
            "\\s*\\\"characterId\\\"\\s*:\\s*\\\"[^\\\"]*\\\"\\s*,",""));
        assertEquals("",legacy.characterId()); assertEquals("",legacy.copy().characterId());
        assertNotEquals("",new CampaignProfile().characterId());
        RunSession run=RunSession.demo(); run.end(); RunReport old=run.report(RunOutcome.ABANDONED,0);
        var json=new com.badlogic.gdx.utils.JsonReader().parse(old.toJson()); json.remove("characterId");
        assertEquals("",RunReport.fromJson(json).characterId());
        assertEquals(old,RunReport.fromJson(json));
    }
}

