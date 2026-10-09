package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ForgeProgressTest {
    static final class Rolls extends Random {
        private final float[] values; private int index;
        Rolls(float... values) { this.values = values; }
        @Override public float nextFloat() { return index < values.length ? values[index++] : 0.5f; }
    }
    static RunSession run(float... rolls) {
        return RunSession.campaign(CampaignProfileTest.region(), Profession.CACADOR, PermanentBonuses.NONE, new Rolls(rolls));
    }
    static void defeatMini(RunSession run) {
        run.advance(45); assertTrue(run.miniBossDue()); run.markMiniBossSpawned(); run.recordMiniBossDefeat();
    }
    @Test void probabilitiesUseStrictBoundariesAndBlacksmithIsDrawnOnlyOncePerAttempt() {
        RunEncounters below = new RunEncounters(new Rolls(0.02999f, 0.01499f));
        assertTrue(below.openBlacksmith()); below.closeBlacksmith();
        assertFalse(below.openBlacksmith()); below.enterRoom(); assertFalse(below.openBlacksmith());
        below.markMiniBossSpawned(); assertTrue(below.defeatMiniBoss());
        assertThrows(IllegalStateException.class, below::defeatMiniBoss);
        RunEncounters edge = new RunEncounters(new Rolls(0.03f, 0.015f));
        assertFalse(edge.openBlacksmith()); edge.markMiniBossSpawned(); assertFalse(edge.defeatMiniBoss());
    }
    @Test void materialCapSurvivesRoomChangesInsideTheSameRegion() {
        RunSession run = RunSession.campaign(EnemyDifficultyTestRegion.region(), Profession.CACADOR, PermanentBonuses.NONE,
            new Rolls(0.5f, 0, 0));
        defeatMini(run); assertEquals(1, run.pendingForgeMaterials());
        assertFalse(run.miniBossDue()); assertThrows(IllegalStateException.class, run::recordMiniBossDefeat);
        assertEquals(0, run.kills(EnemyType.COMMON), "Mini-boss must not substitute room goals");
        run.recordKill(EnemyType.COMMON); run.enterArena("second");
        defeatMini(run); assertEquals(1, run.pendingForgeMaterials());
        assertEquals(3, run.progress().kills());
    }
    @Test void materialIsSavedImmediatelyAndSurvivesDeathReloadAndOtherSaves(@TempDir Path folder) throws Exception {
        Path file = folder.resolve("profile.json"); SaveSlots slots = SaveSlots.files(file);
        ProfileService service = new ProfileService(slots.store(0)); service.chooseProfession(Profession.CACADOR);
        RunSession run = run(0.5f, 0); defeatMini(run);
        assertEquals(1, service.collectForgeMaterials(run)); assertEquals(0, service.collectForgeMaterials(run));
        assertEquals(1, new ProfileService(slots.store(0)).profile().forgeMaterials());
        run.end(); service.settle(run, RunOutcome.DEATH);
        assertEquals(1, new ProfileService(slots.store(0)).profile().forgeMaterials());
        assertEquals(0, new ProfileService(slots.store(1)).profile().forgeMaterials());
        assertTrue(Files.readString(file).contains("forgeMaterials"));
    }
    @Test void existingVersionOneSavesLoadWithZeroMaterialsAndInvalidMaterialsAreRejected() {
        String old = new CampaignProfile().toJson().replaceAll("\\s*\\\"forgeMaterials\\\": 0,", "");
        assertEquals(0, CampaignProfile.fromJson(old).forgeMaterials());
        String json = new CampaignProfile().toJson();
        assertThrows(IllegalArgumentException.class, () -> CampaignProfile.fromJson(json.replace("\"forgeMaterials\": 0", "\"forgeMaterials\": -1")));
        assertThrows(IllegalArgumentException.class, () -> CampaignProfile.fromJson(json.replace("\"forgeMaterials\": 0", "\"forgeMaterials\": 1.5")));
    }
    @Test void forgeConsumesOneMaterialOnlyAfterSavingAndWeaponResetsOnNextRun() {
        ProfileStore store = ProfileStore.memory(); ProfileService service = new ProfileService(store);
        RunSession run = run(0, 0); defeatMini(run); service.collectForgeMaterials(run);
        assertFalse(service.forgeWeapon(run), "The encounter must be open");
        assertTrue(run.encounters().openBlacksmith());
        assertTrue(service.forgeWeapon(run)); assertFalse(service.forgeWeapon(run));
        assertEquals(0, new ProfileService(store).profile().forgeMaterials());
        assertEquals(WeaponRarity.RARE, run.player().getWeaponRarity());
        assertEquals(WeaponRarity.COMMON, run.newAttempt().player().getWeaponRarity());
    }
    @Test void saveFailurePreservesMaterialAndWeaponWhileDropAutosaveCanBeRetried() {
        ProfileStore memory = ProfileStore.memory();
        final boolean[] fail = {true};
        ProfileStore flaky = new ProfileStore() {
            public CampaignProfile load() throws IOException { return memory.load(); }
            public void save(CampaignProfile profile) throws IOException {
                if (fail[0]) throw new IOException("test");
                memory.save(profile);
            }
        };
        ProfileService service = new ProfileService(flaky); RunSession run = run(0, 0);
        defeatMini(run); assertEquals(1, service.collectForgeMaterials(run)); assertEquals(1, service.profile().forgeMaterials());
        run.encounters().openBlacksmith(); assertFalse(service.forgeWeapon(run));
        assertEquals(1, service.profile().forgeMaterials()); assertEquals(WeaponRarity.COMMON, run.player().getWeaponRarity());
        fail[0] = false; assertTrue(service.retrySave());
        assertEquals(1, new ProfileService(memory).profile().forgeMaterials()); assertTrue(service.forgeWeapon(run));
        assertEquals(0, service.profile().forgeMaterials());
    }
    @Test void demoCannotSpawnAwardMaterialOrForgeAndDefeatedRunCannotForge() {
        RunSession demo = RunSession.demo(); demo.advance(60); assertFalse(demo.miniBossDue());
        assertThrows(IllegalStateException.class, demo::markMiniBossSpawned);
        assertThrows(IllegalStateException.class, demo::recordMiniBossDefeat);
        RunSession run = run(0, 0); defeatMini(run); run.encounters().openBlacksmith(); run.end();
        assertFalse(run.canForgeWeapon());
    }
    private static class EnemyDifficultyTestRegion {
        static RegionDefinition region() {
            return new RegionDefinition("r", "R", "first", java.util.List.of(
                new ArenaDefinition("first", "First", "", ArenaDefinition.Kind.COMBAT, 0, java.util.Map.of(EnemyType.COMMON, 1), java.util.List.of("second")),
                new ArenaDefinition("second", "Second", "", ArenaDefinition.Kind.COMBAT, 60, java.util.Map.of(EnemyType.COMMON, 1), java.util.List.of("boss")),
                new ArenaDefinition("boss", "Boss", "", ArenaDefinition.Kind.BOSS_ROOM, 120, java.util.Map.of(), java.util.List.of())));
        }
    }
}
