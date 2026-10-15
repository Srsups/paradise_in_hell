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
            assertEquals(enraged?.65f:.95f,boss.elapsed()-recoveryStart,.025f);
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
        shot=powered.shots().get(4);
        assertEquals(14,powered.update(.01f,shot.x,shot.y,false));
    }
    @Test void currentPatternsStayBoundedAndLeapRemovesOtherHazardsAcrossManyCycles() {
        BossEncounter boss=new BossEncounter(EnemyDifficulty.BASE,new Random(12));boss.hurt(boss.health()/2);
        for(int i=0;i<15000;i++) {
            boss.update(.01f,120,80,false);
            assertTrue(boss.shots().size()<=9);
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
}

