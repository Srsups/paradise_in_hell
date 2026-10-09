package br.com.srsups.paradiseinhell;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MaterialRegionTest {
    private static RegionDefinition region(String id) {
        return new RegionDefinition(id, id, "first", List.of(
            new ArenaDefinition("first", "First", "", ArenaDefinition.Kind.COMBAT, 0, Map.of(EnemyType.COMMON, 1), List.of("side", "boss")),
            new ArenaDefinition("side", "Side", "", ArenaDefinition.Kind.COMBAT, 60, Map.of(EnemyType.COMMON, 1), List.of("boss")),
            new ArenaDefinition("boss", "Boss", "", ArenaDefinition.Kind.BOSS_ROOM, 120, Map.of(), List.of())));
    }
    private static RunSession run(String id, float materialRoll) {
        return RunSession.campaign(region(id), Profession.CACADOR, PermanentBonuses.NONE, new ForgeProgressTest.Rolls(0, materialRoll));
    }
    @Test void failedDropsLeaveTheRegionAvailableAndThirdRunCanClaimIt() {
        ProfileService service = new ProfileService(ProfileStore.memory());
        for (int i = 0; i < 2; i++) {
            RunSession run = run("submundo", 0.9f); service.prepareRun(run); ForgeProgressTest.defeatMini(run);
            assertEquals(0, service.collectForgeMaterials(run)); assertTrue(service.profile().canClaimForgeMaterial("submundo"));
            run.end(); service.settle(run, RunOutcome.DEATH);
        }
        RunSession third = run("submundo", 0); service.prepareRun(third); ForgeProgressTest.defeatMini(third);
        assertEquals(1, service.collectForgeMaterials(third)); assertFalse(service.profile().canClaimForgeMaterial("submundo"));
        RunSession fourth = run("submundo", 0); service.prepareRun(fourth); ForgeProgressTest.defeatMini(fourth);
        assertEquals(0, service.collectForgeMaterials(fourth)); assertEquals(1, service.profile().forgeMaterials());
    }
    @Test void spendingDyingChangingRoomsAndReloadingDoNotRearmButAnotherRegionDoes(@TempDir Path folder) throws Exception {
        ProfileStore store = new FileProfileStore(folder.resolve("profile.json"));
        ProfileService service = new ProfileService(store); service.chooseProfession(Profession.CACADOR);
        RunSession first = run("submundo", 0); service.prepareRun(first); ForgeProgressTest.defeatMini(first);
        service.collectForgeMaterials(first); first.encounters().openBlacksmith(); assertTrue(service.forgeWeapon(first));
        first.recordKill(EnemyType.COMMON); first.enterArena("side");
        ForgeProgressTest.defeatMini(first); assertEquals(0, service.collectForgeMaterials(first));
        first.end(); service.settle(first, RunOutcome.DEATH);
        ProfileService loaded = new ProfileService(store);
        assertEquals(0, loaded.profile().forgeMaterials()); assertFalse(loaded.profile().canClaimForgeMaterial("submundo"));
        RunSession retry = run("submundo", 0); loaded.prepareRun(retry); ForgeProgressTest.defeatMini(retry);
        assertEquals(0, loaded.collectForgeMaterials(retry)); assertEquals(0, retry.encounters().materialChance());
        RunSession next = run("olimpo", 0); loaded.prepareRun(next); ForgeProgressTest.defeatMini(next);
        assertEquals(1, loaded.collectForgeMaterials(next)); assertFalse(loaded.profile().canClaimForgeMaterial("olimpo"));
        assertFalse(loaded.profile().canClaimForgeMaterial("submundo"));
    }
    @Test void repeatedCollectionAndEvenAnUnpreparedSessionCannotCreditTheSameRegionTwice() {
        ProfileService service = new ProfileService(ProfileStore.memory());
        RunSession first = run("r", 0); ForgeProgressTest.defeatMini(first);
        assertEquals(1, service.collectForgeMaterials(first)); assertEquals(0, service.collectForgeMaterials(first));
        RunSession stale = run("r", 0); ForgeProgressTest.defeatMini(stale);
        assertEquals(0, service.collectForgeMaterials(stale)); assertEquals(0, stale.pendingForgeMaterials());
        assertEquals(1, service.profile().forgeMaterials());
    }
    @Test void regionClaimsAreIndependentAcrossSavesAndPersistEvenWithNoMaterials() {
        SaveSlots slots = SaveSlots.memory(); ProfileService first = new ProfileService(slots.store(0));
        RunSession run = run("r", 0); ForgeProgressTest.defeatMini(run); first.collectForgeMaterials(run);
        run.encounters().openBlacksmith(); assertTrue(first.forgeWeapon(run));
        assertFalse(new ProfileService(slots.store(0)).profile().canClaimForgeMaterial("r"));
        assertTrue(new ProfileService(slots.store(3)).profile().canClaimForgeMaterial("r"));
    }
    @Test void olderMaterialSavesPreserveStockAndReserveTheOnlyRegionThenKnown() {
        String legacy = new CampaignProfile().toJson().replace("\"forgeMaterials\": 0", "\"forgeMaterials\": 2")
            .replaceAll("\\s*\\\"claimedMaterialRegions\\\": \\[\\],", "");
        CampaignProfile migrated = CampaignProfile.fromJson(legacy);
        assertEquals(2, migrated.forgeMaterials()); assertFalse(migrated.canClaimForgeMaterial(RegionDefinition.PROTOTYPE_ID));
        assertTrue(migrated.canClaimForgeMaterial("new-region"));
        CampaignProfile loaded = CampaignProfile.fromJson(migrated.toJson());
        assertEquals(2, loaded.forgeMaterials()); assertFalse(loaded.canClaimForgeMaterial(RegionDefinition.PROTOTYPE_ID));
    }
    @Test void malformedRegionLedgersAreRejected() {
        String json = new CampaignProfile().toJson();
        assertThrows(IllegalArgumentException.class, () -> CampaignProfile.fromJson(json.replace("\"claimedMaterialRegions\": []", "\"claimedMaterialRegions\": [7]")));
        assertThrows(IllegalArgumentException.class, () -> CampaignProfile.fromJson(json.replace("\"claimedMaterialRegions\": []", "\"claimedMaterialRegions\": [\"\"]")));
        assertThrows(IllegalArgumentException.class, () -> CampaignProfile.fromJson(json.replace("\"claimedMaterialRegions\": []", "\"claimedMaterialRegions\": [\"r\",\"r\"]")));
    }
}
