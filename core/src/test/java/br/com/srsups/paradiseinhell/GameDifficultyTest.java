package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GameDifficultyTest {
    private static RunSession run(GameDifficulty mode) {
        return RunSession.campaign(ArenaRewardsTest.region(), Profession.CACADOR, PermanentBonuses.NONE, mode,
            new ForgeProgressTest.Rolls(.5f, 0));
    }
    @Test void characterSettingsPersistTogetherAndStayFixedUntilReset(@TempDir Path folder) {
        SaveSlots slots = SaveSlots.files(folder.resolve("profile.json"));
        for (GameDifficulty mode : GameDifficulty.values()) {
            ProfileService service = new ProfileService(slots.store(mode.ordinal()));
            assertTrue(service.createCharacter(Profession.SACERDOTE, mode, false));
            CampaignProfile loaded = new ProfileService(slots.store(mode.ordinal())).profile();
            assertEquals(mode, loaded.difficulty()); assertEquals(Profession.SACERDOTE, loaded.profession());
            assertFalse(service.createCharacter(Profession.CACADOR, GameDifficulty.MEDIUM, true));
            assertThrows(IllegalStateException.class, () -> loaded.createCharacter(Profession.CACADOR, GameDifficulty.HARD));
            assertTrue(service.reset()); assertFalse(service.profile().hasProgress());
            assertTrue(service.createCharacter(Profession.GUERREIRO, GameDifficulty.HARD, false));
        }
    }
    @Test void missingDifficultyMigratesProfilesAndReportsWithoutChangingProgress() {
        CampaignProfile profile = new CampaignProfile();
        profile.createCharacter(Profession.APOSTADOR, GameDifficulty.HARD); profile.credit(72);
        profile.claimForgeMaterial("r"); profile.forgeWeapon();
        profile.finishConversation(CampaignProfile.Event.CARONTE_MET);
        var json = new JsonReader().parse(profile.toJson()); json.remove("difficulty");
        CampaignProfile legacy = CampaignProfile.fromJson(json.toString());
        assertEquals(GameDifficulty.MEDIUM, legacy.difficulty());
        assertEquals(77, legacy.obols()); assertEquals(WeaponRarity.RARE, legacy.weaponRarity());
        assertFalse(legacy.canClaimForgeMaterial("r")); assertEquals(profile.characterId(), legacy.characterId());
        RunSession run = run(GameDifficulty.HARD); run.recordKill(EnemyType.COMMON); run.end();
        RunReport report = run.report(RunOutcome.DEATH, 0);
        var old = report.json(); old.remove("difficulty");
        RunReport restored = RunReport.fromJson(old);
        assertEquals(GameDifficulty.MEDIUM, restored.difficulty()); assertEquals(report.souls(), restored.souls());
        assertEquals(report.id(), restored.id()); assertEquals(report.rooms(), restored.rooms());
    }
    @Test void malformedDifficultyIsRejectedRatherThanSilentlyDowngrading() {
        String profile = new CampaignProfile().toJson();
        for (String value : new String[]{"\"IMPOSSIBLE\"", "null", "1", "true"})
            assertThrows(RuntimeException.class, () -> CampaignProfile.fromJson(profile.replace("\"MEDIUM\"", value)));
        RunSession run = run(GameDifficulty.HARD); run.end();
        String report = run.report(RunOutcome.DEATH, 0).toJson();
        for (String value : new String[]{"\"IMPOSSIBLE\"", "null", "1", "true"})
            assertThrows(RuntimeException.class, () -> RunReport.fromJson(new JsonReader().parse(report.replace("\"HARD\"", value))));
        assertThrows(IllegalArgumentException.class, () -> new EnemyDifficulty(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new EnemyDifficulty(1, Float.NaN));
    }
    @Test void failedCreationAndBlockedLegendaryNeverLeaveAPartialCharacter() throws Exception {
        boolean[] fail = {true}; CampaignProfile[] disk = {new CampaignProfile()};
        ProfileService service = new ProfileService(new ProfileStore() {
            public CampaignProfile load() { return disk[0].copy(); }
            public void save(CampaignProfile profile) throws IOException {
                if (fail[0]) throw new IOException("test"); disk[0] = profile.copy();
            }
        });
        assertFalse(service.createCharacter(Profession.NECROMANTE, GameDifficulty.HARD, false));
        assertFalse(service.createCharacter(Profession.FERREIRO, GameDifficulty.HARD, false));
        assertFalse(service.profile().professionChosen()); assertFalse(disk[0].hasProgress());
        fail[0] = false;
        assertTrue(service.createCharacter(Profession.FERREIRO, GameDifficulty.HARD, false));
        assertEquals(GameDifficulty.HARD, disk[0].difficulty());
        assertEquals(Profession.FERREIRO, disk[0].profession()); assertTrue(service.error().isEmpty());
        ProfileService unreadable = new ProfileService(new ProfileStore() {
            public CampaignProfile load() throws IOException { throw new IOException("corrupt"); }
            public void save(CampaignProfile profile) { fail("Must preserve unreadable file"); }
        });
        assertFalse(unreadable.createCharacter(Profession.CACADOR, GameDifficulty.EASY, false));
        assertFalse(unreadable.profile().professionChosen());
    }
    @Test void modeComposesWithElapsedTimeRoomAndFuryButKeepsWaveAndXpRules() {
        for (GameDifficulty mode : GameDifficulty.values()) {
            RunSession run = run(mode);
            run.advance(120);
            assertEquals(1.12f * mode.healthMultiplier, run.enemyDifficulty().healthMultiplier(), .0001f);
            assertEquals(1.06f * mode.damageMultiplier, run.enemyDifficulty().damageMultiplier(), .0001f);
            assertEquals(3, run.waveSize()); assertEquals(1.7f - 120f / 210, run.spawnInterval(), .0001f);
            assertEquals(5, run.xpDrop(5));
            run.recordKill(EnemyType.COMMON); run.advance(40);
            assertEquals(1, run.fury().level());
            var baseline = EnemyDifficulty.at(160, 0);
            assertEquals(baseline.healthMultiplier() * mode.healthMultiplier * 1.2f, run.enemyDifficulty().healthMultiplier(), .0001f);
            assertEquals(baseline.damageMultiplier() * mode.damageMultiplier * 1.15f, run.enemyDifficulty().damageMultiplier(), .0001f);
            run.enterArena("cinzas");
            assertEquals(0, run.fury().level());
            assertEquals(EnemyDifficulty.at(160, 60).healthMultiplier() * mode.healthMultiplier, run.enemyDifficulty().healthMultiplier(), .0001f);
            assertEquals(mode, run.newAttempt().difficulty()); assertEquals(0, run.newAttempt().earnedSouls());
        }
    }
    @Test void soulsRetainFractionsAcrossEnemyTypesMinibossAndBossUntilTheRunEnds() {
        for (GameDifficulty mode : GameDifficulty.values()) {
            RunSession run = run(mode);
            run.advance(45); run.markMiniBossSpawned();
            run.recordKill(EnemyType.COMMON); assertEquals(mode.reward(1), run.earnedSouls());
            run.recordKill(EnemyType.FAST); assertEquals(mode.reward(3), run.earnedSouls());
            run.recordKill(EnemyType.TANK); run.recordKill(EnemyType.COMMON, ArcherAttack.SOULS);
            assertEquals(mode.reward(11), run.earnedSouls());
            assertTrue(run.recordMiniBossDefeat());
            assertEquals(mode.reward(31), run.earnedSouls());
            run.enterArena("boss"); run.recordBossDefeat();
            assertEquals(mode.reward(131), run.earnedSouls()); run.end();
            assertEquals(mode.reward(131), run.report(RunOutcome.VICTORY, 2).souls());
        }
    }
    @Test void repeatedSmallKillsDoNotRoundUpAndFuryStillSuppressesEligibleSoulRewards() {
        RunSession hard = run(GameDifficulty.HARD), easy = run(GameDifficulty.EASY);
        for (int i = 1; i <= 80; i++) {
            hard.recordKill(EnemyType.COMMON); easy.recordKill(EnemyType.COMMON);
            assertEquals(i * 125 / 100, hard.earnedSouls()); assertEquals(i * 85 / 100, easy.earnedSouls());
        }
        hard.advance(100); assertEquals(3, hard.fury().level());
        int base = hard.xpDrop(SoulRewards.enemy(EnemyType.TANK));
        hard.recordKill(EnemyType.TANK);
        assertEquals((80 + base) * 125 / 100, hard.earnedSouls());
    }
    @Test void chestAndKillCurrencyCombineBeforeRoundingAndBossBonusRequiresActualDefeat() {
        for (GameDifficulty mode : GameDifficulty.values()) {
            RunSession run = run(mode);
            assertEquals(mode.reward(3), run.previewChestObols(3));
            run.addChestObols(3);
            for (int i = 0; i < 10; i++) run.recordKill(EnemyType.COMMON);
            assertEquals(mode.reward(4), run.earnedObols());
            run.enterArena("boss");
            assertEquals(mode.reward(4), run.earnedObols());
            run.recordBossDefeat(); assertEquals(mode.reward(14), run.earnedObols());
            assertThrows(IllegalStateException.class, run::recordBossDefeat);
        }
    }
    @Test void routeCurrencyPreviewsAndMessagesMatchTheScaledAmount() {
        for (GameDifficulty mode : GameDifficulty.values()) {
            RunSession cinzas = run(mode);
            cinzas.addChestObols(3); cinzas.recordKill(EnemyType.COMMON); cinzas.enterArena("cinzas");
            cinzas.recordKill(EnemyType.COMMON);
            int before = cinzas.earnedObols(), increment = cinzas.previewChestObols(5);
            assertEquals("+" + increment + " óbolos ao retornar", cinzas.claimRecoveryReward(true));
            assertEquals(mode.reward(8), cinzas.earnedObols()); assertEquals(increment, cinzas.earnedObols() - before);
            RunSession sombras = run(mode); sombras.recordKill(EnemyType.COMMON); sombras.enterArena("sombras");
            for (Upgrade boon : Upgrade.values()) if (boon.rarity != BoonRarity.COMMON)
                for (int i = 0; i < boon.maxRank; i++) sombras.applyUpgrade(boon);
            sombras.recordKill(EnemyType.COMMON); sombras.claimBoonReward(null);
            assertEquals(mode.reward(8), sombras.earnedObols());
        }
    }
    @Test void settlementRetryReportsAndCaronteDoNotRescaleOrDuplicateBankedCurrency() throws Exception {
        for (GameDifficulty mode : GameDifficulty.values()) {
            ProfileStore store = ProfileStore.memory(); ProfileService personal = new ProfileService(store);
            personal.createCharacter(Profession.CACADOR, mode, false);
            personal.finishConversation(CampaignProfile.Event.CARONTE_MET);
            assertEquals(5, personal.profile().obols());
            SharedProgressService shared = new SharedProgressService(SharedProgressStore.memory());
            RunSession run = run(mode); personal.prepareRun(run);
            for (int i = 0; i < 40; i++) run.recordKill(EnemyType.COMMON);
            run.addChestObols(3); run.end();
            assertEquals(mode.reward(7), personal.settle(run, RunOutcome.ABANDONED));
            RunReport report = shared.settle(run, RunOutcome.ABANDONED, 0);
            personal.settle(run, RunOutcome.ABANDONED); shared.settle(run, RunOutcome.ABANDONED, 0);
            personal.retrySave(); shared.retrySave();
            assertEquals(5 + mode.reward(7), new ProfileService(store).profile().obols());
            assertEquals(mode.reward(40), shared.progress().souls());
            assertEquals(1, shared.progress().history().size());
            assertEquals(mode, report.difficulty());
            assertEquals(report, RunReport.fromJson(new JsonReader().parse(report.toJson())));
            assertEquals(5, PermanentAttribute.VITALITY.price(0));
            assertEquals(1000, SharedProgress.NECROMANCER_PRICE);
        }
    }
    @Test void demoScalesEnemiesWithoutCurrencyOrPermanentBonuses() {
        for (GameDifficulty mode : GameDifficulty.values()) {
            RunSession demo = RunSession.demo(Profession.GUERREIRO, mode);
            demo.advance(300); demo.recordKill(EnemyType.TANK);
            assertEquals(1.3f * mode.healthMultiplier, demo.enemyDifficulty().healthMultiplier(), .0001f);
            assertEquals(1.15f * mode.damageMultiplier, demo.enemyDifficulty().damageMultiplier(), .0001f);
            assertEquals(0, demo.earnedSouls()); assertEquals(0, demo.earnedObols());
            assertEquals(Profession.GUERREIRO.baseHealth, demo.player().getVidaMaxima());
            demo.end(); assertEquals(mode, demo.report(RunOutcome.VICTORY, 0).difficulty());
            assertEquals(mode, demo.newAttempt().difficulty());
        }
    }
}

