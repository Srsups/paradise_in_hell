package br.com.srsups.paradiseinhell;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RunSessionTest {
    private static RegionDefinition region() {
        return new RegionDefinition("test", "Test", "start", List.of(
            new ArenaDefinition("start", "Start", "", ArenaDefinition.Kind.COMBAT, 0,
                Map.of(EnemyType.COMMON, 2, EnemyType.FAST, 1, EnemyType.TANK, 1), List.of("short", "boss")),
            new ArenaDefinition("short", "Short", "", ArenaDefinition.Kind.COMBAT, 60,
                Map.of(EnemyType.COMMON, 1), List.of("boss")),
            new ArenaDefinition("boss", "Boss", "", ArenaDefinition.Kind.BOSS_ROOM, 0, Map.of(), List.of())));
    }
    private static void meetGoals(RunSession run) {
        for (var goal : run.arena().killGoals().entrySet()) for (int i = 0; i < goal.getValue(); i++) run.recordKill(goal.getKey());
    }
    @Test void allThreeGoalsMustBeMetAndTimeCannotUnlockTheBoss() {
        RunSession run = RunSession.campaign(region());
        run.advance(1000);
        assertFalse(run.arenaComplete());
        for (int i = 0; i < 100; i++) run.recordKill(EnemyType.COMMON);
        run.recordKill(EnemyType.FAST);
        assertFalse(run.arenaComplete(), "Extra common kills must not replace the tank goal");
        assertThrows(IllegalStateException.class, () -> run.enterArena("boss"));
        run.recordKill(EnemyType.TANK);
        assertTrue(run.arenaComplete());
        assertEquals(1, run.completedArenas());
        run.recordKill(EnemyType.TANK);
        assertEquals(1, run.completedArenas(), "Unlocking should be counted only once");
        run.advance(1);
        assertEquals(1001, run.progress().elapsed());
        assertFalse(run.progress().won());
    }
    @Test void transitionsKeepProgressUpgradesHealthAndCooldownsButResetLocalKillGoals() {
        RunSession run = RunSession.campaign(region(), Profession.VIGILANTE);
        PlayerState player = run.player();
        run.applyUpgrade(Upgrade.FORCA); run.applyUpgrade(Upgrade.POSEIDON); run.applyUpgrade(Upgrade.RESSURREICAO);
        player.ganharXP(10); player.subirDeNivel(); player.sofrerDano(30);
        run.advance(1); player.tryShoot(); player.tryDash();
        meetGoals(run); run.enterArena("short");
        assertSame(player, run.player());
        assertEquals(55, player.getVida()); assertEquals(2, player.getNivel());
        assertEquals(2.4f, player.getDano(), 0.0001f); assertEquals(Profession.VIGILANTE, player.getProfession());
        assertTrue(player.possuiAura()); assertTrue(player.possuiRessurreicao());
        assertEquals(1, run.rank(Upgrade.FORCA)); assertFalse(player.tryShoot());
        assertEquals(80, player.getEstaminaAtual());
        assertEquals(0, run.arenaElapsed()); assertEquals(1, run.progress().elapsed());
        assertEquals(0, run.kills(EnemyType.COMMON)); assertEquals(4, run.progress().kills());
        assertFalse(run.arenaComplete());
    }
    @Test void onlyUnlockedDeclaredRoutesCanBeUsed() {
        RunSession run = RunSession.campaign(region());
        assertThrows(IllegalStateException.class, () -> run.enterArena("short"));
        meetGoals(run);
        assertThrows(IllegalStateException.class, () -> run.enterArena("missing"));
        run.enterArena("boss");
        assertEquals(ArenaDefinition.Kind.BOSS_ROOM, run.arena().kind());
        assertFalse(run.goalsMet());
    }
    @Test void retryKeepsProfessionAndContentAndClearsEveryTemporaryAdvance() {
        RunSession previous = RunSession.campaign(region(), Profession.FERREIRO);
        previous.applyUpgrade(Upgrade.VIDA); previous.applyUpgrade(Upgrade.RESSURREICAO);
        previous.player().ganharXP(40); previous.advance(1);
        meetGoals(previous); previous.enterArena("short"); previous.end();
        assertThrows(IllegalStateException.class, () -> previous.advance(1));
        RunSession retry = previous.newAttempt();
        assertSame(previous.region(), retry.region()); assertNotSame(previous.player(), retry.player());
        assertEquals(Profession.FERREIRO, retry.player().getProfession());
        assertEquals(WeaponType.SWORD, retry.player().getWeapon());
        assertEquals("start", retry.arena().id()); assertEquals(130, retry.player().getVidaMaxima());
        assertEquals(1, retry.player().getNivel()); assertEquals(0, retry.player().getXpAtual());
        assertEquals(0, retry.progress().kills()); assertEquals(0, retry.progress().elapsed());
        assertEquals(0, retry.rank(Upgrade.VIDA)); assertFalse(retry.player().possuiRessurreicao());
    }
    @Test void consumedResurrectionAndProtectionSurviveTransition() {
        RunSession run = RunSession.campaign(region());
        run.applyUpgrade(Upgrade.RESSURREICAO); run.player().usarRessurreicao();
        meetGoals(run); run.enterArena("short");
        assertFalse(run.player().possuiRessurreicao());
        assertThrows(IllegalStateException.class, () -> run.applyUpgrade(Upgrade.RESSURREICAO));
        run.player().sofrerDano(500); assertFalse(run.player().estaMorto());
        run.player().advance(2); run.player().sofrerDano(500); assertTrue(run.player().estaMorto());
    }
}
