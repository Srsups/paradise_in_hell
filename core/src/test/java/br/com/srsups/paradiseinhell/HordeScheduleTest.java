package br.com.srsups.paradiseinhell;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HordeScheduleTest {
    private static List<HordeSchedule.Batch> at(HordeSchedule schedule,float seconds){
        return schedule.update(seconds,seconds,true,ArenaVariation.BALANCED,0);
    }
    @Test void regularSpawnsStopBeforeThePackAndResumeOnlyAfterItsRecoveryWindow(){
        HordeSchedule schedule=new HordeSchedule(12);
        assertTrue(at(schedule,31.9f).isEmpty());assertTrue(schedule.allowsRegularSpawns());
        assertTrue(at(schedule,32).isEmpty());assertEquals(HordeSchedule.Phase.WARNING,schedule.phase());
        assertFalse(schedule.allowsRegularSpawns());assertEquals(3,schedule.secondsRemaining(),.0001f);
        var first=at(schedule,35);assertEquals(1,first.size());assertEquals(1,first.getFirst().count());
        assertEquals(HordeSchedule.Kind.FLYER,first.getFirst().kind());assertEquals(HordeSchedule.Phase.HORDE,schedule.phase());
        assertFalse(schedule.allowsRegularSpawns());assertEquals(5,schedule.secondsRemaining(),.0001f);
        at(schedule,39.9f);assertEquals(HordeSchedule.Phase.HORDE,schedule.phase());
        assertTrue(at(schedule,40).isEmpty());assertEquals(HordeSchedule.Phase.RECOVERY,schedule.phase());
        assertFalse(schedule.allowsRegularSpawns());assertEquals(4,schedule.secondsRemaining(),.0001f);
        assertTrue(at(schedule,44).isEmpty());assertTrue(schedule.allowsRegularSpawns());
        assertEquals(28,schedule.secondsRemaining(),.0001f);
        assertTrue(at(schedule,72).isEmpty());assertEquals(HordeSchedule.Phase.WARNING,schedule.phase());
        assertEquals(1,at(schedule,75).getFirst().count());
    }
    @Test void packsAreHomogeneousStableAndCompleteAtSixtyAndOneHundredTwentyUpdatesPerSecond(){
        Map<Long,Integer> sixty=run(60),oneTwenty=run(120);
        assertEquals(sixty,oneTwenty);assertEquals(8,sixty.size());
        assertEquals(sixty,run(60));
    }
    private static Map<Long,Integer> run(int fps){
        HordeSchedule schedule=new HordeSchedule(HordeSchedule.seedFor("same-attempt","estige"));
        Map<Long,Integer> counts=new HashMap<>();Map<Long,HordeSchedule.Kind> kinds=new HashMap<>();
        for(int frame=0;frame<=324*fps;frame++)for(var batch:at(schedule,frame/(float)fps)){
            if(kinds.containsKey(batch.formationSeed()))assertEquals(kinds.get(batch.formationSeed()),batch.kind());
            kinds.put(batch.formationSeed(),batch.kind());counts.merge(batch.formationSeed(),batch.count(),Integer::sum);
        }
        counts.forEach((seed,count)->assertEquals(kinds.get(seed).size(),count));return counts;
    }
    @Test void everyFourScheduledPacksIncludeAllRolesAndFastPacksNeverRepeatConsecutively(){
        for(int seed=0;seed<50;seed++){
            HordeSchedule schedule=new HordeSchedule(seed);HordeSchedule.Kind previous=HordeSchedule.Kind.FLYER;
            assertEquals(HordeSchedule.Kind.FLYER,at(schedule,35).getFirst().kind());
            for(int deck=0;deck<8;deck++){
                EnumSet<HordeSchedule.Kind> kinds=EnumSet.noneOf(HordeSchedule.Kind.class);
                for(int offset=0;offset<4;offset++){
                    float time=75+(deck*4+offset)*40;
                    HordeSchedule.Kind current=at(schedule,time).getFirst().kind();
                    assertFalse(current==HordeSchedule.Kind.FAST&&previous==HordeSchedule.Kind.FAST);
                    kinds.add(current);previous=current;
                }
                assertEquals(EnumSet.allOf(HordeSchedule.Kind.class),kinds);
            }
        }
    }
    @Test void aFastCapExpiresBlockedSlotsWithoutConvertingThePackOrEmittingThemLater(){
        HordeSchedule schedule=new HordeSchedule(43);float fastStart=0;
        for(float time=35;time<400;time+=40){
            at(schedule,time);if(schedule.kind()==HordeSchedule.Kind.FAST){fastStart=time;break;}
        }
        assertTrue(fastStart>0);
        assertTrue(schedule.update(fastStart+.1f,fastStart+.1f,true,ArenaVariation.BALANCED,EnemyWaves.MAX_FAST).isEmpty());
        assertTrue(schedule.update(fastStart+.1f,fastStart+.1f,true,ArenaVariation.BALANCED,0).isEmpty());
        var remaining=schedule.update(fastStart+4.9f,fastStart+4.9f,true,ArenaVariation.BALANCED,EnemyWaves.MAX_FAST-1);
        assertEquals(1,remaining.size());assertEquals(HordeSchedule.Kind.FAST,remaining.getFirst().kind());assertEquals(1,remaining.getFirst().count());
        assertTrue(schedule.update(fastStart+4.9f,fastStart+4.9f,true,ArenaVariation.BALANCED,0).isEmpty());
    }
    @Test void largeTimeJumpsSkipExpiredPacksAndDoNotQueueAMassSpawnOnReturnToNormal(){
        HordeSchedule schedule=new HordeSchedule(5);
        assertTrue(at(schedule,500).isEmpty());assertTrue(schedule.allowsRegularSpawns());
        assertTrue(at(schedule,500).isEmpty());assertEquals(1,at(schedule,515).getFirst().count());
        assertTrue(at(schedule,564).isEmpty());assertTrue(schedule.allowsRegularSpawns());
    }
    @Test void demoIntroducesCommonThenFlyerPacksAndRespectsTheTankIntroduction(){
        HordeSchedule schedule=new HordeSchedule(5);
        assertEquals(HordeSchedule.Kind.COMMON,schedule.update(35,35,false,ArenaVariation.BALANCED,0).getFirst().kind());
        assertEquals(HordeSchedule.Kind.FLYER,schedule.update(75,75,false,ArenaVariation.BALANCED,0).getFirst().kind());
        assertNotEquals(HordeSchedule.Kind.TANK,schedule.update(115,115,false,ArenaVariation.BALANCED,0).getFirst().kind());
        for(int index=3;index<20;index++)schedule.update(35+40*index,35+40*index,false,ArenaVariation.BALANCED,0);
    }
    @Test void stableSeedsDependOnAttemptAndRoomWhileInvalidOrReversedTimeIsRejected(){
        assertEquals(HordeSchedule.seedFor("run","room"),HordeSchedule.seedFor("run","room"));
        assertNotEquals(HordeSchedule.seedFor("run","room"),HordeSchedule.seedFor("run","other"));
        assertNotEquals(HordeSchedule.seedFor("run","room"),HordeSchedule.seedFor("other","room"));
        HordeSchedule schedule=new HordeSchedule(0);at(schedule,20);
        assertThrows(IllegalArgumentException.class,()->at(schedule,19));
        assertThrows(IllegalArgumentException.class,()->at(schedule,Float.NaN));
        assertThrows(IllegalArgumentException.class,()->at(schedule,Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,()->schedule.update(21,20,true,ArenaVariation.BALANCED,0));
        assertThrows(IllegalArgumentException.class,()->schedule.update(21,21,true,ArenaVariation.BALANCED,-1));
        assertThrows(NullPointerException.class,()->schedule.update(21,21,true,null,0));
    }
}
