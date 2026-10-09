package br.com.srsups.paradiseinhell;

import java.util.HashSet;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LuckAndBoonTest {
    @Test void luckyProfessionBoostsBothRareDropsAndBlacksmithWithoutRemovingCaps() {
        assertEquals(6f / 100, Luck.chance(0.015f, Profession.APOSTADOR.luckMultiplier), 0.000001f);
        assertEquals(12f / 100, Luck.chance(0.03f, Profession.APOSTADOR.luckMultiplier), 0.000001f);
        RunEncounters standard = new RunEncounters(new ForgeProgressTest.Rolls(0.10f, 0.05f), 1);
        RunEncounters lucky = new RunEncounters(new ForgeProgressTest.Rolls(0.10f, 0.05f), 4);
        assertFalse(standard.openBlacksmith()); assertTrue(lucky.openBlacksmith());
        standard.markMiniBossSpawned(); lucky.markMiniBossSpawned();
        assertFalse(standard.defeatMiniBoss()); assertTrue(lucky.defeatMiniBoss());
        lucky.enterRoom(); lucky.markMiniBossSpawned(); assertFalse(lucky.defeatMiniBoss());
        assertEquals(0, lucky.materialChance()); assertEquals(1, Luck.chance(0.5f, 4));
        assertThrows(IllegalArgumentException.class, () -> Luck.chance(0.1f, -1));
    }
    @Test void tiersReflectPowerWithoutChangingRepeatedRankLimitsOrEffects() {
        assertEquals(BoonRarity.COMMON, Upgrade.FORCA.rarity);
        assertEquals(BoonRarity.RARE, Upgrade.MAGNETISMO.rarity);
        assertEquals(BoonRarity.EPIC, Upgrade.POSEIDON.rarity);
        assertEquals(BoonRarity.MYTHOLOGICAL, Upgrade.RESSURREICAO.rarity);
        RunSession session = RunSession.demo(Profession.APOSTADOR); session.applyUpgrade(Upgrade.FORCA);
        assertEquals(4.8f, session.player().getDano(), 0.0001f);
        assertEquals(1, session.rank(Upgrade.FORCA)); assertEquals(6, Upgrade.FORCA.maxRank);
    }
    @Test void choicesAreDistinctRespectCapsAndExcludePointlessHealingForBothLuckLevels() {
        for (Profession profession : new Profession[]{Profession.CACADOR, Profession.APOSTADOR}) {
            RunSession session = RunSession.demo(profession);
            session.applyUpgrade(Upgrade.EGIDE); session.applyUpgrade(Upgrade.RESSURREICAO);
            for (int i = 0; i < Upgrade.POSEIDON.maxRank; i++) session.applyUpgrade(Upgrade.POSEIDON);
            Random rng = new Random(27);
            for (int i = 0; i < 500; i++) {
                var choices = UpgradeChoices.draw(session, 3, rng);
                assertEquals(3, choices.size()); assertEquals(3, new HashSet<>(choices).size());
                assertFalse(choices.contains(Upgrade.EGIDE)); assertFalse(choices.contains(Upgrade.RESSURREICAO));
                assertFalse(choices.contains(Upgrade.POSEIDON)); assertFalse(choices.contains(Upgrade.CURA));
            }
            assertEquals(0, session.progress().elapsed());
        }
    }
    @Test void seededSamplingFollowsBaseDistributionAndLuckyClassFavorsTheStrongestTiers() {
        RunSession base = RunSession.demo(Profession.CACADOR), lucky = RunSession.demo(Profession.APOSTADOR);
        int[] normal = new int[4], boosted = new int[4];
        Random a = new Random(381), b = new Random(381); int draws = 30000;
        for (int i = 0; i < draws; i++) {
            normal[UpgradeChoices.draw(base, 1, a).get(0).rarity.ordinal()]++;
            boosted[UpgradeChoices.draw(lucky, 1, b).get(0).rarity.ordinal()]++;
        }
        float[] expected = {0.70f, 0.23f, 0.06f, 0.01f};
        for (int i = 0; i < 4; i++) assertEquals(expected[i], normal[i] / (float) draws, 0.009f);
        assertTrue(boosted[3] > normal[3] * 10); assertTrue(boosted[2] > normal[2] * 3);
        assertTrue(boosted[0] < normal[0] / 2);
    }
    @Test void fistsHaveShortRangeAndForgingStillUsesPunchesRatherThanProjectiles() {
        PlayerState fists = new PlayerState(Profession.APOSTADOR);
        assertEquals(WeaponType.FISTS, fists.getWeapon()); assertTrue(fists.getWeapon().isMelee());
        assertEquals(110, fists.getVida()); assertEquals(24, fists.meleeReach());
        assertTrue(WeaponAttack.meleeHits(0, 0, 1, 0, 23, 0, fists.meleeReach(), 40));
        assertFalse(WeaponAttack.meleeHits(0, 0, 1, 0, 25, 0, fists.meleeReach(), 40));
        assertFalse(WeaponAttack.meleeHits(0, 0, 1, 0, -10, 0, fists.meleeReach(), 40));
        fists.forgeWeapon(); assertEquals(34, fists.meleeReach()); assertEquals(5, fists.getDano());
        assertEquals(WeaponType.FISTS, fists.getWeapon()); assertEquals(4, fists.getLuckMultiplier());
    }
}
