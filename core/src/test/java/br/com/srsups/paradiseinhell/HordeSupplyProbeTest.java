package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HordeSupplyProbeTest {
    @Test void seedsReproduceActualSuppliesAndAllThreeGoalsAreSatisfied() {
        RegionDefinition region = HordeSupplyProbe.region();
        for (ArenaDefinition arena : region.arenas()) {
            if (arena.kind() != ArenaDefinition.Kind.COMBAT) continue;
            for (ArenaVariation variation : ArenaVariation.values()) {
                HordeSupplyProbe.Sample a = HordeSupplyProbe.sample(region, arena.id(), variation, 17, GameDifficulty.MEDIUM);
                assertEquals(a, HordeSupplyProbe.sample(region, arena.id(), variation, 17, GameDifficulty.MEDIUM));
                assertTrue(a.common() >= arena.killGoals().get(EnemyType.COMMON));
                assertTrue(a.fast() >= arena.killGoals().get(EnemyType.FAST));
                assertTrue(a.tank() >= arena.killGoals().get(EnemyType.TANK));
                assertEquals(a.seconds(), Math.max(a.commonGoalSeconds(), Math.max(a.fastGoalSeconds(), a.tankGoalSeconds())));
                assertTrue(a.archers()+a.flyers() <= a.common());
                assertEquals((a.common() - a.archers()-a.flyers()) * EnemyType.COMMON.xp + a.archers() * ArcherAttack.XP+a.flyers()*StygianFlyer.XP
                    + a.fast() * EnemyType.FAST.xp + a.tank() * EnemyType.TANK.xp, a.baseXp());
                assertEquals((a.common() - a.archers()-a.flyers()) * SoulRewards.enemy(EnemyType.COMMON) + a.archers() * ArcherAttack.SOULS+a.flyers()*StygianFlyer.SOULS
                    + a.fast() * SoulRewards.enemy(EnemyType.FAST) + a.tank() * SoulRewards.enemy(EnemyType.TANK), a.baseSouls());
            }
        }
    }

    @Test void difficultyCannotChangeInstantKillSupply() {
        RegionDefinition region = HordeSupplyProbe.region();
        for (ArenaDefinition arena : region.arenas()) {
            if (arena.kind() != ArenaDefinition.Kind.COMBAT) continue;
            for (ArenaVariation variation : ArenaVariation.values()) {
                HordeSupplyProbe.Sample medium = HordeSupplyProbe.sample(region, arena.id(), variation, 99, GameDifficulty.MEDIUM);
                assertEquals(medium, HordeSupplyProbe.sample(region, arena.id(), variation, 99, GameDifficulty.EASY));
                assertEquals(medium, HordeSupplyProbe.sample(region, arena.id(), variation, 99, GameDifficulty.HARD));
            }
        }
    }

    @Test void bossArenaIsExplicitlyOutsideTheSupplyProbe() {
        assertThrows(IllegalArgumentException.class, () -> HordeSupplyProbe.sample(HordeSupplyProbe.region(), "portoes", ArenaVariation.BALANCED, 0, GameDifficulty.MEDIUM));
        assertTrue(HordeSupplyProbe.notes().contains("não tempos esperados de conclusão"));
    }
}
