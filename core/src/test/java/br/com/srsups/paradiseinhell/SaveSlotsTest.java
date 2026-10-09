package br.com.srsups.paradiseinhell;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SaveSlotsTest {
    @TempDir Path directory;
    @Test void professionIsLockedIncludingAfterReloadAndOtherSlotsHaveIndependentProgress() {
        SaveSlots saves = SaveSlots.memory();
        ProfileService first = new ProfileService(saves.store(0));
        assertTrue(first.chooseProfession(Profession.CACADOR));
        first.finishConversation(CampaignProfile.Event.CARONTE_MET); first.buy(PermanentAttribute.VITALITY);
        assertFalse(first.chooseProfession(Profession.VIGILANTE));
        assertThrows(IllegalStateException.class, () -> first.profile().chooseProfession(Profession.FERREIRO));
        ProfileService loaded = new ProfileService(saves.store(0));
        assertFalse(loaded.chooseProfession(Profession.FERREIRO));
        assertEquals(Profession.CACADOR, loaded.profile().profession());
        ProfileService second = new ProfileService(saves.store(1));
        assertTrue(second.chooseProfession(Profession.VIGILANTE));
        assertEquals(0, second.profile().obols()); assertEquals(0, second.profile().rank(PermanentAttribute.VITALITY));
        assertFalse(second.profile().has(CampaignProfile.Event.CARONTE_MET));
        assertEquals(1, new ProfileService(saves.store(0)).profile().rank(PermanentAttribute.VITALITY));
    }
    @Test void oldSingleSaveStaysInSlotOneAndCreatingAnotherNeverReplacesItsFiles() throws Exception {
        Path original = directory.resolve("profile.json");
        FileProfileStore oldStore = new FileProfileStore(original);
        CampaignProfile old = new CampaignProfile(); old.chooseProfession(Profession.FERREIRO); old.credit(9); oldStore.save(old);
        String before = Files.readString(original);
        SaveSlots slots = SaveSlots.files(original);
        assertEquals(Profession.FERREIRO, slots.store(0).load().profession());
        ProfileService fresh = new ProfileService(slots.store(1)); fresh.chooseProfession(Profession.VIGILANTE);
        assertEquals(before, Files.readString(original)); assertTrue(Files.exists(directory.resolve("profile-2.json")));
        assertEquals(9, slots.store(0).load().obols()); assertEquals(0, slots.store(1).load().obols());
    }
    @Test void fourthSavePersistsTheLuckyProfessionWithoutChangingTheFirstThree() throws Exception {
        SaveSlots slots = SaveSlots.files(directory.resolve("profile.json"));
        Profession[] previous = {Profession.CACADOR, Profession.FERREIRO, Profession.VIGILANTE};
        String[] before = new String[3];
        for (int i = 0; i < 3; i++) {
            new ProfileService(slots.store(i)).chooseProfession(previous[i]);
            before[i] = slots.store(i).load().toJson();
        }
        ProfileService fourth = new ProfileService(slots.store(3)); assertTrue(fourth.chooseProfession(Profession.APOSTADOR));
        ProfileService loaded = new ProfileService(slots.store(3));
        assertEquals(Profession.APOSTADOR, loaded.profile().profession());
        assertFalse(loaded.chooseProfession(Profession.CACADOR));
        assertTrue(Files.exists(directory.resolve("profile-4.json")));
        for (int i = 0; i < 3; i++) assertEquals(before[i], slots.store(i).load().toJson());
    }
}
