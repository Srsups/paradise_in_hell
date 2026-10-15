package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SharedProgressTest {
    @TempDir Path directory;
    static RunSession run(int kills) {
        RunSession run=RunSession.campaign(CampaignProfileTest.region());
        for(int i=0;i<kills;i++) run.recordKill(EnemyType.COMMON);
        run.end(); return run;
    }
    @Test void twoDifferentCharactersContributeToTheSameBalanceAndReload() throws Exception {
        SaveSlots slots=SaveSlots.memory(); Main game=new Main(slots);
        game.profiles().chooseProfession(Profession.CACADOR); game.profileAt(1).chooseProfession(Profession.FERREIRO);
        game.shared().settle(run(400),RunOutcome.DEATH,0);
        game.shared().settle(run(600),RunOutcome.ABANDONED,1);
        assertEquals(1000,game.shared().progress().souls());
        assertTrue(game.unlockNecromancer()); assertEquals(0,game.shared().progress().souls());
        Main reloaded=new Main(slots);
        assertTrue(reloaded.necromancerUnlocked()); assertEquals(2,reloaded.shared().progress().history().size());
        assertEquals(0,reloaded.profileAt(0).profile().obols()); assertEquals(0,reloaded.profileAt(1).profile().obols());
    }
    @Test void retriesReopeningAndRepeatedSettlementNeverDuplicateSoulsOrReports() throws Exception {
        SharedProgressStore store=SharedProgressStore.memory(); SharedProgressService service=new SharedProgressService(store);
        RunSession run=run(5);
        RunReport report=service.settle(run,RunOutcome.DEATH,0);
        service.settle(run,RunOutcome.DEATH,0); service.retrySave();
        assertEquals(5,service.progress().souls()); assertEquals(1,service.progress().history().size());
        SharedProgress reloaded=store.load();
        assertFalse(reloaded.record(report)); assertEquals(5,reloaded.souls()); assertEquals(1,reloaded.history().size());
    }
    @Test void failedRewardSaveKeepsTheRewardAndRetryWritesItOnce() throws Exception {
        SharedProgress[] saved={new SharedProgress()}; boolean[] fail={true};
        SharedProgressService service=new SharedProgressService(new SharedProgressStore() {
            public SharedProgress load() { return saved[0].copy(); }
            public void save(SharedProgress p) throws IOException { if(fail[0])throw new IOException("test"); saved[0]=p.copy(); }
        });
        RunSession run=run(7); service.settle(run,RunOutcome.ABANDONED,2);
        assertEquals(7,service.progress().souls()); assertEquals(0,saved[0].souls()); assertFalse(service.error().isEmpty());
        fail[0]=false; assertTrue(service.retrySave()); service.settle(run,RunOutcome.ABANDONED,2);
        assertEquals(7,saved[0].souls()); assertEquals(1,saved[0].history().size());
    }
    @Test void demoReportsDoNotGrantEitherCurrency() throws Exception {
        RunSession demo=RunSession.demo(); for(int i=0;i<100;i++)demo.recordKill(EnemyType.TANK); demo.end();
        SharedProgressService service=new SharedProgressService(SharedProgressStore.memory());
        RunReport report=service.settle(demo,RunOutcome.DEATH,0);
        assertEquals(0,report.souls()); assertEquals(0,report.obols()); assertEquals(0,service.progress().souls());
        assertEquals(1,service.progress().history().size());
    }
    @Test void historyIsBoundedAndKeepsTheMostRecentReportForEachCharacter() throws Exception {
        SharedProgressService service=new SharedProgressService(SharedProgressStore.memory());
        for(int i=0;i<25;i++) service.settle(run(1),RunOutcome.DEATH,i%2);
        assertEquals(20,service.progress().history().size()); assertEquals(25,service.progress().souls());
        assertEquals(1,service.progress().latest(0).saveSlot()); assertEquals(2,service.progress().latest(1).saveSlot());
    }
    @Test void legacyPaidUnlockIsMigratedWithoutSpendingPersonalOrSharedCurrency() throws Exception {
        CampaignProfile legacy=CampaignProfile.fromJson(new CampaignProfile().toJson().replace("\"necromancerUnlocked\": false","\"necromancerUnlocked\": true"));
        ProfileStore personal=ProfileStore.memory(); legacy.credit(42); personal.save(legacy);
        SaveSlots slots=new SaveSlots(personal); Main game=new Main(slots);
        assertTrue(game.necromancerUnlocked()); assertEquals(0,game.shared().progress().souls());
        assertEquals(42,game.profiles().profile().obols()); assertTrue(slots.sharedStore().load().necromancerUnlocked());
    }
    @Test void anExistingNecromancerAlsoPreservesAccessWhenTheLegacyFlagIsAbsent() throws Exception {
        ProfileStore personal=ProfileStore.memory(); CampaignProfile old=new CampaignProfile();
        old.chooseProfession(Profession.NECROMANTE); personal.save(old);
        Main game=new Main(new SaveSlots(personal)); assertTrue(game.necromancerUnlocked());
        assertEquals(Profession.NECROMANTE,game.profiles().profile().profession());
    }
    @Test void corruptedPrimaryRecoversTheLastValidSharedBackup() throws Exception {
        Path file=directory.resolve("shared-progress.json"); SharedProgressStore store=new FileSharedProgressStore(file);
        SharedProgress progress=new SharedProgress(); progress.credit(123); store.save(progress);
        progress.credit(50); store.save(progress); Files.writeString(file,"broken");
        assertEquals(123,store.load().souls()); assertTrue(Files.exists(directory.resolve("shared-progress.json.bak")));
    }
    @Test void unreadableOrNewerFilesArePreservedAndCannotBeSpentOrOverwritten() throws Exception {
        Path file=directory.resolve("shared-progress.json"),backup=directory.resolve("shared-progress.json.bak");
        Files.writeString(file,new SharedProgress().toJson().replace("\"version\": 1","\"version\": 2"));
        Files.writeString(backup,new SharedProgress().toJson());
        SharedProgressService service=new SharedProgressService(new FileSharedProgressStore(file));
        service.progress().credit(1000); assertFalse(service.unlockNecromancer()); assertFalse(service.retrySave());
        assertTrue(Files.readString(file).contains("\"version\": 2")); assertFalse(service.error().isEmpty());
        Files.writeString(file,"broken"); Files.writeString(backup,"also broken");
        service=new SharedProgressService(new FileSharedProgressStore(file));
        service.settle(run(3),RunOutcome.DEATH,0);
        assertEquals(3,service.progress().souls()); assertEquals("broken",Files.readString(file));
    }
    @Test void olderAthenaReportsKeepSharedSoulsHistoryAndUnlockPurchasesReadable() throws Exception {
        RunSession run = RunSession.campaign(CampaignProfileTest.region());
        run.applyUpgrade(Upgrade.INTELIGENCIA); run.recordKill(EnemyType.COMMON); run.end();
        SharedProgress original = new SharedProgress();
        original.record(run.report(RunOutcome.ABANDONED, 0)); original.credit(1999);
        var json = new com.badlogic.gdx.utils.JsonReader().parse(original.toJson());
        var ranks = json.get("history").get(0).get("boons");
        ranks.remove("INTELIGENCIA");
        ranks.addChild("INTELIGENCIA", new com.badlogic.gdx.utils.JsonValue(3L));
        Path file = directory.resolve("shared-progress.json");
        Files.writeString(file, json.prettyPrint(com.badlogic.gdx.utils.JsonWriter.OutputType.json, 100));
        SharedProgressStore store = new FileSharedProgressStore(file);
        SharedProgressService service = new SharedProgressService(store);
        assertTrue(service.error().isEmpty()); assertEquals(2000, service.progress().souls());
        assertEquals(3, service.progress().history().getFirst().boons().get(Upgrade.INTELIGENCIA));
        assertTrue(service.unlockNecromancer());
        SharedProgress reloaded = store.load();
        assertEquals(1000, reloaded.souls()); assertTrue(reloaded.necromancerUnlocked());
        assertEquals(service.progress().history(), reloaded.history());
        assertEquals(3, reloaded.history().getFirst().boons().get(Upgrade.INTELIGENCIA));
    }
    @Test void invalidBalancesAndDuplicateHistoryAreRejected() throws Exception {
        assertThrows(IllegalArgumentException.class,()->SharedProgress.fromJson(new SharedProgress().toJson().replace("\"souls\": 0","\"souls\": -1")));
        SharedProgress progress=new SharedProgress(); RunSession run=run(1); RunReport report=run.report(RunOutcome.DEATH,0);
        progress.record(report);
        String json=progress.toJson(); String reportJson=report.toJson();
        String duplicate=new SharedProgress().toJson().replace("\"history\": []","\"history\": ["+reportJson+","+reportJson+"]");
        assertThrows(IllegalArgumentException.class,()->SharedProgress.fromJson(duplicate));
        assertEquals(progress.souls(),SharedProgress.fromJson(json).souls());
    }
}

