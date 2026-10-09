package br.com.srsups.paradiseinhell;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnemyDifficultyTest {
    @Test void healthAndDamageGrowLinearlyWithBothTimeAndRoomWithoutTheWaveTimeCap() {
        EnemyDifficulty start = EnemyDifficulty.at(0, 0), ten = EnemyDifficulty.at(600, 0), later = EnemyDifficulty.at(600, 60);
        assertEquals(1, start.healthMultiplier()); assertEquals(1.6f, ten.healthMultiplier(), 0.0001f);
        assertEquals(1.3f, ten.damageMultiplier(), 0.0001f);
        assertEquals(1.72f, later.healthMultiplier(), 0.0001f); assertEquals(1.36f, later.damageMultiplier(), 0.0001f);
        assertEquals(ten.healthMultiplier() - start.healthMultiplier(),
            EnemyDifficulty.at(1200, 0).healthMultiplier() - ten.healthMultiplier(), 0.0001f);
        assertTrue(EnemyDifficulty.at(3600, 0).healthMultiplier() > EnemyDifficulty.at(300, 0).healthMultiplier());
        assertThrows(IllegalArgumentException.class, () -> EnemyDifficulty.at(-1, 0));
    }
    @Test void movingToAnotherRoomKeepsTotalRunTimeForScaling() {
        var region = new RegionDefinition("r", "R", "first", List.of(
            new ArenaDefinition("first", "First", "", ArenaDefinition.Kind.COMBAT, 0, Map.of(EnemyType.COMMON, 1), List.of("second")),
            new ArenaDefinition("second", "Second", "", ArenaDefinition.Kind.COMBAT, 60, Map.of(EnemyType.COMMON, 1), List.of("boss")),
            new ArenaDefinition("boss", "Boss", "", ArenaDefinition.Kind.BOSS_ROOM, 120, Map.of(), List.of())));
        RunSession session = RunSession.campaign(region); session.advance(600); session.recordKill(EnemyType.COMMON);
        session.enterArena("second");
        assertEquals(0, session.arenaElapsed()); assertEquals(600, session.progress().elapsed());
        assertEquals(1.72f, session.enemyDifficulty().healthMultiplier(), 0.0001f);
    }
    @Test void bossUsesScaledHealthPhaseThresholdAndAttackDamage() {
        BossEncounter boss = new BossEncounter(EnemyDifficulty.at(600, 60));
        assertEquals(420 * 1.72f, boss.maxHealth(), 0.0001f);
        boss.hurt(boss.maxHealth() / 2); assertTrue(boss.enraged());
        for (int frame = 0; frame < 300 && boss.state() != BossEncounter.State.ACTIVE; frame++) boss.update(0.01f, 120, 80);
        assertEquals(28 * 1.36f, boss.update(0.02f, 120, 80), 0.0001f);
    }
}
