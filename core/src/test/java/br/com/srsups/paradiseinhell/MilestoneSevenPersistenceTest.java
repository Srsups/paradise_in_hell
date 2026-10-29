package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MilestoneSevenPersistenceTest {
    @TempDir Path directory;
    private static final CampaignProfile.Event SPARK=CampaignProfile.Event.INSURGENCE_SPARK_OBTAINED;
    private static final CampaignProfile.Event READ=CampaignProfile.Event.UNDERWORLD_EPILOGUE_READ;

    private static final class Store implements ProfileStore {
        CampaignProfile saved=new CampaignProfile();
        boolean fail;
        int writes;
        @Override public CampaignProfile load(){return saved.copy();}
        @Override public void save(CampaignProfile profile) throws IOException {
            writes++;
            if(fail)throw new IOException("Simulated full disk");
            saved=profile.copy();
        }
    }
    private static ProfileService character(ProfileStore store) {
        ProfileService service=new ProfileService(store);
        assertTrue(service.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM,false));
        return service;
    }
    private static RegionDefinition region(String id) {
        return new RegionDefinition(id,"Region","fight",List.of(
            new ArenaDefinition("fight","Fight","",ArenaDefinition.Kind.COMBAT,0,Map.of(EnemyType.COMMON,1),List.of("boss")),
            new ArenaDefinition("boss","Boss","",ArenaDefinition.Kind.BOSS_ROOM,0,Map.of(),List.of())));
    }
    private static RunSession won(ProfileService service,String regionId) {
        RunSession run=RunSession.campaign(region(regionId),service.profile().profession(),service.profile().bonuses());
        service.prepareRun(run);run.recordKill(EnemyType.COMMON);run.enterArena("boss");run.recordBossDefeat();run.end();
        return run;
    }
    private static JsonValue json(CampaignProfile profile){return new JsonReader().parse(profile.toJson());}
    private static String text(JsonValue value){return value.toJson(JsonWriter.OutputType.json);}
    private static JsonValue legacy(CampaignProfile profile) {
        JsonValue value=json(profile);value.remove("narrativeVersion");value.remove("tutorialSeen");value.remove("tutorialEnabled");
        return value;
    }

    @Test void emptyCharactersHaveEnabledUnseenTutorialsAndNoNarrativeReward() {
        CampaignProfile profile=new CampaignProfile();
        assertTrue(profile.tutorialEnabled());assertFalse(profile.hasProgress());
        for(TutorialTopic topic:TutorialTopic.values())assertFalse(profile.tutorialSeen(topic));
        assertFalse(profile.has(SPARK));assertFalse(profile.has(READ));
        JsonValue saved=json(profile);assertEquals(2,saved.getInt("version"));assertEquals(1,saved.getInt("narrativeVersion"));
        assertTrue(saved.require("tutorialSeen").isArray());assertEquals(0,saved.require("tutorialSeen").size);
        assertEquals(profile.toJson(),profile.copy().toJson());
    }

    @Test void legacyVictoryMigratesOnlyItsNarrativeConquestAndKeepsAllExistingProgress() {
        CampaignProfile old=new CampaignProfile();old.createCharacter(Profession.GUERREIRO,GameDifficulty.HARD);
        old.credit(80);old.buy(PermanentAttribute.ARMOR);old.claimForgeMaterial(RegionDefinition.PROTOTYPE_ID);old.forgeWeapon();
        old.finishConversation(CampaignProfile.Event.BRIBE_JOKE);old.recordReturn(RunOutcome.VICTORY,11);
        CampaignProfile loaded=CampaignProfile.fromJson(text(legacy(old)));
        assertTrue(loaded.has(SPARK));assertFalse(loaded.has(READ));assertTrue(loaded.tutorialEnabled());
        for(TutorialTopic topic:TutorialTopic.values())assertFalse(loaded.tutorialSeen(topic));
        assertEquals(old.characterId(),loaded.characterId());assertEquals(old.profession(),loaded.profession());assertEquals(old.difficulty(),loaded.difficulty());
        assertEquals(old.obols(),loaded.obols());assertEquals(old.attempts(),loaded.attempts());assertEquals(old.victories(),loaded.victories());
        assertEquals(old.rank(PermanentAttribute.ARMOR),loaded.rank(PermanentAttribute.ARMOR));assertEquals(WeaponRarity.RARE,loaded.weaponRarity());
        assertFalse(loaded.canClaimForgeMaterial(RegionDefinition.PROTOTYPE_ID));assertTrue(loaded.has(CampaignProfile.Event.BRIBE_JOKE));
        assertEquals(loaded.toJson(),loaded.copy().toJson());assertEquals(1,json(loaded).getInt("narrativeVersion"));
    }

    @Test void legacyDeathDoesNotCreateConquestAndInvalidCountsAreStillRejected() {
        CampaignProfile dead=new CampaignProfile();dead.recordReturn(RunOutcome.DEATH,3);
        CampaignProfile loaded=CampaignProfile.fromJson(text(legacy(dead)));
        assertFalse(loaded.has(SPARK));assertFalse(loaded.has(READ));
        JsonValue invalid=legacy(dead);invalid.remove("victories");invalid.addChild("victories",new JsonValue(1L));
        assertThrows(IllegalArgumentException.class,()->CampaignProfile.fromJson(text(invalid)));
    }

    @Test void currentOtherRegionVictoryNeverMigratesIntoAnUnderworldConquestDuringCopyOrReload() {
        Store store=new Store();ProfileService service=character(store);
        RunSession run=won(service,"another-region");service.settle(run,RunOutcome.VICTORY);
        assertEquals(1,service.profile().victories());assertFalse(service.profile().has(SPARK));
        assertFalse(service.profile().copy().has(SPARK));assertFalse(store.saved.has(SPARK));
        CampaignProfile loaded=new ProfileService(store).profile();assertFalse(loaded.has(SPARK));assertFalse(loaded.has(READ));
        JsonValue withoutTutorialFields=json(loaded);withoutTutorialFields.remove("tutorialSeen");withoutTutorialFields.remove("tutorialEnabled");
        assertFalse(CampaignProfile.fromJson(text(withoutTutorialFields)).has(SPARK),"Tutorial defaults cannot act as a narrative migration marker");
    }

    @Test void tutorialJsonRejectsWrongTypesUnknownTopicsAndDuplicateEntries() {
        String original=new CampaignProfile().toJson();
        for(String invalid:new String[]{"null","false","{}","\"MOVE\"","[1]","[null]","[true]","[\"move\"]","[\"UNKNOWN\"]","[\"MOVE\",\"MOVE\"]"}) {
            JsonValue value=new JsonReader().parse(original);value.remove("tutorialSeen");value.addChild("tutorialSeen",new JsonReader().parse(invalid));
            assertThrows(IllegalArgumentException.class,()->CampaignProfile.fromJson(text(value)),invalid);
        }
        for(String invalid:new String[]{"null","0","1","\"false\"","[]","{}"}) {
            JsonValue value=new JsonReader().parse(original);value.remove("tutorialEnabled");value.addChild("tutorialEnabled",new JsonReader().parse(invalid));
            assertThrows(IllegalArgumentException.class,()->CampaignProfile.fromJson(text(value)),invalid);
        }
    }

    @Test void narrativeSchemaVersionRequiresExactlyTheSupportedInteger() {
        for(String invalid:new String[]{"null","false","1.0","\"1\"","-1","0","2","[]","{}"}) {
            JsonValue value=json(new CampaignProfile());value.remove("narrativeVersion");value.addChild("narrativeVersion",new JsonReader().parse(invalid));
            assertThrows(IllegalArgumentException.class,()->CampaignProfile.fromJson(text(value)),invalid);
        }
    }

    @Test void futureNarrativeSchemaCannotBeOverwrittenThroughAnOlderBackup() throws Exception {
        Path file=directory.resolve("profile.json");FileProfileStore store=new FileProfileStore(file);
        CampaignProfile old=new CampaignProfile();old.chooseProfession(Profession.CACADOR);store.save(old);store.save(old);
        JsonValue future=json(old);future.remove("narrativeVersion");future.addChild("narrativeVersion",new JsonValue(2L));String source=text(future);
        Files.writeString(file,source);ProfileService service=new ProfileService(store);
        assertFalse(service.error().isEmpty());assertFalse(service.retrySave());assertEquals(source,Files.readString(file));
    }

    @Test void emptyAndForeignCharactersCannotRecordTutorialsSettingsOrEpilogues() {
        Store store=new Store();ProfileService service=new ProfileService(store);String empty=service.profile().characterId();
        assertFalse(service.completeTutorial(empty,TutorialTopic.MOVE));assertFalse(service.setTutorialEnabled(empty,false));assertFalse(service.finishUnderworldEpilogue(empty));
        assertFalse(service.profile().hasProgress());assertEquals(0,store.writes);
        assertTrue(service.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM,false));String before=service.profile().toJson();int writes=store.writes;
        String foreign=java.util.UUID.randomUUID().toString();
        assertFalse(service.completeTutorial(foreign,TutorialTopic.MOVE));assertFalse(service.completeTutorial(null,TutorialTopic.MOVE));
        assertFalse(service.completeTutorial(service.profile().characterId(),null));assertFalse(service.setTutorialEnabled(foreign,false));
        assertFalse(service.finishUnderworldEpilogue(foreign));assertFalse(service.finishUnderworldEpilogue(service.profile().characterId()));
        assertEquals(before,service.profile().toJson());assertEquals(writes,store.writes);
    }

    @Test void completedTutorialsAndSettingsPersistAndIdempotentCallsDoNotWriteAgain() {
        Store store=new Store();ProfileService service=character(store);String id=service.profile().characterId();int baseline=store.writes;
        for(TutorialTopic topic:TutorialTopic.values()){assertTrue(service.completeTutorial(id,topic));assertTrue(service.completeTutorial(id,topic));}
        assertEquals(baseline+TutorialTopic.values().length,store.writes);
        assertTrue(service.setTutorialEnabled(id,false));assertTrue(service.setTutorialEnabled(id,false));
        assertEquals(baseline+TutorialTopic.values().length+1,store.writes);
        ProfileService loaded=new ProfileService(store);assertFalse(loaded.profile().tutorialEnabled());
        for(TutorialTopic topic:TutorialTopic.values())assertTrue(loaded.profile().tutorialSeen(topic));
        assertTrue(loaded.setTutorialEnabled(id,true));assertTrue(new ProfileService(store).profile().tutorialEnabled());
    }

    @Test void failedTutorialAutosavesKeepFactsInMemoryAndRetryPersistsWithoutDuplicateWrites() {
        Store store=new Store();ProfileService service=character(store);String id=service.profile().characterId();store.fail=true;
        assertFalse(service.completeTutorial(id,TutorialTopic.DASH));assertFalse(service.setTutorialEnabled(id,false));
        assertTrue(service.profile().tutorialSeen(TutorialTopic.DASH));assertFalse(service.profile().tutorialEnabled());
        assertFalse(store.saved.tutorialSeen(TutorialTopic.DASH));assertTrue(store.saved.tutorialEnabled());assertFalse(service.error().isEmpty());
        int writes=store.writes;assertTrue(service.completeTutorial(id,TutorialTopic.DASH));assertTrue(service.setTutorialEnabled(id,false));
        assertEquals(writes,store.writes,"Already recorded facts use explicit retrySave rather than rewriting every frame");
        store.fail=false;assertTrue(service.retrySave());CampaignProfile loaded=new ProfileService(store).profile();
        assertTrue(loaded.tutorialSeen(TutorialTopic.DASH));assertFalse(loaded.tutorialEnabled());assertTrue(service.error().isEmpty());
    }

    @Test void tutorialMenusDoNotAdvanceTheAttemptOrChangeItsEconomyAndBuild() {
        Store store=new Store();ProfileService service=character(store);CampaignProfile profile=service.profile();
        profile.credit(45);profile.buy(PermanentAttribute.ARMOR);profile.claimForgeMaterial(RegionDefinition.PROTOTYPE_ID);profile.forgeWeapon();service.retrySave();
        RunSession active=RunSession.campaign(region(RegionDefinition.PROTOTYPE_ID),profile.profession(),profile.bonuses());service.prepareRun(active);
        active.advance(13);active.applyUpgrade(Upgrade.FORCA);active.player().sofrerDano(7);
        String id=profile.characterId();int coins=profile.obols(),attempts=profile.attempts(),materials=profile.forgeMaterials();
        float elapsed=active.progress().elapsed(),life=active.player().getVida(),damage=active.player().getDano();
        assertTrue(service.completeTutorial(id,TutorialTopic.REFUGE));assertTrue(service.completeTutorial(id,TutorialTopic.BOONS));assertTrue(service.setTutorialEnabled(id,false));
        assertEquals(coins,service.profile().obols());assertEquals(attempts,service.profile().attempts());assertEquals(materials,service.profile().forgeMaterials());
        assertEquals(profile.rank(PermanentAttribute.ARMOR),service.profile().rank(PermanentAttribute.ARMOR));assertEquals(WeaponRarity.RARE,service.profile().weaponRarity());
        assertEquals(elapsed,active.progress().elapsed());assertEquals(life,active.player().getVida());assertEquals(damage,active.player().getDano());
        assertEquals(1,active.rank(Upgrade.FORCA));assertFalse(active.finished());assertFalse(service.profile().has(CampaignProfile.Event.CARONTE_MET));
        assertEquals(0,service.profile().deaths());assertEquals(0,service.profile().victories());assertFalse(service.profile().has(SPARK));
    }

    @Test void realUnderworldVictoryAwardsOneNarrativeSparkInTheExistingSettlementWrite() {
        Store store=new Store();ProfileService service=character(store);RunSession run=won(service,RegionDefinition.PROTOTYPE_ID);int baseline=store.writes;
        assertEquals(10,service.settle(run,RunOutcome.VICTORY));assertTrue(service.profile().has(SPARK));assertTrue(store.saved.has(SPARK));
        assertFalse(service.profile().has(READ));assertEquals(1,service.profile().victories());assertEquals(1,service.profile().attempts());
        assertEquals(baseline+1,store.writes);assertEquals(10,service.profile().obols());
        assertEquals(0,service.settle(run,RunOutcome.VICTORY));assertEquals(baseline+1,store.writes);
        RunSession again=won(service,RegionDefinition.PROTOTYPE_ID);assertEquals(10,service.settle(again,RunOutcome.VICTORY));
        assertEquals(20,service.profile().obols());assertEquals(2,service.profile().victories());assertTrue(service.profile().has(SPARK));
    }

    @Test void demoDeathAbandonmentAndInvalidVictoryCannotGrantTheSpark() {
        for(RunOutcome outcome:new RunOutcome[]{RunOutcome.DEATH,RunOutcome.ABANDONED}) {
            Store store=new Store();ProfileService service=character(store);
            RunSession run=RunSession.campaign(region(RegionDefinition.PROTOTYPE_ID));service.prepareRun(run);run.end();service.settle(run,outcome);
            assertFalse(service.profile().has(SPARK));assertFalse(new ProfileService(store).profile().has(SPARK));
        }
        Store store=new Store();ProfileService service=character(store);RunSession demo=RunSession.demo();service.prepareRun(demo);demo.end();
        assertEquals(0,service.settle(demo,RunOutcome.VICTORY));assertFalse(service.profile().has(SPARK));assertEquals(0,service.profile().attempts());
        RunSession notWon=RunSession.campaign(region(RegionDefinition.PROTOTYPE_ID));service.prepareRun(notWon);notWon.end();int writes=store.writes;
        assertThrows(IllegalStateException.class,()->service.settle(notWon,RunOutcome.VICTORY));assertFalse(service.profile().has(SPARK));assertEquals(writes,store.writes);
    }

    @Test void failedVictoryAutosaveKeepsSparkAndReturnTogetherForOneRetryWithoutRepaying() {
        Store store=new Store();ProfileService service=character(store);RunSession run=won(service,RegionDefinition.PROTOTYPE_ID);store.fail=true;
        assertEquals(10,service.settle(run,RunOutcome.VICTORY));assertTrue(service.profile().has(SPARK));assertEquals(10,service.profile().obols());
        assertEquals(1,service.profile().victories());assertFalse(store.saved.has(SPARK));assertEquals(0,store.saved.obols());
        int writes=store.writes;assertEquals(0,service.settle(run,RunOutcome.VICTORY));assertEquals(writes,store.writes);
        store.fail=false;assertTrue(service.retrySave());CampaignProfile loaded=new ProfileService(store).profile();
        assertTrue(loaded.has(SPARK));assertFalse(loaded.has(READ));assertEquals(10,loaded.obols());assertEquals(1,loaded.attempts());assertEquals(1,loaded.victories());
    }

    @Test void onlyABoundChosenCharactersVictoryCanGrantTheirPermanentSpark() {
        Store emptyStore=new Store();ProfileService empty=new ProfileService(emptyStore);
        assertEquals(10,empty.settle(won(empty,RegionDefinition.PROTOTYPE_ID),RunOutcome.VICTORY));
        assertFalse(empty.profile().has(SPARK));assertFalse(new ProfileService(emptyStore).profile().has(SPARK));
        Store recipientStore=new Store();ProfileService recipient=character(recipientStore),owner=character(ProfileStore.memory());
        RunSession foreign=won(owner,RegionDefinition.PROTOTYPE_ID);
        assertEquals(10,recipient.settle(foreign,RunOutcome.VICTORY));assertFalse(recipient.profile().has(SPARK));
        assertFalse(new ProfileService(recipientStore).profile().has(SPARK));
        RunSession unbound=RunSession.campaign(region(RegionDefinition.PROTOTYPE_ID));
        unbound.recordKill(EnemyType.COMMON);unbound.enterArena("boss");unbound.recordBossDefeat();unbound.end();
        assertEquals(10,recipient.settle(unbound,RunOutcome.VICTORY));assertFalse(recipient.profile().has(SPARK));
        assertEquals(10,recipient.settle(won(recipient,RegionDefinition.PROTOTYPE_ID),RunOutcome.VICTORY));
        assertTrue(recipient.profile().has(SPARK));assertTrue(new ProfileService(recipientStore).profile().has(SPARK));
    }

    @Test void epilogueCompletionRequiresTheCurrentConquerorAndNeverRewardsAnotherReturn() {
        Store store=new Store();ProfileService service=character(store);String id=service.profile().characterId();
        assertFalse(service.finishUnderworldEpilogue(id));RunSession run=won(service,RegionDefinition.PROTOTYPE_ID);service.settle(run,RunOutcome.VICTORY);
        int coins=service.profile().obols(),attempts=service.profile().attempts(),baseline=store.writes;
        assertFalse(service.finishUnderworldEpilogue(java.util.UUID.randomUUID().toString()));assertFalse(service.profile().has(READ));
        assertTrue(service.finishUnderworldEpilogue(id));assertTrue(service.finishUnderworldEpilogue(id));assertEquals(baseline+1,store.writes);
        assertTrue(new ProfileService(store).profile().has(READ));assertEquals(coins,service.profile().obols());assertEquals(attempts,service.profile().attempts());
        assertEquals(1,service.profile().victories());assertEquals(0,service.profile().deaths());
    }

    @Test void failedEpilogueAutosaveRemembersReadingAndExplicitRetryMakesItDurable() {
        Store store=new Store();ProfileService service=character(store);RunSession run=won(service,RegionDefinition.PROTOTYPE_ID);service.settle(run,RunOutcome.VICTORY);
        String id=service.profile().characterId();store.fail=true;
        assertFalse(service.finishUnderworldEpilogue(id));assertTrue(service.profile().has(READ));assertFalse(store.saved.has(READ));assertFalse(service.error().isEmpty());
        int writes=store.writes;assertTrue(service.finishUnderworldEpilogue(id));assertEquals(writes,store.writes);
        store.fail=false;assertTrue(service.retrySave());CampaignProfile loaded=new ProfileService(store).profile();
        assertTrue(loaded.has(SPARK));assertTrue(loaded.has(READ));assertEquals(10,loaded.obols());assertEquals(1,loaded.attempts());
    }

    @Test void slotIsolationAndResetRemoveOnlyTheSelectedCharactersLessonsAndStory() throws Exception {
        SaveSlots slots=SaveSlots.memory();ProfileService first=character(slots.store(0)),second=character(slots.store(1));
        String oldId=first.profile().characterId(),otherId=second.profile().characterId();
        first.completeTutorial(oldId,TutorialTopic.ATTACK);first.setTutorialEnabled(oldId,false);
        first.settle(won(first,RegionDefinition.PROTOTYPE_ID),RunOutcome.VICTORY);first.finishUnderworldEpilogue(oldId);
        second.completeTutorial(otherId,TutorialTopic.REFUGE);String other=second.profile().toJson();
        assertFalse(second.completeTutorial(oldId,TutorialTopic.ATTACK));assertFalse(second.profile().has(SPARK));
        assertTrue(first.reset());assertNotEquals(oldId,first.profile().characterId());assertFalse(first.profile().professionChosen());assertFalse(first.profile().hasProgress());
        assertTrue(first.profile().tutorialEnabled());for(TutorialTopic topic:TutorialTopic.values())assertFalse(first.profile().tutorialSeen(topic));
        assertFalse(first.profile().has(SPARK));assertFalse(first.profile().has(READ));assertFalse(first.completeTutorial(oldId,TutorialTopic.MOVE));
        assertTrue(first.createCharacter(Profession.VIGILANTE,GameDifficulty.HARD,false));assertFalse(first.finishUnderworldEpilogue(oldId));
        assertFalse(first.profile().has(SPARK));assertEquals(other,new ProfileService(slots.store(1)).profile().toJson());
    }
}
