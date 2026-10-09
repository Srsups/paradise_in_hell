package br.com.srsups.paradiseinhell;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossProgressTest {
    private static RunSession session() {
        var region = new RegionDefinition("r", "R", "fight", List.of(
            new ArenaDefinition("fight", "Fight", "", ArenaDefinition.Kind.COMBAT, 0, Map.of(EnemyType.COMMON, 1), List.of("boss")),
            new ArenaDefinition("boss", "Boss", "", ArenaDefinition.Kind.BOSS_ROOM, 0, Map.of(), List.of())));
        return RunSession.campaign(region, Profession.FERREIRO);
    }
    @Test void bossVictoryRequiresBossRoomCountsOnceAndResetsOnRetry() {
        RunSession run = session();
        assertThrows(IllegalStateException.class, run::recordBossDefeat);
        run.recordKill(EnemyType.COMMON); run.enterArena("boss");
        assertFalse(run.bossDefeated(), "Entering the room is not victory");
        int killsBefore = run.progress().kills();
        assertThrows(IllegalStateException.class, () -> run.recordKill(EnemyType.TANK));
        assertEquals(killsBefore, run.progress().kills(), "Rejected kills must not mutate statistics");
        run.recordBossDefeat();
        assertTrue(run.bossDefeated()); assertEquals(killsBefore + 1, run.progress().kills());
        assertEquals(2, run.completedArenas());
        assertThrows(IllegalStateException.class, run::recordBossDefeat);
        run.end();
        RunSession retry = run.newAttempt();
        assertFalse(retry.bossDefeated()); assertEquals(0, retry.progress().kills());
        assertEquals(Profession.FERREIRO, retry.player().getProfession());
    }
}
