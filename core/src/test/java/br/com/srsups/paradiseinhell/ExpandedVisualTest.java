package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpandedVisualTest {
    @Test void turningBlendsBeforeChangingTheDominantContactFrameAndKeepsGaitPhase() {
        ActorAnimation motion=new ActorAnimation();motion.advance(.19f,0,-1);
        float phase=motion.phase();motion.advance(0,1,0);
        assertEquals(phase,motion.phase());assertEquals(0,motion.turnBlend());
        assertEquals(ActorAnimation.Direction.DOWN,motion.dominantDirection());
        motion.advance(.04f,1,0);assertEquals(.4f,motion.turnBlend(),.001f);
        assertEquals(ActorAnimation.Direction.DOWN,motion.dominantDirection());
        motion.advance(.02f,1,0);assertEquals(ActorAnimation.Direction.RIGHT,motion.dominantDirection());
        motion.advance(.2f,0,0);assertEquals(1,motion.turnBlend());assertEquals(ActorAnimation.Direction.RIGHT,motion.direction());
    }
    @Test void rapidReversalsStartFromTheVisibleFacingRatherThanAQueuedInvisibleDirection() {
        ActorAnimation motion=new ActorAnimation();motion.advance(.1f,0,-1);motion.advance(0,1,0);
        motion.advance(.02f,1,0);motion.advance(0,0,1);
        assertEquals(ActorAnimation.Direction.DOWN,motion.previousDirection());
        assertEquals(ActorAnimation.Direction.UP,motion.direction());assertEquals(0,motion.turnBlend());
        motion.advance(.11f,0,1);assertEquals(ActorAnimation.Direction.UP,motion.dominantDirection());
    }
    @Test void sixFrameWalkCoversTheFullCycleAndStopsWithoutLosingTheLastFacing() {
        ActorAnimation motion=new ActorAnimation();java.util.Set<Integer> seen=new java.util.HashSet<>();
        for(int i=0;i<60;i++){motion.advance(.01f,-1,0);seen.add(motion.frame(6));}
        assertEquals(java.util.Set.of(0,1,2,3,4,5),seen);
        float phase=motion.phase();motion.advance(10,0,0);
        assertEquals(phase,motion.phase());assertEquals(1,motion.frame(6));assertEquals(ActorAnimation.Direction.LEFT,motion.direction());
    }
    @Test void openedChestAnimationsAreBoundedExpireAndDoNotAlterTheRewardModel() {
        ChestVisuals visuals=new ChestVisuals();
        for(int i=0;i<10;i++)visuals.open(new RunChests.Chest(i,0,RunChests.Reward.HEALTH));
        assertEquals(6,visuals.active());visuals.advance(.4f);assertEquals(6,visuals.active());
        visuals.advance(.5f);assertEquals(0,visuals.active());
        visuals.open(new RunChests.Chest(0,0,RunChests.Reward.OBOLS));visuals.clear();assertEquals(0,visuals.active());
    }
}

