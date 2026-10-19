package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EquippedAnimationTest {
    private static void until(BossEncounter boss,BossEncounter.Attack attack,BossEncounter.State state){
        for(int i=0;i<3000;i++){
            if(boss.attack()==attack&&boss.state()==state)return;
            boss.update(.01f,400,70,false);
        }
        fail("Unreached boss state");
    }
    @Test void allFourDiagonalsAreRealFacingsAndTurningPreservesTheWalkCycle(){
        ActorAnimation.Direction[] expected={ActorAnimation.Direction.RIGHT,ActorAnimation.Direction.UP_RIGHT,
            ActorAnimation.Direction.UP,ActorAnimation.Direction.UP_LEFT,ActorAnimation.Direction.LEFT,
            ActorAnimation.Direction.DOWN_LEFT,ActorAnimation.Direction.DOWN,ActorAnimation.Direction.DOWN_RIGHT};
        ActorAnimation motion=new ActorAnimation();motion.advance(.23f,0,-1);float phase=motion.phase();
        for(int i=0;i<expected.length;i++){
            float angle=(float)Math.toRadians(i*45),dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            assertEquals(expected[i],ActorAnimation.facing(dx,dy));
            motion.advance(0,dx,dy);assertEquals(expected[i],motion.direction());assertEquals(phase,motion.phase());
        }
    }
    @Test void onlyRightViewsMirrorTheCorrespondingCanonicalLeftView(){
        for(var d:ActorAnimation.Direction.values()) assertFalse(d.authored().mirrored());
        assertEquals(ActorAnimation.Direction.LEFT,ActorAnimation.Direction.RIGHT.authored());
        assertEquals(ActorAnimation.Direction.DOWN_LEFT,ActorAnimation.Direction.DOWN_RIGHT.authored());
        assertEquals(ActorAnimation.Direction.UP_LEFT,ActorAnimation.Direction.UP_RIGHT.authored());
        assertNotEquals(ActorAnimation.Direction.LEFT,ActorAnimation.Direction.DOWN_LEFT.authored());
    }
    @Test void everyProfessionBreathesAndTurningPreservesTheSlowerWalkCycle(){
        for(Profession profession:Profession.values()){
            var animation=new PlayerAnimation(profession);var motion=new ActorAnimation();
            animation.select(motion,ActorAnimation.Direction.DOWN,false);
            assertEquals(EquippedArt.actor(profession)+"-idle",animation.current().clip());
            animation.advance(.41f);animation.select(motion,ActorAnimation.Direction.DOWN,false);
            assertEquals(1,animation.current().frame());assertFalse(animation.attacking());
            motion.advance(.28f,-1,-1);animation.select(motion,motion.direction(),false);
            assertEquals(EquippedArt.actor(profession)+"-walk",animation.current().clip());
            assertEquals(2,animation.current().frame());assertEquals(ActorAnimation.Direction.DOWN_LEFT,animation.current().direction());
            motion.advance(0,1,-1);animation.select(motion,motion.direction(),false);
            assertEquals(2,animation.current().frame(),"Turning preserves gait phase");
        }
    }
    @Test void spellsHaveTimeToReadAndReturningToIdleDoesNotFireAgain(){
        var animation=new PlayerAnimation(Profession.FEITICEIRA);var motion=new ActorAnimation();
        animation.attack(1);assertEquals(.48f,animation.attackDuration(),.0001f);
        animation.select(motion,ActorAnimation.Direction.DOWN_LEFT,false);
        assertEquals("feiticeira-attack",animation.current().clip());assertEquals(0,animation.current().frame());
        animation.advance(.25f);animation.select(motion,ActorAnimation.Direction.DOWN_LEFT,false);assertEquals(2,animation.current().frame());
        animation.advance(.24f);animation.select(motion,ActorAnimation.Direction.DOWN_LEFT,false);
        assertFalse(animation.attacking());assertEquals("feiticeira-idle",animation.current().clip());
        animation.attack(20);assertEquals(.20f,animation.attackDuration(),.0001f);
    }
    @Test void rapidTurnsRetainTheDominantPoseDuringCrossfade(){
        var animation=new PlayerAnimation(Profession.CACADOR);var motion=new ActorAnimation();
        animation.select(motion,ActorAnimation.Direction.LEFT,false);var start=animation.previous();
        animation.advance(.01f);animation.select(motion,ActorAnimation.Direction.DOWN_LEFT,false);
        assertEquals(start,animation.previous());assertEquals(0,animation.blend());
        animation.advance(.08f);assertEquals(animation.current(),animation.dominant());
    }
    @Test void angularHysteresisRejectsBoundaryNoiseInBothDirections(){
        ActorAnimation motion=new ActorAnimation();motion.advance(.1f,1,0);
        for(int i=0;i<100;i++)motion.advance(.01f,1,i%2==0?.40f:.48f);
        assertEquals(ActorAnimation.Direction.RIGHT,motion.direction());
        motion.advance(.01f,1,.60f);assertEquals(ActorAnimation.Direction.UP_RIGHT,motion.direction());
        motion.advance(.1f,1,.40f);assertEquals(ActorAnimation.Direction.UP_RIGHT,motion.direction());
        motion.advance(.01f,1,.28f);assertEquals(ActorAnimation.Direction.RIGHT,motion.direction());
    }
    @Test void breathingChangesFramesWithoutMovingTheBossOrAdvancingCombat(){
        BossEncounter boss=new BossEncounter();ActorAnimation motion=new ActorAnimation();
        var first=EncounterAnimation.boss(boss,motion);boss.update(.4f,400,70,false);
        var second=EncounterAnimation.boss(boss,motion);assertEquals("guardian-idle",first.clip());
        assertNotEquals(first.frame(),second.frame());assertEquals(240,boss.x());assertEquals(172,boss.y());
        float elapsed=boss.elapsed(),health=boss.health();
        for(int i=0;i<100;i++)EncounterAnimation.boss(boss,motion);
        assertEquals(elapsed,boss.elapsed());assertEquals(health,boss.health());
    }
    @Test void chargeRunsThenHoldsTheShoulderBashBeforeReturningToBreathing(){
        BossEncounter boss=new BossEncounter();ActorAnimation motion=new ActorAnimation();
        until(boss,BossEncounter.Attack.CHARGE,BossEncounter.State.TELEGRAPH);
        assertEquals(4,EncounterAnimation.boss(boss,motion).frame());
        until(boss,BossEncounter.Attack.CHARGE,BossEncounter.State.ACTIVE);
        assertTrue(EncounterAnimation.boss(boss,motion).frame()<4);
        until(boss,BossEncounter.Attack.CHARGE,BossEncounter.State.RECOVERY);
        assertEquals("guardian-charge",EncounterAnimation.boss(boss,motion).clip());
        assertEquals(5,EncounterAnimation.boss(boss,motion).frame());
        boss.update(.22f,400,70,false);
        assertEquals(BossEncounter.State.RECOVERY,boss.state());
        assertEquals("guardian-idle",EncounterAnimation.boss(boss,motion).clip());
    }
    @Test void leapUsesTakeoffAirborneAndLandingPosesWithoutCreatingAdditionalDamage(){
        BossEncounter boss=new BossEncounter();ActorAnimation motion=new ActorAnimation();
        until(boss,BossEncounter.Attack.LEAP,BossEncounter.State.TELEGRAPH);
        assertEquals("guardian-jump",EncounterAnimation.boss(boss,motion).clip());
        boss.update(.5f,400,70,false);assertFalse(boss.spriteVisible());assertFalse(boss.targetable());
        until(boss,BossEncounter.Attack.LEAP,BossEncounter.State.ACTIVE);
        assertEquals("guardian-land",EncounterAnimation.boss(boss,motion).clip());
        assertTrue(EncounterAnimation.boss(boss,motion).frame()>=1);
        int hits=boss.hits().size();float elapsed=boss.elapsed();
        for(int i=0;i<20;i++)EncounterAnimation.boss(boss,motion);
        assertEquals(hits,boss.hits().size());assertEquals(elapsed,boss.elapsed());
    }
}
