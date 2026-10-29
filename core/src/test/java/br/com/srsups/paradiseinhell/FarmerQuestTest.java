package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FarmerQuestTest {
    private static RegionDefinition region(String id) {
        return new RegionDefinition(id,"Submundo de teste","fight",List.of(
            new ArenaDefinition("fight","Estige","",ArenaDefinition.Kind.COMBAT,0,
                Map.of(EnemyType.COMMON,1),List.of("side","boss")),
            new ArenaDefinition("side","Cinzas","",ArenaDefinition.Kind.COMBAT,60,
                Map.of(EnemyType.COMMON,1),List.of("boss")),
            new ArenaDefinition("boss","Guardião","",ArenaDefinition.Kind.BOSS_ROOM,120,Map.of(),List.of())));
    }
    private static CampaignProfile character() {
        CampaignProfile p=new CampaignProfile();p.createCharacter(Profession.CACADOR,GameDifficulty.HARD);return p;
    }
    private static ProfileService service(ProfileStore store) throws IOException {
        store.save(character());return new ProfileService(store);
    }
    private static RunSession run(ProfileService service) {
        return run(service,RegionDefinition.PROTOTYPE_ID);
    }
    private static RunSession run(ProfileService service,String regionId) {
        CampaignProfile p=service.profile();
        RunSession run=RunSession.campaign(region(regionId),p.profession(),p.bonuses(),p.difficulty(),
            new ForgeProgressTest.Rolls(.5f,1,1));
        service.prepareRun(run);return run;
    }
    private static void meetGoals(RunSession run) {
        for(var goal:run.arena().killGoals().entrySet())
            while(run.kills(goal.getKey())<goal.getValue())run.recordKill(goal.getKey());
    }
    private static void accept(ProfileService service,RunSession run) {
        meetGoals(run);
        assertTrue(run.encounters().openFarmer(true,new ForgeProgressTest.Rolls(0)));
        assertTrue(service.acceptFarmerQuest(run));
        assertEquals(FarmerQuest.ACCEPTED,service.profile().farmerQuest());
        assertEquals(FarmerQuest.ACCEPTED,run.farmerQuest());
    }
    private static void defeatMini(RunSession run) {
        run.advance(RunEncounters.MINI_BOSS_TIME);assertTrue(run.miniBossDue());
        run.markMiniBossSpawned();run.recordMiniBossDefeat();
    }
    private static void win(RunSession run) {
        meetGoals(run);run.enterArena("boss");run.recordBossDefeat();
    }
    private static String json(JsonValue value) { return value.toJson(JsonWriter.OutputType.json); }
    private static final class CountedRoll extends Random {
        private final float value;
        private int draws;
        CountedRoll(float value) { this.value=value; }
        @Override public float nextFloat() { draws++;return value; }
    }
    private static final class FlakyProfileStore implements ProfileStore {
        private CampaignProfile saved=character();
        private boolean fail;
        public CampaignProfile load() { return saved.copy(); }
        public void save(CampaignProfile p) throws IOException {
            if(fail)throw new IOException("test");saved=p.copy();
        }
    }
    @Test void legacyProfilesAndSharedProgressStartWithoutTheQuestOrEpicUnlock() {
        CampaignProfile original=character();original.credit(47);original.buy(PermanentAttribute.ARMOR);
        JsonValue oldProfile=new JsonReader().parse(original.toJson());oldProfile.remove("farmerQuest");
        CampaignProfile migrated=CampaignProfile.fromJson(json(oldProfile));
        assertEquals(FarmerQuest.NOT_STARTED,migrated.farmerQuest());assertEquals(original.obols(),migrated.obols());
        assertEquals(original.characterId(),migrated.characterId());assertEquals(original.difficulty(),migrated.difficulty());
        assertEquals(1,migrated.bonuses().armor());assertEquals(original.profession(),migrated.profession());
        SharedProgress shared=new SharedProgress();shared.credit(37);
        JsonValue oldShared=new JsonReader().parse(shared.toJson());oldShared.remove("farmerUnlocked");
        SharedProgress loaded=SharedProgress.fromJson(json(oldShared));
        assertFalse(loaded.farmerUnlocked());assertEquals(37,loaded.souls());assertFalse(loaded.necromancerUnlocked());
    }
    @Test void invalidQuestStatesAndEpicUnlockTypesAreRejected() {
        for(String value:new String[]{"\"UNKNOWN\"","1","true","null","[]","{}"}) {
            JsonValue profile=new JsonReader().parse(character().toJson());profile.remove("farmerQuest");
            profile.addChild("farmerQuest",new JsonReader().parse(value));
            assertThrows(IllegalArgumentException.class,()->CampaignProfile.fromJson(json(profile)),value);
        }
        for(String value:new String[]{"\"true\"","1","null","[]","{}"}) {
            JsonValue shared=new JsonReader().parse(new SharedProgress().toJson());shared.remove("farmerUnlocked");
            shared.addChild("farmerUnlocked",new JsonReader().parse(value));
            assertThrows(IllegalArgumentException.class,()->SharedProgress.fromJson(json(shared)),value);
        }
    }
    @Test void npcDrawIsLazyAndNeverRepeatsAfterClosingOrChangingRooms() {
        RunEncounters encounters=new RunEncounters(new ForgeProgressTest.Rolls(.5f),1);CountedRoll roll=new CountedRoll(.09999f);
        assertFalse(encounters.openFarmer(false,roll));assertEquals(0,roll.draws);
        assertTrue(encounters.openFarmer(true,roll));assertTrue(encounters.farmerOpen());assertEquals(1,roll.draws);
        encounters.closeFarmer();assertFalse(encounters.farmerOpen());assertFalse(encounters.openFarmer(true,roll));
        encounters.enterRoom();assertFalse(encounters.openFarmer(true,roll));assertEquals(1,roll.draws);
        RunEncounters absent=new RunEncounters(new ForgeProgressTest.Rolls(.5f),1);CountedRoll failed=new CountedRoll(.10f);
        assertFalse(absent.openFarmer(true,failed));assertFalse(absent.openFarmer(true,failed));assertEquals(1,failed.draws);
        assertTrue(new RunEncounters(new ForgeProgressTest.Rolls(.5f),4).openFarmer(true,new CountedRoll(.39999f)));
        assertFalse(new RunEncounters(new ForgeProgressTest.Rolls(.5f),4).openFarmer(true,new CountedRoll(.40f)));
    }
    @Test void acceptingRequiresAnOpenEncounterAndCommitsBeforeChangingEitherQuestState() {
        FlakyProfileStore store=new FlakyProfileStore();ProfileService service=new ProfileService(store);RunSession run=run(service);
        meetGoals(run);assertFalse(service.acceptFarmerQuest(run));
        assertTrue(run.encounters().openFarmer(true,new ForgeProgressTest.Rolls(0)));store.fail=true;
        assertFalse(service.acceptFarmerQuest(run));assertEquals(FarmerQuest.NOT_STARTED,service.profile().farmerQuest());
        assertEquals(FarmerQuest.NOT_STARTED,run.farmerQuest());assertEquals(FarmerQuest.NOT_STARTED,store.saved.farmerQuest());
        assertFalse(service.error().isEmpty());store.fail=false;assertTrue(service.acceptFarmerQuest(run));
        assertEquals(FarmerQuest.ACCEPTED,store.saved.farmerQuest());assertEquals(FarmerQuest.ACCEPTED,run.farmerQuest());
        assertEquals(store.saved.characterId(),run.characterId());assertEquals(0,store.saved.obols());
    }
    @Test void oneCharactersEncounterAndEarnedSeedCannotAdvanceAnotherSave() throws Exception {
        SaveSlots slots=SaveSlots.memory();ProfileService owner=service(slots.store(0)),other=service(slots.store(1));
        RunSession run=run(owner);meetGoals(run);
        assertTrue(run.encounters().openFarmer(true,new ForgeProgressTest.Rolls(0)));
        assertFalse(other.acceptFarmerQuest(run));assertEquals(FarmerQuest.NOT_STARTED,other.profile().farmerQuest());
        assertEquals(FarmerQuest.NOT_STARTED,run.farmerQuest());assertTrue(owner.acceptFarmerQuest(run));
        defeatMini(run);other.collectFarmerQuest(run);
        assertEquals(FarmerQuest.NOT_STARTED,other.profile().farmerQuest());assertEquals(FarmerQuest.NOT_STARTED,slots.store(1).load().farmerQuest());
        owner.collectFarmerQuest(run);assertEquals(FarmerQuest.SEED_FOUND,slots.store(0).load().farmerQuest());
    }
    @Test void miniBossesBeforeAcceptanceDoNotCountAndTheSeedIgnoresForgeDropChanceAndRegionalCap() throws Exception {
        ProfileStore store=ProfileStore.memory();ProfileService service=service(store);
        service.profile().claimForgeMaterial(RegionDefinition.PROTOTYPE_ID);assertTrue(service.retrySave());
        RunSession run=run(service);defeatMini(run);service.collectFarmerQuest(run);
        assertEquals(FarmerQuest.NOT_STARTED,run.farmerQuest());assertEquals(FarmerQuest.NOT_STARTED,service.profile().farmerQuest());
        accept(service,run);assertEquals(0,run.pendingForgeMaterials());
        run.enterArena("side");service.prepareRun(run);defeatMini(run);
        assertEquals(FarmerQuest.SEED_FOUND,run.farmerQuest());assertEquals(0,run.pendingForgeMaterials());
        service.collectFarmerQuest(run);service.collectFarmerQuest(run);
        assertEquals(FarmerQuest.SEED_FOUND,store.load().farmerQuest());assertEquals(1,service.profile().forgeMaterials());
        assertFalse(service.profile().canClaimForgeMaterial(RegionDefinition.PROTOTYPE_ID));
    }
    @Test void acceptanceAndSeedSurviveDeathsAbandonmentNewRunsAndReloads() throws Exception {
        ProfileStore store=ProfileStore.memory();ProfileService service=service(store);RunSession accepted=run(service);
        accept(service,accepted);accepted.end();service.settle(accepted,RunOutcome.DEATH);
        service=new ProfileService(store);assertEquals(FarmerQuest.ACCEPTED,service.profile().farmerQuest());
        RunSession seeded=run(service);assertEquals(FarmerQuest.ACCEPTED,seeded.farmerQuest());
        defeatMini(seeded);service.collectFarmerQuest(seeded);seeded.end();service.settle(seeded,RunOutcome.DEATH);
        ProfileService afterDeath=new ProfileService(store);RunSession after=run(afterDeath);
        assertEquals(FarmerQuest.SEED_FOUND,after.farmerQuest());after.end();afterDeath.settle(after,RunOutcome.ABANDONED);
        ProfileService afterAbandon=new ProfileService(store);
        assertEquals(FarmerQuest.SEED_FOUND,afterAbandon.profile().farmerQuest());
        assertEquals(FarmerQuest.SEED_FOUND,run(afterAbandon).farmerQuest());assertEquals(2,afterAbandon.profile().deaths());
    }
    @Test void earnedSeedsRemainInMemoryAfterFailedAutosavesAndRetryPersistsThemOnce() {
        FlakyProfileStore store=new FlakyProfileStore();ProfileService service=new ProfileService(store);RunSession run=run(service);
        accept(service,run);store.fail=true;defeatMini(run);service.collectFarmerQuest(run);
        assertEquals(FarmerQuest.SEED_FOUND,run.farmerQuest());assertEquals(FarmerQuest.SEED_FOUND,service.profile().farmerQuest());
        assertEquals(FarmerQuest.ACCEPTED,store.saved.farmerQuest());assertFalse(service.error().isEmpty());
        service.collectFarmerQuest(run);assertEquals(FarmerQuest.SEED_FOUND,service.profile().farmerQuest());
        store.fail=false;assertTrue(service.retrySave());
        ProfileService loaded=new ProfileService(store);assertEquals(FarmerQuest.SEED_FOUND,loaded.profile().farmerQuest());
        assertEquals(0,loaded.profile().forgeMaterials());assertEquals(0,loaded.profile().obols());
    }
    @Test void winningWithoutASeedDoesNotCompleteTheQuestAndOtherRegionsCannotCompleteIt() throws Exception {
        ProfileStore emptyStore=ProfileStore.memory();ProfileService empty=service(emptyStore);RunSession noQuest=run(empty);
        win(noQuest);empty.collectFarmerQuest(noQuest);assertEquals(FarmerQuest.NOT_STARTED,empty.profile().farmerQuest());
        ProfileStore store=ProfileStore.memory();ProfileService service=service(store);RunSession noSeed=run(service);
        accept(service,noSeed);win(noSeed);service.collectFarmerQuest(noSeed);
        assertEquals(FarmerQuest.ACCEPTED,service.profile().farmerQuest());
        RunSession seedRun=run(service);defeatMini(seedRun);service.collectFarmerQuest(seedRun);
        RunSession elsewhere=run(service,"another-region");assertFalse(elsewhere.questEnabled());
        win(elsewhere);service.collectFarmerQuest(elsewhere);
        assertEquals(FarmerQuest.SEED_FOUND,service.profile().farmerQuest());
        RunSession demo=RunSession.demo(service.profile().profession());service.prepareRun(demo);
        assertFalse(demo.questEnabled());assertFalse(service.acceptFarmerQuest(demo));
    }
    @Test void guardianCompletionSurvivesEndAndFailedSavesUntilTheProfileCanBeWritten() {
        FlakyProfileStore store=new FlakyProfileStore();ProfileService service=new ProfileService(store);RunSession run=run(service);
        accept(service,run);defeatMini(run);service.collectFarmerQuest(run);assertEquals(FarmerQuest.SEED_FOUND,store.saved.farmerQuest());
        store.fail=true;win(run);assertEquals(FarmerQuest.COMPLETED,run.farmerQuest());
        service.collectFarmerQuest(run);assertEquals(FarmerQuest.COMPLETED,service.profile().farmerQuest());
        assertEquals(FarmerQuest.SEED_FOUND,store.saved.farmerQuest());run.end();assertEquals(FarmerQuest.COMPLETED,run.farmerQuest());
        service.settle(run,RunOutcome.VICTORY);assertEquals(FarmerQuest.COMPLETED,service.profile().farmerQuest());
        store.fail=false;assertTrue(service.retrySave());
        ProfileService loaded=new ProfileService(store);assertEquals(FarmerQuest.COMPLETED,loaded.profile().farmerQuest());
        assertEquals(1,loaded.profile().victories());assertEquals(loaded.profile().characterId(),run.characterId());
    }
    @Test void completedProfileRecoversTheSharedUnlockAndAllowsOnlyNewFarmerCharacters() throws Exception {
        SaveSlots slots=SaveSlots.memory();ProfileService service=service(slots.store(0));RunSession run=run(service);
        accept(service,run);defeatMini(run);service.collectFarmerQuest(run);win(run);service.collectFarmerQuest(run);
        run.end();service.settle(run,RunOutcome.VICTORY);
        assertFalse(slots.sharedStore().load().farmerUnlocked());
        Main loaded=new Main(slots);assertTrue(loaded.shared().progress().farmerUnlocked());
        assertTrue(slots.sharedStore().load().farmerUnlocked());assertEquals(0,loaded.shared().progress().souls());
        assertFalse(loaded.shared().progress().necromancerUnlocked());assertEquals(Profession.CACADOR,loaded.profiles().profile().profession());
        assertFalse(loaded.createCharacter(Profession.FAZENDEIRO,GameDifficulty.EASY));
        ProfileService fresh=loaded.profileAt(3);
        assertTrue(fresh.createCharacter(Profession.FAZENDEIRO,GameDifficulty.EASY,false,loaded.shared().progress().farmerUnlocked()));
        assertEquals(Profession.FAZENDEIRO,new ProfileService(slots.store(3)).profile().profession());
        assertEquals(GameDifficulty.EASY,new ProfileService(slots.store(3)).profile().difficulty());
        assertEquals(FarmerQuest.NOT_STARTED,fresh.profile().farmerQuest());
        assertTrue(service.reset());assertEquals(FarmerQuest.NOT_STARTED,service.profile().farmerQuest());
        assertTrue(new Main(slots).shared().progress().farmerUnlocked());
    }
    @Test void sharedUnlockFailureRetainsTheRewardForRetryWithoutChargingEitherCurrency() throws Exception {
        SharedProgress[] saved={new SharedProgress()};saved[0].credit(59);boolean[] fail={true};
        SharedProgressStore store=new SharedProgressStore() {
            public SharedProgress load() { return saved[0].copy(); }
            public void save(SharedProgress p) throws IOException {
                if(fail[0])throw new IOException("test");saved[0]=p.copy();
            }
        };
        SharedProgressService shared=new SharedProgressService(store);shared.preserveFarmerUnlock();
        assertTrue(shared.progress().farmerUnlocked());assertFalse(saved[0].farmerUnlocked());assertEquals(59,shared.progress().souls());
        assertFalse(shared.error().isEmpty());fail[0]=false;assertTrue(shared.retrySave());shared.preserveFarmerUnlock();
        SharedProgress loaded=store.load();assertTrue(loaded.farmerUnlocked());assertEquals(59,loaded.souls());assertTrue(loaded.history().isEmpty());
    }
    @Test void deletingTheCompletedCharacterWaitsUntilItsSharedUnlockIsDurable() throws Exception {
        SharedProgress[] saved={new SharedProgress()};saved[0].credit(41);boolean[] fail={true};
        SharedProgressStore globalStore=new SharedProgressStore() {
            public SharedProgress load() { return saved[0].copy(); }
            public void save(SharedProgress value) throws IOException {
                if(fail[0])throw new IOException("test");saved[0]=value.copy();
            }
        };
        ProfileStore personalStore=ProfileStore.memory();SaveSlots slots=new SaveSlots(globalStore,personalStore);
        ProfileService owner=service(personalStore);RunSession run=run(owner);
        accept(owner,run);defeatMini(run);owner.collectFarmerQuest(run);win(run);owner.collectFarmerQuest(run);
        run.end();owner.settle(run,RunOutcome.VICTORY);String completedJson=personalStore.load().toJson();
        Main game=new Main(slots);assertTrue(game.shared().progress().farmerUnlocked());
        assertFalse(game.shared().error().isEmpty());assertFalse(globalStore.load().farmerUnlocked());
        assertFalse(game.deleteSave(0));assertEquals(completedJson,personalStore.load().toJson());
        assertEquals(FarmerQuest.COMPLETED,game.profileAt(0).profile().farmerQuest());
        assertEquals(owner.profile().characterId(),game.profileAt(0).profile().characterId());
        fail[0]=false;assertTrue(game.deleteSave(0));
        assertTrue(globalStore.load().farmerUnlocked());assertEquals(41,globalStore.load().souls());
        assertTrue(game.shared().error().isEmpty());assertFalse(personalStore.load().hasProgress());
        assertEquals(FarmerQuest.NOT_STARTED,personalStore.load().farmerQuest());
        assertTrue(new Main(slots).shared().progress().farmerUnlocked());
    }
    @Test void lockedFarmerCannotBeCreatedThroughLegacyOrExplicitServiceEntryPoints() {
        ProfileService service=new ProfileService(ProfileStore.memory());
        assertFalse(service.chooseProfession(Profession.FAZENDEIRO));
        assertFalse(service.chooseProfession(Profession.FAZENDEIRO,true));
        assertFalse(service.chooseProfession(Profession.FAZENDEIRO,true,false));
        assertFalse(service.createCharacter(Profession.FAZENDEIRO,GameDifficulty.HARD,true));
        assertFalse(service.createCharacter(Profession.FAZENDEIRO,GameDifficulty.HARD,true,false));
        assertFalse(service.profile().professionChosen());
        assertTrue(service.createCharacter(Profession.FAZENDEIRO,GameDifficulty.HARD,false,true));
        assertEquals(Profession.FAZENDEIRO,service.profile().profession());assertEquals(GameDifficulty.HARD,service.profile().difficulty());
        assertEquals(0,service.profile().obols());assertFalse(service.createCharacter(Profession.CACADOR,GameDifficulty.EASY,true,true));
    }
}
