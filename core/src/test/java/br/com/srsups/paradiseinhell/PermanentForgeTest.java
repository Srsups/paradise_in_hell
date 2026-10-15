package br.com.srsups.paradiseinhell;

import java.nio.file.Path;
import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PermanentForgeTest {
    @Test void allProfessionsKeepForgedStatsAfterDeathAbandonVictoryAndDiskReload(@TempDir Path folder) throws Exception {
        for (Profession profession : Profession.values()) {
            ProfileStore store = new FileProfileStore(folder.resolve(profession.name() + ".json"));
            ProfileService service = new ProfileService(store); service.chooseProfession(profession);
            service.finishConversation(CampaignProfile.Event.CARONTE_MET); assertTrue(service.buy(PermanentAttribute.STRENGTH));
            RunSession run = RunSession.campaign(CampaignProfileTest.region(), profession, service.profile().bonuses(), new ForgeProgressTest.Rolls(0, 0));
            service.prepareRun(run); ForgeProgressTest.defeatMini(run); service.collectForgeMaterials(run);
            run.encounters().openBlacksmith(); assertTrue(service.forgeWeapon(run));
            run.applyUpgrade(Upgrade.FORCA); run.applyUpgrade(Upgrade.POSEIDON);
            run.end(); service.settle(run, RunOutcome.DEATH);
            ProfileService loaded = new ProfileService(store);
            for (RunOutcome outcome : new RunOutcome[]{RunOutcome.ABANDONED, RunOutcome.VICTORY}) {
                RunSession retry = RunSession.campaign(CampaignProfileTest.region(), profession, loaded.profile().bonuses());
                loaded.prepareRun(retry); loaded.prepareRun(retry);
                assertEquals(WeaponRarity.RARE, retry.player().getWeaponRarity());
                assertEquals(profession.weapon.damage * 1.25f * 1.05f, retry.player().getDano(), 0.0001f);
                assertEquals(profession.weapon.interval / 1.10f, retry.player().getAttackInterval(), 0.0001f);
                assertEquals(0, retry.rank(Upgrade.FORCA)); assertFalse(retry.player().possuiAura());
                assertEquals(1, retry.player().getNivel()); assertEquals(profession.baseHealth, retry.player().getVida());
                if (outcome == RunOutcome.VICTORY) { retry.recordKill(EnemyType.COMMON); retry.enterArena("boss"); retry.recordBossDefeat(); }
                retry.end(); loaded.settle(retry, outcome);
                loaded = new ProfileService(store);
                assertEquals(WeaponRarity.RARE, loaded.profile().weaponRarity());
            }
        }
    }
    @Test void failedCommitKeepsBothStockAndPermanentRarityUnchanged() throws Exception {
        ProfileStore memory = ProfileStore.memory(); final boolean[] fail = {false};
        ProfileStore flaky = new ProfileStore() {
            public CampaignProfile load() throws java.io.IOException { return memory.load(); }
            public void save(CampaignProfile p) throws java.io.IOException {
                if (fail[0]) throw new java.io.IOException("test"); memory.save(p);
            }
        };
        ProfileService service = new ProfileService(flaky); RunSession run = ForgeProgressTest.run(0, 0);
        ForgeProgressTest.defeatMini(run); service.collectForgeMaterials(run); run.encounters().openBlacksmith();
        fail[0] = true; assertFalse(service.forgeWeapon(run));
        assertEquals(1, service.profile().forgeMaterials()); assertEquals(WeaponRarity.COMMON, service.profile().weaponRarity());
        assertEquals(WeaponRarity.COMMON, memory.load().weaponRarity()); assertEquals(1, memory.load().forgeMaterials());
        fail[0] = false; assertTrue(service.forgeWeapon(run));
        assertEquals(WeaponRarity.RARE, memory.load().weaponRarity()); assertEquals(0, memory.load().forgeMaterials());
        service.profile().claimForgeMaterial("another-region"); service.retrySave();
        RunSession stale = ForgeProgressTest.run(0); stale.encounters().openBlacksmith();
        assertFalse(service.forgeWeapon(stale)); assertEquals(1, service.profile().forgeMaterials());
    }
    @Test void legacySpentFragmentsRestoreThePermanentUpgradeWithoutConsumingStock() {
        CampaignProfile p = new CampaignProfile(); p.claimForgeMaterial("submundo"); p.claimForgeMaterial("olimpo"); p.spendForgeMaterial();
        var json = new JsonReader().parse(p.toJson()); json.remove("weaponRarity");
        CampaignProfile migrated = CampaignProfile.fromJson(json.toString());
        assertEquals(WeaponRarity.RARE, migrated.weaponRarity()); assertEquals(1, migrated.forgeMaterials());
        assertEquals(WeaponRarity.RARE, migrated.copy().weaponRarity());
        CampaignProfile untouched = new CampaignProfile(); untouched.claimForgeMaterial("r");
        var unspent = new JsonReader().parse(untouched.toJson()); unspent.remove("weaponRarity");
        assertEquals(WeaponRarity.COMMON, CampaignProfile.fromJson(unspent.toString()).weaponRarity());
        var empty = new JsonReader().parse(new CampaignProfile().toJson()); empty.remove("weaponRarity");
        assertEquals(WeaponRarity.COMMON, CampaignProfile.fromJson(empty.toString()).weaponRarity());
    }
    @Test void saveSlotsRemainIndependentAndInvalidRarityFieldsAreRejected() throws Exception {
        SaveSlots slots = SaveSlots.memory(); ProfileService first = new ProfileService(slots.store(0));
        RunSession run = ForgeProgressTest.run(0, 0); ForgeProgressTest.defeatMini(run); first.collectForgeMaterials(run);
        run.encounters().openBlacksmith(); assertTrue(first.forgeWeapon(run));
        assertEquals(WeaponRarity.COMMON, new ProfileService(slots.store(3)).profile().weaponRarity());
        String json = new CampaignProfile().toJson();
        assertThrows(IllegalArgumentException.class, () -> CampaignProfile.fromJson(json.replace("\"weaponRarity\": \"COMMON\"", "\"weaponRarity\": \"UNKNOWN\"")));
        assertThrows(IllegalArgumentException.class, () -> CampaignProfile.fromJson(json.replace("\"weaponRarity\": \"COMMON\"", "\"weaponRarity\": 7")));
        RunSession demo = RunSession.demo(); first.prepareRun(demo);
        assertEquals(WeaponRarity.COMMON, demo.player().getWeaponRarity());
    }
}
