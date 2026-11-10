package br.com.srsups.paradiseinhell;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossPressureTest {
    private static void until(BossEncounter boss,BossEncounter.Attack attack,BossEncounter.State state) {
        for(int i=0;i<10000;i++) {
            if(boss.attack()==attack && boss.state()==state)return;
            boss.update(.01f,120,80,false);
        }
        fail("Unreachable pattern "+attack+"/"+state);
    }
    private static List<BossEncounter.Attack> patterns(long seed,int count) {
        BossEncounter boss=new BossEncounter(EnemyDifficulty.BASE,new Random(seed));
        List<BossEncounter.Attack> result=new ArrayList<>();BossEncounter.State before=boss.state();
        for(int i=0;i<30000 && result.size()<count;i++) {
            boss.update(.01f,120,80,false);
            if(before!=BossEncounter.State.TELEGRAPH && boss.state()==BossEncounter.State.TELEGRAPH)result.add(boss.attack());
            before=boss.state();
        }
        assertEquals(count,result.size());return result;
    }
    @Test void introductoryCycleTeachesFourAttacksAndLaterCyclesAreFairVariedAndSeedable() {
        for(long seed=0;seed<16;seed++) {
            List<BossEncounter.Attack> attacks=patterns(seed,24);
            assertEquals(List.of(BossEncounter.Attack.values()),attacks.subList(0,4));
            for(int i=0;i<24;i+=4)assertEquals(4,new HashSet<>(attacks.subList(i,i+4)).size());
            for(int i=1;i<24;i++)assertNotEquals(attacks.get(i-1),attacks.get(i));
            assertEquals(attacks,patterns(seed,24));
        }
        assertNotEquals(patterns(1,24),patterns(2,24));
    }
    @Test void phaseTwoRecoveryIsShorterAndWarningsStillProvideReactionTime() {
        for(boolean enraged:List.of(false,true)) {
            BossEncounter boss=new BossEncounter();if(enraged)boss.hurt(boss.health()/2);
            until(boss,BossEncounter.Attack.MARKS,BossEncounter.State.TELEGRAPH);
            float warningStart=boss.elapsed();
            until(boss,BossEncounter.Attack.MARKS,BossEncounter.State.ACTIVE);
            assertEquals(enraged?.75f:.95f,boss.elapsed()-warningStart,.025f);
            until(boss,BossEncounter.Attack.MARKS,BossEncounter.State.RECOVERY);
            float recoveryStart=boss.elapsed();
            until(boss,BossEncounter.Attack.CHARGE,BossEncounter.State.TELEGRAPH);
            assertEquals(enraged?BossEncounter.ENRAGED_RECOVERY_SECONDS:BossEncounter.RECOVERY_SECONDS,boss.elapsed()-recoveryStart,.025f);
        }
    }
    @Test void oldVolleyKeepsItsDamageSnapshotAfterTheHealthThresholdChanges() {
        BossEncounter boss=new BossEncounter();
        until(boss,BossEncounter.Attack.VOLLEY,BossEncounter.State.ACTIVE);
        var shot=boss.shots().get(2);float x=shot.x,y=shot.y;
        boss.hurt(boss.health()/2);assertTrue(boss.enraged());
        assertEquals(12,boss.update(.01f,x,y,false));
        BossEncounter powered=new BossEncounter();powered.hurt(powered.health()/2);
        until(powered,BossEncounter.Attack.VOLLEY,BossEncounter.State.ACTIVE);
        shot=powered.shots().get(BossEncounter.MAX_SHOTS/2);
        assertEquals(14,powered.update(.01f,shot.x,shot.y,false));
    }
    @Test void currentPatternsStayBoundedAndLeapRemovesOtherHazardsAcrossManyCycles() {
        BossEncounter boss=new BossEncounter(EnemyDifficulty.BASE,new Random(12));boss.hurt(boss.health()/2);
        for(int i=0;i<15000;i++) {
            boss.update(.01f,120,80,false);
            assertTrue(boss.shots().size()<=BossEncounter.MAX_SHOTS);
            if(boss.airborne())assertTrue(boss.shots().isEmpty());
        }
    }
    @Test void phaseMeasurementsPersistAndOlderReportsRemainReadable() {
        RunSession run=RunSession.campaign(CampaignProfileTest.region());
        run.recordKill(EnemyType.COMMON);run.enterArena("boss");
        BossEncounter boss=new BossEncounter(EnemyDifficulty.BASE,new Random(1));run.statistics().observeBoss(boss);
        for(int i=0;i<200;i++){run.advance(.01f);boss.update(.01f,120,80,false);}
        boss.hurt(boss.health()/2);
        for(int i=0;i<500;i++){run.advance(.01f);boss.update(.01f,120,80,false);}
        run.end();RunReport report=run.report(RunOutcome.ABANDONED,0);
        var fight=report.rooms().getLast().bossFight();
        assertNotNull(fight);assertEquals(2,fight.phaseOneSeconds(),.001f);assertEquals(5,fight.phaseTwoSeconds(),.001f);
        assertEquals(7,boss.elapsed(),.001f);assertTrue(fight.attacks().get(BossEncounter.Attack.MARKS)>0);
        assertThrows(UnsupportedOperationException.class,()->fight.attacks().clear());
        SharedProgress progress=new SharedProgress();progress.record(report);
        assertEquals(report,SharedProgress.fromJson(progress.toJson()).history().getFirst());
        var json=report.json();json.get("rooms").get(1).remove("bossFight");
        assertNull(RunReport.fromJson(json).rooms().getLast().bossFight());
        boss.update(2,120,80,false);assertEquals(5,fight.phaseTwoSeconds(),.001f);
        assertThrows(IllegalArgumentException.class,()->new BossFightSummary(-1,0,Map.of()));
        assertThrows(IllegalArgumentException.class,()->new BossFightSummary(0,0,Map.of(BossEncounter.Attack.LEAP,0)));
    }
    @Test void denserPhaseTwoMarksRemainLockedAndEveryProfessionCanTakeTheOpenEscapeRoute() {
        for(int fps:new int[]{60,120})for(Profession profession:Profession.values())for(boolean phaseTwo:List.of(false,true)) {
            BossEncounter boss=new BossEncounter();if(phaseTwo)boss.hurt(boss.health()/2);
            until(boss,BossEncounter.Attack.MARKS,BossEncounter.State.TELEGRAPH);
            List<BossEncounter.Mark> original=List.copyOf(boss.marks());
            PlayerState player=new PlayerState(profession);
            if(phaseTwo){assertTrue(player.canDash());player.tryDash();assertTrue(player.isDashing());}
            float x=120,y=80,damage=0,step=1f/fps;
            for(int frame=0;frame<fps*2&&boss.attack()==BossEncounter.Attack.MARKS;frame++) {
                x-=player.movementSpeed()*step;
                x=Math.max(BossEncounter.MIN_X+BossEncounter.PLAYER_RADIUS,x);
                damage=Math.max(damage,boss.update(step,x,y,false));player.advance(step);
                if(boss.attack()==BossEncounter.Attack.MARKS&&boss.state()==BossEncounter.State.TELEGRAPH)
                    assertEquals(original,boss.marks(),"Warnings must not chase the escape");
            }
            assertEquals(0,damage,profession+" must escape phase "+(phaseTwo?2:1)+" at "+fps+"Hz");
            assertEquals(phaseTwo?3:1,original.size());
        }
    }
    @Test void fasterChargeKeepsItsSweptSingleHitAtTypicalAndClampedFrameSteps() {
        for(float step:new float[]{1f/60,1f/120,.05f})for(boolean phaseTwo:List.of(false,true)) {
            BossEncounter boss=new BossEncounter();if(phaseTwo)boss.hurt(boss.health()/2);
            until(boss,BossEncounter.Attack.CHARGE,BossEncounter.State.TELEGRAPH);
            float damage=0;int impactCount=0;
            while(boss.state()==BossEncounter.State.TELEGRAPH||boss.state()==BossEncounter.State.ACTIVE) {
                damage+=boss.update(step,120,80,false);
                impactCount+=boss.hits().stream().filter(hit->hit.kind()==CombatHit.Kind.CONTACT).count();
            }
            assertEquals(phaseTwo?24:18,damage,.001f);
            assertEquals(1,impactCount,"A faster charge must not tunnel or hit again on successive frames");
        }
    }
    @Test void tighterPhaseTwoFanStillFitsTheAnnouncedConeAndCannotStackWithTheLeap() {
        BossEncounter boss=new BossEncounter();boss.hurt(boss.health()/2);
        until(boss,BossEncounter.Attack.VOLLEY,BossEncounter.State.ACTIVE);
        assertEquals(BossEncounter.MAX_SHOTS,boss.shots().size());
        for(BossEncounter.Shot shot:boss.shots()) {
            double angle=Math.atan2(shot.y-boss.y(),shot.x-boss.x());
            double offset=Math.atan2(Math.sin(angle-boss.aimAngle()),Math.cos(angle-boss.aimAngle()));
            assertTrue(Math.abs(Math.toDegrees(offset))<=56.01,"No projectile may leave the visual warning cone");
        }
        boss.update(1.5f,460,210,false);
        assertTrue(boss.shots().isEmpty(),"The following leap removes the fan instead of stacking hazards");
        assertTrue(boss.airborne());
    }
    @Test void phaseTwoInitiatesMoreThreatsWithoutShorteningTheThreeSecondLeap() {
        for(int fps:new int[]{60,120}) {
            BossEncounter normal=new BossEncounter(EnemyDifficulty.BASE,new Random(4));
            BossEncounter powered=new BossEncounter(EnemyDifficulty.BASE,new Random(4));powered.hurt(powered.health()/2);
            for(int frame=0;frame<30*fps;frame++) {
                normal.update(1f/fps,120,80,false);powered.update(1f/fps,120,80,false);
            }
            int normalAttacks=normal.summary().attacks().values().stream().mapToInt(Integer::intValue).sum();
            int poweredAttacks=powered.summary().attacks().values().stream().mapToInt(Integer::intValue).sum();
            assertTrue(poweredAttacks>normalAttacks,"Phase two needs more attacks, not only a larger life bar");
            assertEquals(30,normal.elapsed(),.015f);assertEquals(30,powered.elapsed(),.015f);
        }
    }
}

