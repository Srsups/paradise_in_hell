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
    @Test void excessFastSpawnsAreReplacedWithoutShrinkingHordesOrRemovingGoals() {
        EnemyWaves waves=new EnemyWaves(new Random(){@Override public float nextFloat(){return .6f;}});
        for(ArenaVariation variation:ArenaVariation.values()) {
            var wave=waves.next(8,180,true,variation,0);
            assertEquals(8,wave.size()); assertEquals(2,wave.stream().filter(t->t==EnemyType.FAST).count());
            wave=waves.next(8,180,true,variation,EnemyWaves.MAX_FAST-1);
            assertEquals(8,wave.size()); assertEquals(1,wave.stream().filter(t->t==EnemyType.FAST).count());
            wave=waves.next(8,180,true,variation,EnemyWaves.MAX_FAST);
            assertEquals(8,wave.size()); assertFalse(wave.contains(EnemyType.FAST));
        }
        var demo=waves.next(8,90,false,ArenaVariation.BALANCED,EnemyWaves.MAX_FAST);
        assertTrue(demo.stream().allMatch(type->type==EnemyType.COMMON));
        assertThrows(IllegalArgumentException.class,()->waves.next(8,180,true,ArenaVariation.BALANCED,-1));
    }
    @Test void fastHuntsRemainVariedAndAllGoalsRemainAvailableAfterReplenishing() {
        EnemyWaves waves=new EnemyWaves(new Random(18)); EnumSet<EnemyType> types=EnumSet.noneOf(EnemyType.class);
        for(int i=0;i<1000;i++) {
            var wave=waves.next(8,300,true,ArenaVariation.FAST,0);
            assertTrue(wave.stream().filter(type->type==EnemyType.FAST).count()<=2);
            types.addAll(wave);
        }
        assertEquals(EnumSet.allOf(EnemyType.class),types);
    }
    @Test void archersKeepTheirOwnIntroductionChanceAndActiveLimits() {
        EnemyWaves waves = new EnemyWaves(new Random(42));
        for(int i=0;i<100;i++) {
            assertFalse(waves.archer(EnemyType.COMMON,14.9f,true,0,0));
            assertFalse(waves.archer(EnemyType.COMMON,59.9f,false,0,0));
            assertFalse(waves.archer(EnemyType.FAST,100,true,0,0));
            assertFalse(waves.archer(EnemyType.TANK,100,true,0,0));
            assertFalse(waves.archer(EnemyType.COMMON,100,true,EnemyWaves.MAX_ARCHERS,0));
            assertFalse(waves.archer(EnemyType.COMMON,100,true,0,EnemyWaves.MAX_ARCHERS_PER_WAVE));
        }
        int count=0;
        for(int i=0;i<10000;i++) if(waves.archer(EnemyType.COMMON,15,true,0,0)) count++;
        assertTrue(count>1800 && count<2200,"The chance applies to eligible common spawns");
    }
    @Test void preservesDemoIntroductionAndScalesXpWithThreatType() {
        EnemyWaves waves = new EnemyWaves(new Random(5));
        assertTrue(waves.next(10, 0, false).stream().allMatch(type -> type == EnemyType.COMMON));
        assertTrue(EnemyType.FAST.xp > EnemyType.COMMON.xp);
        assertTrue(EnemyType.TANK.xp > EnemyType.FAST.xp);
    }
}
