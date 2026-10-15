package br.com.srsups.paradiseinhell;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RunChestsTest {
    private static EnemyNavigation openMap() {
        EnemyNavigation navigation = new EnemyNavigation((x, y) -> false);
        navigation.update(0, 8, 8); return navigation;
    }
    private static RunChests.Chest spawn(RunChests chests, EnemyNavigation navigation, boolean campaign) {
        for (int i = 0; i < 100 && chests.chests().isEmpty(); i++) chests.update(40, 8, 8, navigation, campaign);
        assertFalse(chests.chests().isEmpty()); return chests.chests().get(0);
    }
    @Test void suppliesSpawnOnlyOnReachableGroundAndStayBoundedDuringLongSessions() {
        RunChests chests = new RunChests(new Random(42)); EnemyNavigation navigation = openMap();
        chests.update(0, 8, 8, navigation, true); assertTrue(chests.chests().isEmpty());
        for (int i = 0; i < 1000; i++) {
            chests.update(40, 8, 8, navigation, true);
            assertTrue(chests.chests().size() <= 3);
            for (RunChests.Chest chest : chests.chests()) {
                assertTrue(navigation.reachable(chest.x(), chest.y()));
                assertTrue(navigation.canOccupy(chest.x(), chest.y()));
            }
        }
        assertEquals(3, chests.chests().size());
        EnemyNavigation closed = new EnemyNavigation((x, y) -> true); closed.update(0, 8, 8);
        RunChests blocked = new RunChests(new Random(1));
        for (int i = 0; i < 100; i++) blocked.update(40, 8, 8, closed, true);
        assertTrue(blocked.chests().isEmpty());
    }
    @Test void openingEachRewardAppliesOnceAndDemoNeverSpawnsPersistentCurrency() {
        java.util.EnumSet<RunChests.Reward> seen = java.util.EnumSet.noneOf(RunChests.Reward.class);
        for (int seed = 0; seed < 300 && seen.size() < 4; seed++) {
            RunChests chests = new RunChests(new Random(seed));
            RunChests.Chest chest = spawn(chests, openMap(), true);
            RunSession run = RunSession.campaign(CampaignProfileTest.region());
            run.player().sofrerDano(50); run.player().tryDash();
            String reward = chests.open(chest, run); seen.add(chest.reward());
            switch (chest.reward()) {
                case HEALTH -> assertEquals(80, run.player().getVida());
                case EXPERIENCE -> assertEquals(12, run.player().getXpAtual());
                case STAMINA -> assertEquals(100, run.player().getEstaminaAtual());
                case OBOLS -> assertEquals(3, run.earnedObols());
            }
            assertFalse(reward.isEmpty()); assertEquals("", chests.open(chest, run)); assertTrue(chests.chests().isEmpty());
        }
        assertEquals(4, seen.size());
        for (int seed = 0; seed < 50; seed++) {
            RunChests chests = new RunChests(new Random(seed));
            assertNotEquals(RunChests.Reward.OBOLS, spawn(chests, openMap(), false).reward());
        }
    }
    @Test void xpAndConvertedRewardsShowTheActualAthenaBonusAndCannotBeClaimedTwice() {
        var seen = java.util.EnumSet.noneOf(RunChests.Reward.class);
        for (int seed = 0; seed < 300 && seen.size() < 3; seed++) {
            RunChests chests = new RunChests(new Random(seed));
            RunChests.Chest chest = spawn(chests, openMap(), true);
            if (chest.reward() == RunChests.Reward.OBOLS) continue;
            RunSession run = RunSession.campaign(CampaignProfileTest.region());
            run.applyUpgrade(Upgrade.INTELIGENCIA);
            assertEquals("+30 XP", chests.open(chest, run));
            assertEquals(30, run.player().getXpAtual());
            assertEquals("", chests.open(chest, run));
            assertEquals(30, run.player().getXpAtual());
            seen.add(chest.reward());
        }
        assertEquals(3, seen.size(), "XP and both full-status conversions must receive the boon");
    }
    @Test void fullHealthOrStaminaConvertsToXpAndDistantChestsAreEvicted() {
        boolean checked = false;
        for (int seed = 0; seed < 100 && !checked; seed++) {
            RunChests chests = new RunChests(new Random(seed));
            RunChests.Chest chest = spawn(chests, openMap(), true);
            if (chest.reward() == RunChests.Reward.HEALTH || chest.reward() == RunChests.Reward.STAMINA) {
                RunSession run = RunSession.campaign(CampaignProfileTest.region());
                assertEquals("+12 XP", chests.open(chest, run)); assertEquals(12, run.player().getXpAtual()); checked = true;
            }
        }
        assertTrue(checked);
        RunChests chests = new RunChests(new Random(0)); spawn(chests, openMap(), true);
        chests.update(0, 1000, 1000, openMap(), true); assertTrue(chests.chests().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> chests.update(-1, 8, 8, openMap(), true));
    }
}
