package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MiniBossEncounterTest {
    private static EnemyNavigation free(float x, float y) {
        EnemyNavigation nav = new EnemyNavigation((a,b) -> false); nav.update(0, x, y); return nav;
    }
    private static void until(MiniBossEncounter boss, EnemyNavigation nav, MiniBossEncounter.Attack attack, MiniBossEncounter.State state, float x, float y) {
        for (int i = 0; i < 1500; i++) {
            if (boss.attack() == attack && boss.state() == state) return;
            boss.update(0.01f, x, y, nav);
        }
        fail("Unreached mini-boss state");
    }
    @Test void normalPursuitStopsDuringWarningsAndAttacksWithNoDash() {
        MiniBossEncounter boss = new MiniBossEncounter(0, 0, EnemyDifficulty.BASE);
        EnemyNavigation nav = free(180, 8); boss.update(0.5f, 180, 8, nav);
        assertTrue(boss.x > 0 && boss.x <= 12.01f);
        until(boss, nav, MiniBossEncounter.Attack.SPIN, MiniBossEncounter.State.TELEGRAPH, 180, 8);
        float x = boss.x, y = boss.y;
        assertEquals(0, boss.update(0.4f, boss.centerX() + 30, boss.centerY(), nav));
        assertEquals(x, boss.x); assertEquals(y, boss.y);
        until(boss, nav, MiniBossEncounter.Attack.SPIN, MiniBossEncounter.State.ACTIVE, 180, 8);
        boss.update(0.2f, 180, 8, nav); assertEquals(x, boss.x); assertEquals(y, boss.y);
    }
    @Test void spinHitsOnceAndMovingOutsideTheCircleAvoidsDamage() {
        MiniBossEncounter boss = new MiniBossEncounter(0, 0, EnemyDifficulty.BASE);
        EnemyNavigation nav = free(200, 8);
        until(boss, nav, MiniBossEncounter.Attack.SPIN, MiniBossEncounter.State.ACTIVE, 200, 8);
        assertEquals(28, boss.update(0.02f, boss.centerX() + 30, boss.centerY(), nav));
        assertEquals(0, boss.update(0.1f, boss.centerX() + 30, boss.centerY(), nav));
        MiniBossEncounter dodge = new MiniBossEncounter(0, 0, EnemyDifficulty.BASE);
        until(dodge, nav, MiniBossEncounter.Attack.SPIN, MiniBossEncounter.State.ACTIVE, 200, 8);
        assertEquals(0, dodge.update(0.3f, dodge.centerX() + 80, dodge.centerY(), nav));
    }
    @Test void stonesAreAimedAtTheWarnedDirectionAndCollideAcrossTheirMovement() {
        MiniBossEncounter boss = new MiniBossEncounter(0, 0, EnemyDifficulty.BASE); EnemyNavigation nav = free(220, 8);
        until(boss, nav, MiniBossEncounter.Attack.STONES, MiniBossEncounter.State.TELEGRAPH, 220, 8);
        float angle = boss.aimAngle(); boss.update(0.2f, 8, 220, nav); assertEquals(angle, boss.aimAngle());
        until(boss, nav, MiniBossEncounter.Attack.STONES, MiniBossEncounter.State.ACTIVE, 220, 8);
        assertEquals(5, boss.stones().size());
        assertEquals(20, boss.update(0.3f, boss.centerX() + 45, boss.centerY(), nav));
    }
    @Test void structuresBlockSpinAndStones() {
        MiniBossEncounter boss = new MiniBossEncounter(0, 0, EnemyDifficulty.BASE);
        EnemyNavigation blocked = new EnemyNavigation((x,y) -> x >= 32 && x < 48); blocked.update(0, 72, 8);
        until(boss, blocked, MiniBossEncounter.Attack.SPIN, MiniBossEncounter.State.ACTIVE, 72, 8);
        assertEquals(0, boss.update(0.3f, boss.centerX() + 38, boss.centerY(), blocked));
        until(boss, blocked, MiniBossEncounter.Attack.STONES, MiniBossEncounter.State.ACTIVE, 72, 8);
        assertEquals(0, boss.update(1.3f, 72, 8, blocked)); assertTrue(boss.stones().isEmpty());
    }
    @Test void healthAndDamageUseTheRoomSnapshotAndDefeatClearsAllHazards() {
        MiniBossEncounter boss = new MiniBossEncounter(0, 0, new EnemyDifficulty(2, 1.5f)); EnemyNavigation nav = free(220, 8);
        assertEquals(360, boss.maxHealth()); boss.hurt(10); assertEquals(350, boss.health());
        until(boss, nav, MiniBossEncounter.Attack.SPIN, MiniBossEncounter.State.ACTIVE, 220, 8);
        assertEquals(42, boss.update(0.02f, boss.centerX() + 30, boss.centerY(), nav));
        until(boss, nav, MiniBossEncounter.Attack.STONES, MiniBossEncounter.State.ACTIVE, 220, 8);
        assertFalse(boss.stones().isEmpty()); boss.hurt(10000);
        assertTrue(boss.defeated()); assertTrue(boss.stones().isEmpty()); assertEquals(0, boss.update(10, 8, 8, nav));
    }
}
