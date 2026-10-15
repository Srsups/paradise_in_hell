package br.com.srsups.paradiseinhell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceBalanceTest {
    private static RegionDefinition prototype() throws IOException {
        try (var input = ExperienceBalanceTest.class.getResourceAsStream("/" + RegionDefinition.PROTOTYPE_ASSET)) {
            assertNotNull(input);
            return RegionDefinition.fromJson(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private record Result(int level, int otherChoices) {}

    /** Collect every goal enemy's crystal and resolve levels as they arrive; no farming or extra rewards. */
    private static Result playGoals(RegionDefinition region, float bonus, int acquiredAt, String side) {
        RunSession run = RunSession.campaign(region);
        boolean acquired = false;
        String[] rooms = side == null ? new String[]{region.start()} : new String[]{region.start(), side};
        for (String room : rooms) {
            if (!room.equals(region.start())) run.enterArena(room);
            for (EnemyType type : EnemyType.values()) {
                for (int i = 0; i < run.arena().killGoals().getOrDefault(type, 0); i++) {
                    run.recordKill(type);
                    run.player().ganharXP(type.xp);
                    while (run.player().podeSubirDeNivel()) {
                        run.player().subirDeNivel();
                        if (bonus > 0 && !acquired && run.player().getNivel() == acquiredAt) {
                            if (bonus == Upgrade.ATHENA_XP_BONUS) run.applyUpgrade(Upgrade.INTELIGENCIA);
                            else run.player().aumentarInteligencia(bonus); // Compare the former 20% rule.
                            acquired = true;
                        }
                    }
                }
            }
        }
        return new Result(run.player().getNivel(), run.player().getNivel() - 1 - (acquired ? 1 : 0));
    }

    @Test void earlyAthenaRepaysItsChoiceWithOneMoreCombatBoonOnAllCurrentRoutes() throws IOException {
        RegionDefinition region = prototype();
        // XP budgets: Estige 140, Estige + Cinzas 216, Estige + Sombras 280.
        for (String side : new String[]{null, "cinzas", "sombras"}) {
            Result normal = playGoals(region, 0, 2, side);
            Result old = playGoals(region, .2f, 2, side);
            for (int acquiredAt : new int[]{2, 3}) {
                Result athena = playGoals(region, Upgrade.ATHENA_XP_BONUS, acquiredAt, side);
                assertTrue(athena.otherChoices() >= normal.otherChoices() + 1, "Early Athena must repay the spent choice on " + side);
            }
            if (side == null) {
                assertEquals(new Result(6, 5), normal);
                assertEquals(new Result(6, 4), old);
                assertEquals(new Result(8, 6), playGoals(region, Upgrade.ATHENA_XP_BONUS, 2, null));
            }
        }
    }

    @Test void lateSelectionIsAnInvestmentAndDoesNotMultiplyBankedXp() throws IOException {
        RegionDefinition region = prototype();
        assertEquals(new Result(6, 4), playGoals(region, Upgrade.ATHENA_XP_BONUS, 6, null));
        RunSession run = RunSession.demo();
        run.player().ganharXP(140);
        run.applyUpgrade(Upgrade.INTELIGENCIA);
        assertEquals(140, run.player().getXpAtual());
        while (run.player().podeSubirDeNivel()) run.player().subirDeNivel();
        assertEquals(6, run.player().getNivel());
        assertEquals(11, run.player().getXpAtual());
        run.player().ganharXP(1);
        assertEquals(13.5f, run.player().getXpAtual());
    }

    @Test void fractionalCrystalRewardsSurviveLevelUpsForEveryProfession() {
        for (Profession profession : Profession.values()) {
            RunSession run = RunSession.demo(profession);
            run.applyUpgrade(Upgrade.INTELIGENCIA);
            for (int reward : new int[]{EnemyType.COMMON.xp, EnemyType.FAST.xp, EnemyType.TANK.xp,
                    ArcherAttack.XP, MiniBossEncounter.XP}) {
                float before = run.player().getXpAtual();
                run.player().ganharXP(reward);
                assertEquals(reward * 2.5f, run.player().getXpAtual() - before);
            }
            // 1 + 2 + 5 + 3 + 20 = 31 base XP -> 77.5 XP: level 4, 30.5 banked.
            while (run.player().podeSubirDeNivel()) run.player().subirDeNivel();
            assertEquals(4, run.player().getNivel());
            assertEquals(30.5f, run.player().getXpAtual());
        }
    }

    @Test void athenaIsUniqueAndOnlyResetsOnANewAttempt() throws IOException {
        RunSession run = RunSession.campaign(prototype(), Profession.APOSTADOR);
        run.applyUpgrade(Upgrade.INTELIGENCIA);
        assertEquals(BoonRarity.EPIC, Upgrade.INTELIGENCIA.rarity);
        assertEquals(1, Upgrade.INTELIGENCIA.maxRank);
        assertThrows(IllegalStateException.class, () -> run.applyUpgrade(Upgrade.INTELIGENCIA));
        assertEquals(2.5f, run.player().xpGain(1));
        for (var goal : run.arena().killGoals().entrySet())
            for (int i = 0; i < goal.getValue(); i++) run.recordKill(goal.getKey());
        run.enterArena("cinzas");
        assertEquals(2.5f, run.player().xpGain(1));
        Random random = new Random(11);
        for (int i = 0; i < 500; i++) assertFalse(UpgradeChoices.draw(run, 3, random).contains(Upgrade.INTELIGENCIA));
        run.end();
        RunSession next = run.newAttempt();
        assertEquals(0, next.rank(Upgrade.INTELIGENCIA));
        assertEquals(1, next.player().xpGain(1));
        assertEquals(0, next.player().getXpAtual());
    }

    @Test void xpBoonPreservesFuryPenaltiesAndDoesNotMultiplyPersistentCurrencies() throws IOException {
        RunSession normal = RunSession.campaign(prototype()), athena = RunSession.campaign(prototype());
        athena.applyUpgrade(Upgrade.INTELIGENCIA);
        for (RunSession run : new RunSession[]{normal, athena}) {
            for (var goal : run.arena().killGoals().entrySet())
                for (int i = 0; i < goal.getValue(); i++) run.recordKill(goal.getKey());
            run.addChestObols(3);
            run.advance(600); // Farming activates the existing XP/drop penalty.
        }
        assertTrue(athena.fury().level() > 0);
        assertEquals(normal.fury().level(), athena.fury().level());
        assertEquals(normal.earnedSouls(), athena.earnedSouls());
        assertEquals(normal.earnedObols(), athena.earnedObols());
        int reduced = athena.xpDrop(MiniBossEncounter.XP);
        assertTrue(reduced < MiniBossEncounter.XP);
        assertEquals(normal.xpDrop(MiniBossEncounter.XP), reduced);
        athena.player().ganharXP(reduced);
        assertEquals(reduced * 2.5f, athena.player().getXpAtual());
    }

    @Test void firstOptionIsRarerForNormalClassesAndStillRespondsToLuck() {
        int draws = 30000, standard = 0, lucky = 0;
        RunSession base = RunSession.demo(), boosted = RunSession.demo(Profession.APOSTADOR);
        Random a = new Random(381), b = new Random(381);
        for (int i = 0; i < draws; i++) {
            if (UpgradeChoices.draw(base, 1, a).getFirst() == Upgrade.INTELIGENCIA) standard++;
            if (UpgradeChoices.draw(boosted, 1, b).getFirst() == Upgrade.INTELIGENCIA) lucky++;
        }
        // One of three Epic effects: 6% / 3 = 2%; lucky tier weight = 96 / 322.
        assertEquals(.02f, standard / (float) draws, .004f);
        assertEquals(96f / 322 / 3, lucky / (float) draws, .009f);
        assertTrue(standard / (float) draws < .23f / 7); // Former Rare pool had seven effects.
        assertTrue(lucky > standard * 3);
        assertTrue(Upgrade.INTELIGENCIA.description.contains("+150%"));
        assertTrue(Upgrade.INTELIGENCIA.description.contains("2,5x"));
    }
}

