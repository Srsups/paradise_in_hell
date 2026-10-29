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

/** Crash boundaries and durable reward identity, independently of the graphical screens. */
class CheckpointPersistenceTest {
    @TempDir Path directory;
    private static final class Store implements ProfileStore {
        CampaignProfile saved=new CampaignProfile();boolean fail;int writes,terminalWrites;
        @Override public CampaignProfile load(){return saved.copy();}
        @Override public void save(CampaignProfile value)throws IOException{writes++;if(fail)throw new IOException("disk full");saved=value.copy();}
        @Override public void saveTerminal(CampaignProfile value)throws IOException{terminalWrites++;save(value);}
    }
    private static final class GlobalStore implements SharedProgressStore {
        SharedProgress saved=new SharedProgress();boolean fail;
        @Override public SharedProgress load(){return saved.copy();}
        @Override public void save(SharedProgress value)throws IOException{if(fail)throw new IOException("shared disk full");saved=value.copy();}
    }
    private static RegionDefinition region(){return region("checkpoint-test");}
    private static RegionDefinition region(String id){return new RegionDefinition(id,"Checkpoint test","fight",List.of(
        new ArenaDefinition("fight","Fight","",ArenaDefinition.Kind.COMBAT,0,Map.of(EnemyType.COMMON,1),List.of("boss")),
        new ArenaDefinition("boss","Boss","",ArenaDefinition.Kind.BOSS_ROOM,0,Map.of(),List.of())));}
    private static ProfileService character(ProfileStore store){
        ProfileService profile=new ProfileService(store);
        assertTrue(profile.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM,false));return profile;
    }
    private static RunSession run(ProfileService service){return run(service,region());}
    private static RunSession run(ProfileService service,RegionDefinition region){
        RunSession run=RunSession.campaign(region,service.profile().profession(),service.profile().bonuses(),service.profile().difficulty(),
            new java.util.Random(){@Override public float nextFloat(){return 0;}});
        service.prepareRun(run);return run;
    }
    private static RunSession saved(ProfileService service){
        RunSession run=run(service);assertTrue(service.saveCheckpoint(run,RunCheckpoint.Stage.ROOM_START));return run;
    }
    private static void win(RunSession run){run.recordKill(EnemyType.COMMON);run.enterArena("boss");run.recordBossDefeat();run.end();}

    @Test void checkpointCommitsBeforeBindingAndAWriteFailureLeavesThePreviousAttemptUntouched(){
        Store store=new Store();ProfileService service=character(store);RunSession run=run(service);
        store.fail=true;assertFalse(service.saveCheckpoint(run,RunCheckpoint.Stage.ROOM_START));
        assertEquals(0,run.attemptSequence());assertNull(service.profile().checkpoint());assertNull(store.saved.checkpoint());
        store.fail=false;assertTrue(service.saveCheckpoint(run,RunCheckpoint.Stage.ROOM_START));
        assertEquals(1,run.attemptSequence());assertEquals(run.id(),store.saved.checkpoint().runId());
        RunCheckpoint first=service.profile().checkpoint();
        store.fail=true;assertFalse(service.saveCheckpoint(run,RunCheckpoint.Stage.ROOM_START));
        assertEquals(first.revision(),service.profile().checkpoint().revision());
        assertEquals(first.revision(),store.saved.checkpoint().revision());
        store.fail=false;assertTrue(service.saveCheckpoint(run,RunCheckpoint.Stage.ROOM_START));
        assertEquals(first.revision()+1,service.profile().checkpoint().revision());
    }

    @Test void anAttemptCannotReplaceAnotherCharactersCheckpointOrAnExistingActiveAttempt(){
        Store first=new Store(),second=new Store();ProfileService a=character(first),b=character(second);
        RunSession run=saved(a);assertFalse(b.saveCheckpoint(run,RunCheckpoint.Stage.ROOM_START));
        assertNull(b.profile().checkpoint());assertEquals(run.id(),a.profile().checkpoint().runId());
        RunSession another=run(a);assertFalse(a.saveCheckpoint(another,RunCheckpoint.Stage.ROOM_START));
        assertEquals(0,another.attemptSequence());assertEquals(run.id(),first.saved.checkpoint().runId());
    }

    @Test void crashAfterPersonalSettlementResumesTheReceiptInsteadOfTheDeadAttempt(){
        Store store=new Store();ProfileService service=character(store);RunSession run=saved(service);win(run);
        int coins=service.settle(run,RunOutcome.VICTORY,0);assertTrue(coins>0);
        assertNull(service.profile().checkpoint());assertNotNull(service.pendingSettlement());assertTrue(service.settlementDurable());
        ProfileService restarted=new ProfileService(store);
        assertNull(restarted.profile().checkpoint());assertEquals(1,restarted.profile().attempts());
        assertEquals(coins,restarted.profile().obols());assertEquals(run.id(),restarted.pendingSettlement().report().id());
        SharedProgressService shared=new SharedProgressService(SharedProgressStore.memory());
        assertTrue(shared.settleReceipt(restarted.pendingSettlement()));
        assertTrue(restarted.acknowledgeSettlement(restarted.pendingSettlement()));
        assertNull(new ProfileService(store).pendingSettlement());assertNull(store.saved.checkpoint());
        assertEquals(run.earnedSouls(),shared.progress().souls());
    }

    @Test void personalSaveFailureRetainsAnUnsavedReceiptAndOnlyRetryMakesItDurable(){
        Store store=new Store();ProfileService service=character(store);RunSession run=saved(service);win(run);
        store.fail=true;service.settle(run,RunOutcome.VICTORY,0);
        assertFalse(service.settlementDurable());assertNotNull(service.pendingSettlement());assertNotNull(store.saved.checkpoint());
        int obols=service.profile().obols();
        assertFalse(service.acknowledgeSettlement(service.pendingSettlement()));
        assertEquals(0,service.settle(run,RunOutcome.VICTORY,0));assertEquals(obols,service.profile().obols());
        store.fail=false;assertTrue(service.retrySave());assertTrue(service.settlementDurable());
        assertNull(store.saved.checkpoint());assertEquals(1,store.saved.attempts());assertTrue(store.terminalWrites>0);
    }

    @Test void sharedSaveAndAcknowledgementFailuresNeverCreditTheSameReceiptTwice(){
        Store store=new Store();GlobalStore global=new GlobalStore();ProfileService profile=character(store);
        RunSession run=saved(profile);win(run);profile.settle(run,RunOutcome.VICTORY,0);
        RunSettlement receipt=profile.pendingSettlement();SharedProgressService shared=new SharedProgressService(global);
        global.fail=true;assertFalse(shared.settleReceipt(receipt));assertEquals(0,shared.progress().souls());
        assertEquals(0,global.saved.souls());global.fail=false;assertTrue(shared.retrySave());
        assertEquals(receipt.report().souls(),global.saved.souls());
        store.fail=true;assertFalse(profile.acknowledgeSettlement(receipt));
        ProfileService restarted=new ProfileService(store);SharedProgressService globalRestart=new SharedProgressService(global);
        assertNotNull(restarted.pendingSettlement());assertTrue(globalRestart.settleReceipt(restarted.pendingSettlement()));
        assertEquals(receipt.report().souls(),globalRestart.progress().souls());
        store.fail=false;assertTrue(restarted.acknowledgeSettlement(restarted.pendingSettlement()));
        assertNull(store.saved.pendingSettlement());assertEquals(1,store.saved.attempts());
    }

    @Test void receiptDeduplicationSurvivesEvictionFromTheTwentyVisibleReports(){
        Store store=new Store();ProfileService profile=character(store);RunSession run=saved(profile);win(run);
        profile.settle(run,RunOutcome.VICTORY,0);RunSettlement receipt=profile.pendingSettlement();
        GlobalStore global=new GlobalStore();SharedProgressService shared=new SharedProgressService(global);
        assertTrue(shared.settleReceipt(receipt));int balance=shared.progress().souls();
        for(int i=0;i<25;i++){RunSession demo=RunSession.demo();demo.end();shared.settle(demo,RunOutcome.ABANDONED,i%SaveSlots.COUNT);}
        assertEquals(20,shared.progress().history().size());
        assertTrue(shared.progress().history().stream().noneMatch(report->report.id().equals(receipt.report().id())));
        SharedProgressService restarted=new SharedProgressService(global);
        assertEquals(1,restarted.progress().highestCompletedSequence(receipt.report().characterId()));
        assertTrue(restarted.settleReceipt(receipt));assertEquals(balance,restarted.progress().souls());
    }

    @Test void terminalBackupNeverRevivesAnActiveCheckpointAfterPrimaryCorruption()throws Exception{
        Path file=directory.resolve("profile.json");FileProfileStore store=new FileProfileStore(file);
        ProfileService profile=character(store);RunSession run=saved(profile);win(run);profile.settle(run,RunOutcome.VICTORY,0);
        int coins=profile.profile().obols();Files.writeString(file,"corrupt primary");
        ProfileService recovered=new ProfileService(store);
        assertNull(recovered.profile().checkpoint());assertNotNull(recovered.pendingSettlement());
        assertEquals(coins,recovered.profile().obols());assertEquals(1,recovered.profile().attempts());
        SharedProgressService shared=new SharedProgressService(SharedProgressStore.memory());
        assertTrue(shared.settleReceipt(recovered.pendingSettlement()));assertTrue(recovered.acknowledgeSettlement(recovered.pendingSettlement()));
        Files.writeString(file,"corrupt again");CampaignProfile acknowledged=new ProfileService(store).profile();
        assertNull(acknowledged.checkpoint());assertNull(acknowledged.pendingSettlement());assertEquals(coins,acknowledged.obols());
    }

    @Test void legacyIdentityIsCommittedBeforeTheFirstCheckpointAndResetStartsAnEmptyIdentity(){
        CampaignProfile old=new CampaignProfile();old.createCharacter(Profession.CACADOR,GameDifficulty.MEDIUM);
        JsonValue data=new JsonReader().parse(old.toJson());data.get("version").set(1L,null);
        for(String key:List.of("characterId","checkpoint","pendingSettlement","attemptSequence","lastSettledSequence"))data.remove(key);
        Store store=new Store();store.saved=CampaignProfile.fromJson(data.toJson(JsonWriter.OutputType.json));
        ProfileService service=new ProfileService(store);assertEquals("",service.profile().characterId());
        store.fail=true;assertFalse(service.ensureCharacterIdentity());assertEquals("",service.profile().characterId());
        store.fail=false;assertTrue(service.ensureCharacterIdentity());String identity=service.profile().characterId();
        saved(service);assertTrue(service.reset());assertNotEquals(identity,service.profile().characterId());
        assertNull(service.profile().checkpoint());assertNull(service.pendingSettlement());assertEquals(0,service.profile().attemptSequence());
    }

    @Test void permanentForgingAndItsSafeCheckpointCommitTogether(){
        Store store=new Store();ProfileService profile=character(store);RunSession run=saved(profile);
        profile.profile().claimForgeMaterial(run.region().id());assertTrue(profile.retrySave());
        run.recordKill(EnemyType.COMMON);assertTrue(run.encounters().openBlacksmith());
        assertTrue(profile.saveCheckpoint(run,RunCheckpoint.Stage.BLACKSMITH));
        store.fail=true;assertFalse(profile.forgeWeapon(run));assertEquals(WeaponRarity.COMMON,run.player().getWeaponRarity());
        assertEquals(1,profile.profile().forgeMaterials());assertEquals(WeaponRarity.COMMON,store.saved.weaponRarity());
        store.fail=false;assertTrue(profile.forgeWeapon(run));assertEquals(0,store.saved.forgeMaterials());
        assertEquals(WeaponRarity.RARE,store.saved.weaponRarity());
        assertEquals(WeaponRarity.RARE,store.saved.checkpoint().restore(run.region()).player().getWeaponRarity());
    }

    @Test void futureProfileOrBackupIsNeverOverwrittenBySaveOrTerminalSave()throws Exception{
        Path file=directory.resolve("profile.json");FileProfileStore store=new FileProfileStore(file);
        CampaignProfile original=new CampaignProfile();store.save(original);store.save(original);
        Path backup=file.resolveSibling("profile.json.bak");
        String future=original.toJson().replace("\"version\": 2","\"version\": 99");Files.writeString(backup,future);
        String before=Files.readString(file);
        assertThrows(IOException.class,()->store.save(original));assertThrows(IOException.class,()->store.saveTerminal(original));
        assertEquals(before,Files.readString(file));assertEquals(future,Files.readString(backup));
        Files.writeString(file,future);assertThrows(IOException.class,store::load);assertEquals(future,Files.readString(file));
    }

    @Test void sharedReceiptBackupRetainsBalanceAndLedgerAfterAcknowledgementAndCorruption()throws Exception{
        Store personal=new Store();ProfileService profile=character(personal);RunSession run=saved(profile);win(run);
        profile.settle(run,RunOutcome.VICTORY,0);RunSettlement receipt=profile.pendingSettlement();
        Path file=directory.resolve("shared.json");FileSharedProgressStore store=new FileSharedProgressStore(file);
        SharedProgressService shared=new SharedProgressService(store);assertTrue(shared.settleReceipt(receipt));
        assertTrue(profile.acknowledgeSettlement(receipt));Files.writeString(file,"damaged global primary");
        SharedProgressService restarted=new SharedProgressService(store);
        assertEquals(receipt.report().souls(),restarted.progress().souls());
        assertEquals(receipt.sequence(),restarted.progress().highestCompletedSequence(receipt.report().characterId()));
        assertTrue(restarted.settleReceipt(receipt));assertEquals(receipt.report().souls(),restarted.progress().souls());
    }

    @Test void receiptValidationAndFutureSharedBackupProtectTheFinancialIdentity()throws Exception{
        Store personal=new Store();ProfileService profile=character(personal);RunSession run=saved(profile);win(run);
        profile.settle(run,RunOutcome.VICTORY,0);RunSettlement receipt=profile.pendingSettlement();
        assertEquals(receipt,RunSettlement.fromJson(receipt.toJson()));
        assertThrows(IllegalArgumentException.class,()->new RunSettlement(receipt.report(),0));
        JsonValue invalid=new JsonReader().parse(receipt.toJson());invalid.get("sequence").set(1.0,null);
        assertThrows(IllegalArgumentException.class,()->RunSettlement.fromJson(invalid));
        Path file=directory.resolve("shared.json");FileSharedProgressStore store=new FileSharedProgressStore(file);
        SharedProgress empty=new SharedProgress();store.save(empty);store.save(empty);
        Path backup=file.resolveSibling("shared.json.bak");String future=empty.toJson().replace("\"version\": 2","\"version\": 99");
        Files.writeString(backup,future);String before=Files.readString(file);
        assertThrows(IOException.class,()->store.save(empty));assertThrows(IOException.class,()->store.saveSettlement(empty));
        assertEquals(before,Files.readString(file));assertEquals(future,Files.readString(backup));
    }

    @Test void recoveredSequenceSynchronizationCommitsBeforeReusingAnAttemptIdentity(){
        Store store=new Store();ProfileService profile=character(store);
        String characterId=profile.profile().characterId();profile.profile().credit(35);assertTrue(profile.retrySave());
        store.fail=true;assertFalse(profile.syncAttemptSequence(12));
        assertEquals(0,profile.profile().attemptSequence());assertEquals(0,store.saved.lastSettledSequence());
        store.fail=false;assertTrue(profile.syncAttemptSequence(12));
        assertEquals(12,store.saved.attemptSequence());assertEquals(12,store.saved.lastSettledSequence());
        assertEquals(35,store.saved.obols());assertEquals(0,store.saved.attempts());assertEquals(characterId,store.saved.characterId());
        int writes=store.writes;assertTrue(profile.syncAttemptSequence(8));assertEquals(writes,store.writes);
        RunSession next=saved(profile);assertEquals(13,next.attemptSequence());
        assertFalse(profile.syncAttemptSequence(20));assertEquals(13,profile.profile().attemptSequence());
        assertEquals(next.id(),profile.profile().checkpoint().runId());
        assertThrows(IllegalArgumentException.class,()->profile.syncAttemptSequence(-1));
    }

    @Test void globallyConsumedRecoveredCheckpointIsDiscardedWithoutAnySecondReward(){
        Store store=new Store();ProfileService profile=character(store);RunSession run=saved(profile);
        CampaignProfile oldCheckpoint=store.saved.copy();win(run);profile.settle(run,RunOutcome.VICTORY,0);
        SharedProgressService shared=new SharedProgressService(SharedProgressStore.memory());
        assertTrue(shared.settleReceipt(profile.pendingSettlement()));
        long completed=shared.progress().highestCompletedSequence(oldCheckpoint.characterId());int souls=shared.progress().souls();
        store.saved=oldCheckpoint;ProfileService recovered=new ProfileService(store);
        assertFalse(recovered.discardCompletedCheckpoint(0));assertNotNull(recovered.profile().checkpoint());
        store.fail=true;assertFalse(recovered.discardCompletedCheckpoint(completed));
        assertNotNull(recovered.profile().checkpoint());assertEquals(0,recovered.profile().lastSettledSequence());
        store.fail=false;assertTrue(recovered.discardCompletedCheckpoint(completed));
        assertNull(store.saved.checkpoint());assertNull(store.saved.pendingSettlement());
        assertEquals(completed,store.saved.lastSettledSequence());assertEquals(0,store.saved.obols());assertEquals(0,store.saved.attempts());
        assertEquals(0,recovered.settle(run,RunOutcome.VICTORY,0));assertEquals(souls,shared.progress().souls());
        assertTrue(recovered.discardCompletedCheckpoint(completed));assertEquals(completed+1,saved(recovered).attemptSequence());
    }

    @Test void explicitIncompatibleCheckpointDiscardPreservesPermanentFactsAndMirrorsTheClosedBackup()throws Exception{
        Path file=directory.resolve("incompatible.json");FileProfileStore store=new FileProfileStore(file);
        ProfileService profile=character(store);CampaignProfile before=profile.profile();String identity=before.characterId();
        before.credit(70);before.buy(PermanentAttribute.ARMOR);before.claimForgeMaterial("past-region");before.forgeWeapon();
        before.acceptFarmerQuest();before.completeTutorial(TutorialTopic.MOVE);before.finishConversation(CampaignProfile.Event.BRIBE_JOKE);
        assertTrue(profile.retrySave());int coins=profile.profile().obols();RunSession run=saved(profile);
        assertThrows(IllegalArgumentException.class,()->profile.profile().checkpoint().restore(region("changed-content")));
        assertTrue(profile.discardIncompatibleCheckpoint());assertNull(profile.profile().checkpoint());
        assertEquals(run.attemptSequence(),profile.profile().lastSettledSequence());
        Files.writeString(file,"corrupt after confirmed discard");ProfileService recovered=new ProfileService(store);
        CampaignProfile kept=recovered.profile();assertNull(kept.checkpoint());assertNull(kept.pendingSettlement());
        assertEquals(identity,kept.characterId());assertEquals(coins,kept.obols());assertEquals(0,kept.attempts());
        assertEquals(1,kept.rank(PermanentAttribute.ARMOR));assertEquals(WeaponRarity.RARE,kept.weaponRarity());
        assertFalse(kept.canClaimForgeMaterial("past-region"));assertEquals(FarmerQuest.ACCEPTED,kept.farmerQuest());
        assertTrue(kept.tutorialSeen(TutorialTopic.MOVE));assertTrue(kept.has(CampaignProfile.Event.BRIBE_JOKE));
        assertEquals(run.attemptSequence()+1,saved(recovered).attemptSequence());
    }

    @Test void failedOrPrematureDiscardNeverErasesTheCheckpointOrItsPendingReturn(){
        Store store=new Store();ProfileService profile=character(store);RunSession run=saved(profile);RunCheckpoint old=profile.profile().checkpoint();
        store.fail=true;assertFalse(profile.discardIncompatibleCheckpoint());assertEquals(old.runId(),profile.profile().checkpoint().runId());
        assertEquals(0,profile.profile().lastSettledSequence());assertEquals(0,profile.profile().attempts());
        store.fail=false;win(run);profile.settle(run,RunOutcome.VICTORY,0);RunSettlement receipt=profile.pendingSettlement();
        String before=profile.profile().toJson();assertFalse(profile.discardIncompatibleCheckpoint());
        assertFalse(profile.discardCompletedCheckpoint(100));assertFalse(profile.syncAttemptSequence(100));
        assertEquals(before,profile.profile().toJson());assertEquals(receipt,profile.pendingSettlement());
    }

    @Test void futureEmbeddedCheckpointPlayerReceiptAndReportVersionsNeverRecoverOrOverwriteOlderBackups()throws Exception{
        Store memory=new Store();ProfileService profile=character(memory);RunSession run=saved(profile);
        CampaignProfile checkpoint=profile.profile().copy();win(run);profile.settle(run,RunOutcome.VICTORY,0);
        CampaignProfile receipt=profile.profile().copy();
        assertFutureProfileIsPreserved(checkpoint,root->root.get("checkpoint").get("version").set(2L,null),"checkpoint");
        assertFutureProfileIsPreserved(checkpoint,root->root.get("checkpoint").get("state").get("player").get("version").set(2L,null),"player");
        assertFutureProfileIsPreserved(receipt,root->root.get("pendingSettlement").get("version").set(2L,null),"receipt");
        assertFutureProfileIsPreserved(receipt,root->root.get("pendingSettlement").get("report").get("version").set(2L,null),"report");
        JsonValue futureReceipt=new JsonReader().parse(profile.pendingSettlement().toJson());futureReceipt.get("version").set(2L,null);
        assertThrows(RunSettlement.UnsupportedVersion.class,()->RunSettlement.fromJson(futureReceipt));
    }

    private void assertFutureProfileIsPreserved(CampaignProfile original,java.util.function.Consumer<JsonValue> change,String name)throws Exception{
        Path file=directory.resolve(name+".json"),backup=file.resolveSibling(name+".json.bak");
        FileProfileStore store=new FileProfileStore(file);store.save(original);store.save(original);
        String previous=Files.readString(backup);JsonValue value=new JsonReader().parse(original.toJson());change.accept(value);
        String future=value.toJson(JsonWriter.OutputType.json);Files.writeString(file,future);
        assertThrows(CampaignProfile.UnsupportedVersion.class,()->CampaignProfile.fromJson(future));
        ProfileService refused=new ProfileService(store);assertFalse(refused.writable());assertFalse(refused.retrySave());
        assertFalse(refused.error().isEmpty());assertThrows(IOException.class,()->store.save(original));
        assertThrows(IOException.class,()->store.saveTerminal(new CampaignProfile()));
        assertEquals(future,Files.readString(file));assertEquals(previous,Files.readString(backup));
        Files.writeString(file,previous);Files.writeString(backup,future);
        assertThrows(IOException.class,()->store.save(original));assertThrows(IOException.class,()->store.saveTerminal(new CampaignProfile()));
        assertEquals(previous,Files.readString(file));assertEquals(future,Files.readString(backup));
    }

    @Test void malformedVersionsRemainOrdinaryCorruptionAndUnversionedBoonFieldsDoNotClaimFutureSchemas()throws Exception{
        Store memory=new Store();ProfileService profile=character(memory);saved(profile);
        CampaignProfile original=profile.profile().copy();Path file=directory.resolve("malformed-version.json");
        FileProfileStore store=new FileProfileStore(file);store.save(original);store.save(original);
        for(JsonValue malformed:List.of(new JsonValue(0L),new JsonValue("2"),new JsonValue(2.0))){
            JsonValue bad=new JsonReader().parse(original.toJson());bad.get("checkpoint").remove("version");
            bad.get("checkpoint").addChild("version",malformed);
            Files.writeString(file,bad.toJson(JsonWriter.OutputType.json));
            ProfileService recovered=new ProfileService(store);assertTrue(recovered.writable());assertTrue(recovered.error().isEmpty());
            assertEquals(original.checkpoint().runId(),recovered.profile().checkpoint().runId());
        }
        JsonValue unknown=new JsonReader().parse(original.toJson());
        unknown.get("checkpoint").get("state").get("player").get("boons").addChild("version",new JsonValue(2L));
        CampaignProfile retained=CampaignProfile.fromJson(unknown.toJson(JsonWriter.OutputType.json));
        assertThrows(IllegalArgumentException.class,()->retained.checkpoint().restore(region()));
        assertEquals(original.characterId(),retained.characterId());
    }

    @Test void futureReportInsideSharedHistoryPreservesPrimaryAndBackupInsteadOfDowngrading()throws Exception{
        SharedProgress original=new SharedProgress();RunSession demo=RunSession.demo();demo.end();original.record(demo.report(RunOutcome.ABANDONED,0));
        Path file=directory.resolve("shared-future-report.json"),backup=file.resolveSibling("shared-future-report.json.bak");
        FileSharedProgressStore store=new FileSharedProgressStore(file);store.save(original);store.save(original);
        String previous=Files.readString(backup);JsonValue root=new JsonReader().parse(original.toJson());
        root.get("history").get(0).get("version").set(2L,null);String future=root.toJson(JsonWriter.OutputType.json);
        Files.writeString(file,future);assertThrows(SharedProgress.UnsupportedVersion.class,()->SharedProgress.fromJson(future));
        SharedProgressService refused=new SharedProgressService(store);assertFalse(refused.error().isEmpty());assertFalse(refused.retrySave());
        assertThrows(IOException.class,()->store.save(original));assertThrows(IOException.class,()->store.saveSettlement(original));
        assertEquals(future,Files.readString(file));assertEquals(previous,Files.readString(backup));
        Files.writeString(file,previous);Files.writeString(backup,future);
        assertThrows(IOException.class,()->store.save(original));assertThrows(IOException.class,()->store.saveSettlement(original));
        assertEquals(previous,Files.readString(file));assertEquals(future,Files.readString(backup));
    }
}
