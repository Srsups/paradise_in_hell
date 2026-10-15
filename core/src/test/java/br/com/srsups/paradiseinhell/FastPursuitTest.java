package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FastPursuitTest {
    @Test void alternatesBurstsAndRecoveryAndKeepsBaseRewards() {
        assertEquals(70,FastPursuit.speed(.99f,0));
        assertEquals(24,FastPursuit.speed(1,0)); assertEquals(24,FastPursuit.speed(1.99f,0));
        assertEquals(70,FastPursuit.speed(2,0)); assertEquals(24,FastPursuit.speed(0,1.5f));
        assertEquals(2,EnemyType.FAST.xp); assertEquals(2,SoulRewards.enemy(EnemyType.FAST));
    }
    @Test void everyBaseProfessionCanOpenADistanceGapOnAStraightUnobstructedChase() {
        for(Profession profession:Profession.values()) for(float offset:new float[]{0,.2f,.7f,1.1f,1.8f}) {
            float gap=64, minimum=gap;
            for(int frame=0;frame<2000;frame++) {
                gap+=(profession.baseSpeed-FastPursuit.speed(frame*.01f,offset))*.01f;
                minimum=Math.min(minimum,gap);
            }
            assertTrue(minimum>16,profession+" must have a recovery window without Hermes on a straight chase");
            assertTrue(gap>64,profession+" base movement must exceed the long-term chase speed");
        }
    }
    @Test void offsetsAreStableAndDifferentSpawnsDoNotAllBurstTogether() {
        java.util.Set<Float> offsets=new java.util.HashSet<>();
        for(int i=0;i<40;i++) {
            float offset=FastPursuit.offset(i*7.3f,i*-4.7f);
            assertTrue(offset>=0 && offset<2); assertEquals(offset,FastPursuit.offset(i*7.3f,i*-4.7f));
            offsets.add(offset);
        }
        assertTrue(offsets.size()>20);
    }
}

