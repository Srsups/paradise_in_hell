package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeathVisualsTest {
    private static void add(DeathVisuals visuals,float x) {
        visuals.add("common",ActorAnimation.Direction.DOWN_LEFT,x,20,18);
    }
    @Test void manySimultaneousDeathsHaveABoundedCostAndEvictTheOldestVisual() {
        var visuals=new DeathVisuals();for(int i=0;i<100;i++)add(visuals,i);
        assertEquals(DeathVisuals.MAX_CORPSES,visuals.size());
        assertEquals(52,visuals.corpses().getFirst().x);assertEquals(99,visuals.corpses().getLast().x);
        assertThrows(UnsupportedOperationException.class,()->visuals.corpses().clear());
    }
    @Test void corpseFallsOnceThenFadesAndExpiresWithoutLeavingATargetBehind() {
        var visuals=new DeathVisuals();add(visuals,0);var corpse=visuals.corpses().getFirst();
        assertEquals(0,corpse.progress());assertEquals(1,corpse.opacity());
        visuals.advance(ActorReaction.DEATH_SECONDS);assertEquals(1,corpse.progress());assertEquals(1,corpse.opacity(),.0001f);
        visuals.advance(DeathVisuals.FADE_SECONDS/2);assertEquals(1,corpse.progress());assertEquals(.5f,corpse.opacity(),.0001f);
        visuals.advance(1);assertEquals(0,visuals.size());
    }
    @Test void pauseDoesNotAdvanceCorpseFramesAndLeavingARoomClearsAllVisuals() {
        var visuals=new DeathVisuals();add(visuals,1);visuals.advance(.15f);float age=visuals.corpses().getFirst().age();
        for(int i=0;i<60;i++)visuals.advance(0);
        assertEquals(age,visuals.corpses().getFirst().age());visuals.clear();assertEquals(0,visuals.size());
        assertThrows(IllegalArgumentException.class,()->visuals.advance(Float.NaN));
        assertThrows(IllegalArgumentException.class,()->visuals.advance(-1));
    }
}
