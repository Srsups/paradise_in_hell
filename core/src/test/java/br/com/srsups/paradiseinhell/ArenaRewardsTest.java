package br.com.srsups.paradiseinhell;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArenaRewardsTest {
    static RegionDefinition region() {
        return new RegionDefinition("r", "R", "start", List.of(
            new ArenaDefinition("start", "Start", "", ArenaDefinition.Kind.COMBAT, 0, Map.of(EnemyType.COMMON, 1), List.of("cinzas", "sombras", "boss")),
            new ArenaDefinition("cinzas", "Cinzas", "", ArenaDefinition.Kind.COMBAT, 60, Map.of(EnemyType.COMMON, 1), List.of("boss"), ArenaReward.RECOVERY_CHEST, 1.25f),
            new ArenaDefinition("sombras", "Sombras", "", ArenaDefinition.Kind.COMBAT, 120, Map.of(EnemyType.COMMON, 1), List.of("boss"), ArenaReward.RARE_BOON, 0.85f),
            new ArenaDefinition("boss", "Boss", "", ArenaDefinition.Kind.BOSS_ROOM, 140, Map.of(), List.of())));
    }
    static RunSession side(String id) {
        RunSession run = RunSession.campaign(region());
        run.recordKill(EnemyType.COMMON); run.enterArena(id); return run;
    }
    @Test void recoveryIsUnavailableBeforeGoalsClampsHealthAndCannotBeClaimedTwice() {
        RunSession run = side("cinzas"); run.player().sofrerDano(20);
        assertFalse(run.canClaimArenaReward());
        assertThrows(IllegalStateException.class, () -> run.claimRecoveryReward(true));
        run.recordKill(EnemyType.COMMON); assertTrue(run.canClaimArenaReward());
        assertThrows(IllegalStateException.class, () -> run.enterArena("boss"));
        assertEquals("+40 de vida", run.claimRecoveryReward(true));
        assertEquals(100, run.player().getVida()); assertFalse(run.canClaimArenaReward());
        assertThrows(IllegalStateException.class, () -> run.claimRecoveryReward(false));
        run.enterArena("boss"); assertFalse(run.canClaimArenaReward());
        assertFalse(run.newAttempt().canClaimArenaReward());
    }
    @Test void fullHealthConvertsRecoveryToCurrencyAndExplicitCurrencyWorksWhenHurt() {
        for (boolean full : new boolean[]{true, false}) {
            RunSession run = side("cinzas"); if (!full) run.player().sofrerDano(50);
            run.recordKill(EnemyType.COMMON);
            assertEquals("+5 óbolos ao retornar", run.claimRecoveryReward(full));
            assertEquals(5, run.earnedObols()); assertEquals(full ? 100 : 50, run.player().getVida());
            ProfileService service = new ProfileService(ProfileStore.memory());
            run.end(); service.settle(run, RunOutcome.DEATH); assertEquals(5, service.profile().obols());
        }
    }
    @Test void rareRewardIsCachedUniqueAndGrantsABoonWithoutChangingExperienceOrLevel() {
        RunSession run = side("sombras"); run.recordKill(EnemyType.COMMON);
        var options = run.arenaRewardChoices();
        assertEquals(3, options.size()); assertEquals(3, options.stream().distinct().count());
        assertSame(options, run.arenaRewardChoices());
        assertTrue(options.stream().allMatch(b -> b.rarity.ordinal() >= BoonRarity.RARE.ordinal()));
        assertThrows(IllegalArgumentException.class, () -> run.claimBoonReward(Upgrade.FORCA));
        assertTrue(run.canClaimArenaReward());
        run.claimBoonReward(options.get(0));
        assertEquals(1, run.rank(options.get(0))); assertEquals(1, run.player().getNivel()); assertEquals(0, run.player().getXpAtual());
        assertThrows(IllegalStateException.class, () -> run.claimBoonReward(options.get(1)));
        run.enterArena("boss");
    }
    @Test void exhaustedRareCategoriesConvertToCurrencyWithoutOfferingCommonBoons() {
        RunSession run = side("sombras");
        for (Upgrade boon : Upgrade.values()) if (boon.rarity != BoonRarity.COMMON)
            for (int i = 0; i < boon.maxRank; i++) run.applyUpgrade(boon);
        run.recordKill(EnemyType.COMMON);
        assertTrue(run.arenaRewardChoices().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> run.claimBoonReward(Upgrade.FORCA));
        run.claimBoonReward(null); assertEquals(8, run.earnedObols());
        assertEquals(0, run.rank(Upgrade.FORCA)); assertFalse(run.canClaimArenaReward());
    }
    @Test void minimumTierHonorsRankLimitsAndLuckForBothProfessionTypes() {
        for (Profession profession : new Profession[]{Profession.CACADOR, Profession.APOSTADOR}) {
            RunSession run = RunSession.demo(profession); run.applyUpgrade(Upgrade.RESSURREICAO); run.applyUpgrade(Upgrade.EGIDE);
            for (int i = 0; i < Upgrade.POSEIDON.maxRank; i++) run.applyUpgrade(Upgrade.POSEIDON);
            run.applyUpgrade(Upgrade.INTELIGENCIA);
            Random rng = new Random(55);
            for (int i = 0; i < 100; i++) {
                var options = UpgradeChoices.draw(run, 3, rng, BoonRarity.RARE);
                assertEquals(3, options.size()); assertTrue(options.stream().allMatch(b -> b.rarity == BoonRarity.RARE));
            }
        }
    }
    @Test void variationsStayFixedForUnvisitedAndVisitedRoomsAndKeepEveryGoalTypePossible() {
        RunSession run = RunSession.campaign(region());
        ArenaVariation forecast = run.variation("sombras");
        for (int i = 0; i < 100; i++) assertSame(forecast, run.variation("sombras"));
        run.recordKill(EnemyType.COMMON); run.enterArena("sombras");
        assertSame(forecast, run.variation()); assertEquals(0, run.progress().elapsed());
        for (ArenaVariation v : ArenaVariation.values()) {
            EnemyWaves waves = new EnemyWaves(new Random(45)); int[] count = new int[3]; int total = 0;
            // Single-member waves measure the variation's raw weights without the per-wave fast cap.
            for (int i = 0; i < 40000; i++) for (EnemyType t : waves.next(1, 300, true, v)) { count[t.ordinal()]++; total++; }
            float[] expected = switch (v) {
                case BALANCED -> new float[]{.50f, .35f, .15f};
                case FAST -> new float[]{.35f, .50f, .15f};
                case TANK -> new float[]{.40f, .25f, .35f};
            };
            for (int i = 0; i < 3; i++) assertEquals(expected[i], count[i] / (float) total, .025f);
            assertEquals(EnemyType.COMMON, new EnemyWaves(new Random(1)).next(1, 0, false, v).get(0));
        }
    }
    @Test void routeIntensityUsesContentAndInvalidRewardConfigurationIsRejected() {
        RunSession cinzas = side("cinzas"), sombras = side("sombras");
        assertEquals((1.7f - 60f / 210) * 1.25f, cinzas.spawnInterval(), .0001f);
        assertEquals((1.7f - 120f / 210) * .85f, sombras.spawnInterval(), .0001f);
        assertThrows(IllegalArgumentException.class, () -> new ArenaDefinition("a", "A", "", ArenaDefinition.Kind.COMBAT, 0,
            Map.of(EnemyType.COMMON, 1), List.of("b"), ArenaReward.NONE, 0));
        assertThrows(IllegalArgumentException.class, () -> new ArenaDefinition("b", "B", "", ArenaDefinition.Kind.BOSS_ROOM, 0,
            Map.of(), List.of(), ArenaReward.RARE_BOON, 1));
        assertEquals(ArenaReward.NONE, CampaignProfileTest.region().arena("fight").reward());
    }
}
