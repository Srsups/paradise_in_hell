package br.com.srsups.paradiseinhell;

import java.util.EnumSet;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnemyWavesTest {
    @Test void generatesMixedAndSingleTypeHordesContinuouslyIncludingAllRequiredTypes() {
        EnemyWaves waves = new EnemyWaves(new Random(42));
        EnumSet<EnemyType> types = EnumSet.noneOf(EnemyType.class);
        boolean grouped = false, mixed = false;
        int generated = 0;
        for (int wave = 0; wave < 1000; wave++) {
            var next = waves.next(6, 0, true);
            assertEquals(6, next.size()); generated += next.size(); types.addAll(next);
            if (next.stream().distinct().count() == 1) grouped = true; else mixed = true;
        }
        assertEquals(6000, generated); assertEquals(EnumSet.allOf(EnemyType.class), types);
        assertTrue(grouped); assertTrue(mixed);
    }
    @Test void preservesDemoIntroductionAndScalesXpWithThreatType() {
        EnemyWaves waves = new EnemyWaves(new Random(5));
        assertTrue(waves.next(10, 0, false).stream().allMatch(type -> type == EnemyType.COMMON));
        assertTrue(EnemyType.FAST.xp > EnemyType.COMMON.xp);
        assertTrue(EnemyType.TANK.xp > EnemyType.FAST.xp);
    }
}
