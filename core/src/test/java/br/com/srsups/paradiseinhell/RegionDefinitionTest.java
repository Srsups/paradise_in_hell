package br.com.srsups.paradiseinhell;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegionDefinitionTest {
    private static ArenaDefinition combat(String id, String... exits) {
        return new ArenaDefinition(id, id, "", ArenaDefinition.Kind.COMBAT, 0, Map.of(EnemyType.COMMON, 1), List.of(exits));
    }
    private static ArenaDefinition boss(String id) {
        return new ArenaDefinition(id, id, "", ArenaDefinition.Kind.BOSS_ROOM, 0, Map.of(), List.of());
    }
    @Test void shippedGoalsUnlockDirectBossAccessAndOptionalCombatRoutes() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream(RegionDefinition.PROTOTYPE_ASSET)) {
            assertNotNull(input);
            RegionDefinition region = RegionDefinition.fromJson(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            assertEquals(Map.of(EnemyType.COMMON, 40, EnemyType.FAST, 30, EnemyType.TANK, 8), region.arena(region.start()).killGoals());
            assertEquals(3, region.arena(region.start()).exits().size());
            for (String branch : region.arena(region.start()).exits()) {
                RunSession run = RunSession.campaign(region);
                meetGoals(run); run.enterArena(branch);
                if (run.arena().kind() == ArenaDefinition.Kind.COMBAT) {
                    meetGoals(run);
                    if (run.arena().reward() == ArenaReward.RECOVERY_CHEST) run.claimRecoveryReward(false);
                    else if (run.arena().reward() == ArenaReward.RARE_BOON) run.claimBoonReward(run.arenaRewardChoices().get(0));
                    run.enterArena("portoes");
                }
                assertEquals(ArenaDefinition.Kind.BOSS_ROOM, run.arena().kind());
                assertFalse(run.progress().won(), "Access alone is not boss victory");
            }
        }
    }
    private static void meetGoals(RunSession run) {
        for (var goal : run.arena().killGoals().entrySet()) for (int i = 0; i < goal.getValue(); i++) run.recordKill(goal.getKey());
    }
    @Test void rejectsMissingDestinationsDuplicatesCyclesAndUnreachableArenas() {
        assertThrows(IllegalArgumentException.class, () -> new RegionDefinition("r", "R", "a", List.of(combat("a", "missing"))));
        assertThrows(IllegalArgumentException.class, () -> new RegionDefinition("r", "R", "a", List.of(combat("a", "b"), combat("a", "b"), boss("b"))));
        assertThrows(IllegalArgumentException.class, () -> new RegionDefinition("r", "R", "a", List.of(combat("a", "b"), combat("b", "a"))));
        assertThrows(IllegalArgumentException.class, () -> new RegionDefinition("r", "R", "a", List.of(boss("a"), boss("b"))));
    }
    @Test void rejectsInvalidGoalsKindsAndRepeatedExits() {
        assertThrows(IllegalArgumentException.class, () -> new ArenaDefinition("a", "A", "", ArenaDefinition.Kind.COMBAT, 0, Map.of(), List.of("b")));
        assertThrows(IllegalArgumentException.class, () -> new ArenaDefinition("a", "A", "", ArenaDefinition.Kind.COMBAT, 0, Map.of(EnemyType.FAST, 0), List.of("b")));
        assertThrows(IllegalArgumentException.class, () -> new ArenaDefinition("a", "A", "", ArenaDefinition.Kind.BOSS_ROOM, 0, Map.of(EnemyType.FAST, 1), List.of()));
        assertThrows(IllegalArgumentException.class, () -> combat("a", "b", "b"));
    }
}
