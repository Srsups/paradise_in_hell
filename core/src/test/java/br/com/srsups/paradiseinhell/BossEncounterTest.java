package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossEncounterTest {
    private static void until(BossEncounter boss, BossEncounter.Attack attack, BossEncounter.State state, float x, float y) {
        for (int frame = 0; frame < 3000; frame++) {
            if (boss.attack() == attack && boss.state() == state) return;
            boss.update(0.01f, x, y);
        }
        fail("Encounter did not reach " + attack + "/" + state);
    }
    @Test void marksWarnWithoutDamageLockToOldPositionAndResolveOnlyOnce() {
        BossEncounter boss = new BossEncounter();
        until(boss, BossEncounter.Attack.MARKS, BossEncounter.State.TELEGRAPH, 120, 80);
        assertEquals(1, boss.marks().size());
        assertEquals(120, boss.marks().get(0).x()); assertEquals(80, boss.marks().get(0).y());
        assertEquals(0, boss.update(0.6f, 120, 80));
        assertEquals(BossEncounter.State.TELEGRAPH, boss.state());
        until(boss, BossEncounter.Attack.MARKS, BossEncounter.State.ACTIVE, 120, 80);
        assertEquals(22, boss.update(0.04f, 120, 80));
        assertEquals(0, boss.update(0.1f, 120, 80));
        BossEncounter dodged = new BossEncounter();
        until(dodged, BossEncounter.Attack.MARKS, BossEncounter.State.TELEGRAPH, 120, 80);
        until(dodged, BossEncounter.Attack.MARKS, BossEncounter.State.ACTIVE, 240, 60);
        assertEquals(0, dodged.update(0.04f, 240, 60), "A warning must not follow the dodging player");
    }
    @Test void chargeUsesMarkedLaneStaysWithinRoomAndCanBeDodged() {
        BossEncounter boss = new BossEncounter();
        until(boss, BossEncounter.Attack.CHARGE, BossEncounter.State.TELEGRAPH, 120, 80);
        float expectedX = boss.endX(), expectedY = boss.endY(), initialX = boss.x(), initialY = boss.y();
        assertEquals(0, boss.update(0.6f, 420, 180));
        assertEquals(initialX, boss.x()); assertEquals(initialY, boss.y());
        until(boss, BossEncounter.Attack.CHARGE, BossEncounter.State.ACTIVE, 420, 180);
        float damage = 0;
        for (int frame = 0; frame < 300 && boss.state() == BossEncounter.State.ACTIVE; frame++) {
            damage += boss.update(0.01f, 420, 180);
            assertTrue(boss.x() >= BossEncounter.MIN_X + BossEncounter.RADIUS - 0.01f && boss.x() <= BossEncounter.MAX_X - BossEncounter.RADIUS + 0.01f);
            assertTrue(boss.y() >= BossEncounter.MIN_Y + BossEncounter.RADIUS - 0.01f && boss.y() <= BossEncounter.MAX_Y - BossEncounter.RADIUS + 0.01f);
        }
        assertEquals(0, damage); assertEquals(expectedX, boss.x(), 0.01f); assertEquals(expectedY, boss.y(), 0.01f);
        BossEncounter hit = new BossEncounter();
        until(hit, BossEncounter.Attack.CHARGE, BossEncounter.State.ACTIVE, 120, 80);
        float received = 0;
        for (int frame = 0; frame < 300 && hit.state() == BossEncounter.State.ACTIVE; frame++) received += hit.update(0.01f, 120, 80);
        assertEquals(18, received, "A charge should hit only once, even across many frames");
    }
    @Test void halfHealthIncreasesMarksAndVolleyAndBulletsExpire() {
        BossEncounter normal = new BossEncounter(), powered = new BossEncounter();
        powered.hurt(BossEncounter.MAX_HEALTH / 2);
        assertTrue(powered.enraged());
        until(powered, BossEncounter.Attack.MARKS, BossEncounter.State.TELEGRAPH, 120, 80);
        assertEquals(2, powered.marks().size());
        until(normal, BossEncounter.Attack.VOLLEY, BossEncounter.State.ACTIVE, 120, 80);
        until(powered, BossEncounter.Attack.VOLLEY, BossEncounter.State.ACTIVE, 120, 80);
        assertEquals(5, normal.shots().size()); assertEquals(9, powered.shots().size());
        for (int frame = 0; frame < 600; frame++) {
            normal.update(0.02f, 120, 80);
            assertTrue(normal.shots().size() <= 9, "No unbounded accumulation of projectiles");
        }
    }
    @Test void defeatClearsHazardsStopsAttacksAndDoesNotReviveBoss() {
        BossEncounter boss = new BossEncounter();
        until(boss, BossEncounter.Attack.VOLLEY, BossEncounter.State.ACTIVE, 120, 80);
        assertFalse(boss.shots().isEmpty());
        boss.hurt(BossEncounter.MAX_HEALTH);
        assertTrue(boss.defeated()); assertEquals(0, boss.health());
        assertTrue(boss.shots().isEmpty()); assertTrue(boss.marks().isEmpty());
        float stoppedAt = boss.elapsed();
        assertEquals(0, boss.update(10, boss.x(), boss.y()));
        assertEquals(stoppedAt, boss.elapsed());
        boss.hurt(10); assertEquals(0, boss.health());
    }
    @Test void rejectsInvalidTimeAndDamage() {
        BossEncounter boss = new BossEncounter();
        assertThrows(IllegalArgumentException.class, () -> boss.hurt(-1));
        assertThrows(IllegalArgumentException.class, () -> boss.hurt(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> boss.update(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> boss.update(Float.POSITIVE_INFINITY, 0, 0));
    }
}
