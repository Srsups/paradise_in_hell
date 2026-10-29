package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class OpeningPersistenceTest {
    @TempDir Path directory;
    private static final class Store implements ProfileStore {
        CampaignProfile saved=new CampaignProfile();int writes;boolean fail;
        public CampaignProfile load(){return saved.copy();}
        public void save(CampaignProfile value)throws IOException{writes++;if(fail)throw new IOException("locked");saved=value.copy();}
    }
    private static String json(JsonValue value){return value.toJson(JsonWriter.OutputType.json);}
    private static JsonValue profile(CampaignProfile value){return new JsonReader().parse(value.toJson());}
    private static void replace(JsonValue value,String key,String encoded){value.remove(key);value.addChild(key,new JsonReader().parse(encoded));}
    private static CampaignProfile character(){CampaignProfile profile=new CampaignProfile();profile.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM);return profile;}
    private static String otherFacts(CampaignProfile profile){JsonValue value=profile(profile);value.remove("cinematics");return json(value);}

    @Test void newProfilesAreUnseenAndExplicitEmptyArrayPreventsFutureLegacyInference() {
        CampaignProfile profile=character();assertEquals(CampaignProfile.CinematicState.UNSEEN,profile.openingState());assertTrue(profile.openingPending());
        JsonValue encoded=profile(profile);assertTrue(encoded.has("cinematics"));assertEquals(0,encoded.require("cinematics").size);
        profile.recordReturn(RunOutcome.ABANDONED,0);
        assertEquals(CampaignProfile.CinematicState.UNSEEN,CampaignProfile.fromJson(profile.toJson()).openingState());
        encoded.remove("cinematics");assertEquals(CampaignProfile.CinematicState.UNSEEN,CampaignProfile.fromJson(json(encoded)).openingState());
    }

    @Test void onlyMissingCinematicArrayAndExistingAttemptsMigrateToLegacyWithoutClaimingViewedOrGrantingRewards() {
        CampaignProfile old=character();old.recordReturn(RunOutcome.DEATH,7);JsonValue legacy=profile(old);legacy.remove("cinematics");
        CampaignProfile migrated=CampaignProfile.fromJson(json(legacy));assertEquals(CampaignProfile.CinematicState.LEGACY,migrated.openingState());
        assertFalse(migrated.openingPending());assertTrue(migrated.openingSeen());assertEquals(otherFacts(old),otherFacts(migrated));
        assertEquals(CampaignProfile.CinematicState.LEGACY,CampaignProfile.fromJson(migrated.toJson()).openingState());
        assertFalse(migrated.has(CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ));assertFalse(migrated.has(CampaignProfile.Event.INSURGENCE_SPARK_OBTAINED));
    }

    @Test void completedAndSkippedArePersonalIdempotentFactsWithNoCurrencyTutorialOrNarrativeSideEffects() {
        for(boolean skipped:new boolean[]{false,true}) {
            Store store=new Store();store.saved=character();ProfileService service=new ProfileService(store);String before=otherFacts(service.profile());
            assertTrue(service.markOpening(service.profile().characterId(),skipped));
            assertEquals(skipped?CampaignProfile.CinematicState.SKIPPED:CampaignProfile.CinematicState.COMPLETED,service.profile().openingState());
            assertEquals(before,otherFacts(service.profile()));assertEquals(1,store.writes);
            assertTrue(service.markOpening(service.profile().characterId(),!skipped));assertEquals(1,store.writes);
            assertEquals(service.profile().openingState(),new ProfileService(store).profile().openingState());
        }
    }

    @Test void identityAndConfirmedProfessionAreRequiredBeforeRecording() {
        Store store=new Store();ProfileService service=new ProfileService(store);
        assertFalse(service.markOpening(service.profile().characterId(),true));assertEquals(0,store.writes);
        service.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM,false);int writes=store.writes;
        assertFalse(service.markOpening("another-character",false));assertFalse(service.markOpening(null,false));
        assertEquals(writes,store.writes);assertTrue(service.profile().openingPending());
    }

    @Test void autosaveFailureRetainsAcknowledgementInMemoryForRetryWithoutDuplicatingAReward() {
        Store store=new Store();store.saved=character();ProfileService service=new ProfileService(store);String before=otherFacts(service.profile());
        store.fail=true;assertFalse(service.markOpening(service.profile().characterId(),true));
        assertEquals(CampaignProfile.CinematicState.SKIPPED,service.profile().openingState());assertTrue(store.saved.openingPending());
        assertFalse(service.error().isEmpty());assertEquals(before,otherFacts(service.profile()));int writes=store.writes;
        assertTrue(service.markOpening(service.profile().characterId(),true));assertEquals(writes,store.writes);
        store.fail=false;assertTrue(service.retrySave());assertEquals(CampaignProfile.CinematicState.SKIPPED,store.saved.openingState());
        assertEquals(before,otherFacts(service.profile()));assertEquals("",service.error());
    }

    @Test void savesAndResetKeepCinematicsIndependentOfSharedProgressAndOtherCharacters() throws Exception {
        SaveSlots slots=SaveSlots.memory();ProfileService first=new ProfileService(slots.store(0)),second=new ProfileService(slots.store(1));
        first.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM,false);second.createCharacter(Profession.FERREIRO,GameDifficulty.HARD,false);
        String shared=slots.sharedStore().load().toJson(),oldId=first.profile().characterId();
        first.markOpening(oldId,false);assertTrue(second.profile().openingPending());assertEquals(shared,slots.sharedStore().load().toJson());
        assertTrue(first.reset());assertTrue(first.profile().openingPending());assertFalse(first.markOpening(oldId,true));
        assertTrue(first.createCharacter(Profession.GUERREIRO,GameDifficulty.EASY,false));assertNotEquals(oldId,first.profile().characterId());
        assertTrue(first.profile().openingPending());assertTrue(new ProfileService(slots.store(1)).profile().openingPending());
    }

    @Test void malformedCinematicRecordsAreRejectedInsteadOfBeingCoercedOrDuplicated() {
        for(String invalid:List.of("null","{}","42","[null]","[\"opening-v1\"]",
            "[{\"id\":\"opening-v1\"}]","[{\"id\":\"opening-v1\",\"state\":42}]",
            "[{\"id\":\"opening-v1\",\"state\":\"UNKNOWN\"}]","[{\"id\":\" \",\"state\":\"SKIPPED\"}]",
            "[{\"id\":\"opening-v1\",\"state\":\"SKIPPED\",\"reward\":1}]",
            "[{\"id\":\"opening-v1\",\"state\":\"SKIPPED\"},{\"id\":\"opening-v1\",\"state\":\"COMPLETED\"}]",
            "[{\"id\":\"opening-v1\",\"id\":\"other\"}]")) {
            JsonValue root=profile(character());replace(root,"cinematics",invalid);
            assertThrows(RuntimeException.class,()->CampaignProfile.fromJson(json(root)),invalid);
        }
    }

    @Test void unrelatedCinematicIdsAndLegacyStateRemainStableWhenAnOpeningIsRecorded() {
        JsonValue root=profile(character());replace(root,"cinematics","[{\"id\":\"other-film-v2\",\"state\":\"COMPLETED\"}]");
        CampaignProfile value=CampaignProfile.fromJson(json(root));assertTrue(value.openingPending());value.markOpening(true);
        CampaignProfile loaded=CampaignProfile.fromJson(value.toJson());
        assertEquals(CampaignProfile.CinematicState.COMPLETED,loaded.cinematicState("other-film-v2"));
        assertEquals(CampaignProfile.CinematicState.SKIPPED,loaded.openingState());
        replace(root,"cinematics","[{\"id\":\"opening-v1\",\"state\":\"LEGACY\"}]");
        Store store=new Store();store.saved=CampaignProfile.fromJson(json(root));ProfileService service=new ProfileService(store);
        assertTrue(service.markOpening(service.profile().characterId(),false));assertEquals(0,store.writes);
        assertEquals(CampaignProfile.CinematicState.LEGACY,service.profile().openingState());
    }

    @Test void fileRoundTripKeepsOpeningFactsAndMalformedRecordsPreserveTheOriginalFile() throws Exception {
        Path path=directory.resolve("profile.json");FileProfileStore store=new FileProfileStore(path);CampaignProfile value=character();value.markOpening(false);store.save(value);
        assertEquals(CampaignProfile.CinematicState.COMPLETED,new ProfileService(store).profile().openingState());
        JsonValue malformed=profile(value);replace(malformed,"cinematics","null");String encoded=json(malformed);Files.writeString(path,encoded);
        ProfileService invalid=new ProfileService(store);assertFalse(invalid.error().isEmpty());
        assertFalse(invalid.retrySave());assertEquals(encoded,Files.readString(path));
    }
}
