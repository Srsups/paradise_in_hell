package br.com.srsups.paradiseinhell;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClassBalanceProbeTest {
    private static ClassBalanceProbe.Row find(Profession profession,WeaponType weapon,ClassBalanceProbe.Progression progression,
                                              GameDifficulty difficulty,EnemyType enemy,int elapsed){
        return ClassBalanceProbe.rows().stream().filter(row->row.profession()==profession&&row.weapon()==weapon&&row.progression()==progression
            &&row.difficulty()==difficulty&&row.enemy()==enemy&&row.elapsedSeconds()==elapsed).findFirst().orElseThrow();
    }
    @Test void allClassToolProgressionDifficultyEnemyAndTimeCombinationsArePresentAndFinite(){
        var rows=ClassBalanceProbe.rows();assertEquals(360,rows.size());HashSet<String> combinations=new HashSet<>();
        for(var row:rows){
            assertTrue(combinations.add(row.profession()+":"+row.weapon()+":"+row.progression()+":"+row.difficulty()+":"+row.enemy()+":"+row.elapsedSeconds()));
            assertTrue(Float.isFinite(row.directDps())&&row.directDps()>0);
            assertTrue(Float.isFinite(row.idealDirectKillSeconds())&&row.idealDirectKillSeconds()>=0);
            assertTrue(row.idealWeaponHits()>0&&row.enemyHealth()>0&&row.health()>0);
        }
    }
    @Test void enemyDifficultyAndElapsedTimeCannotShortenAnOtherwiseIdenticalDirectKill(){
        var rows=ClassBalanceProbe.rows();
        for(var row:rows){
            var previous=rows.stream().filter(candidate->candidate.profession()==row.profession()&&candidate.weapon()==row.weapon()
                &&candidate.progression()==row.progression()&&candidate.enemy()==row.enemy()&&candidate.elapsedSeconds()==row.elapsedSeconds()
                &&candidate.difficulty().ordinal()==Math.max(0,row.difficulty().ordinal()-1)).findFirst().orElseThrow();
            assertTrue(row.enemyHealth()>=previous.enemyHealth());assertTrue(row.idealWeaponHits()>=previous.idealWeaponHits());
            assertTrue(row.idealDirectKillSeconds()>=previous.idealDirectKillSeconds());
            if(row.elapsedSeconds()==120){
                var start=rows.stream().filter(candidate->candidate.profession()==row.profession()&&candidate.weapon()==row.weapon()
                    &&candidate.progression()==row.progression()&&candidate.enemy()==row.enemy()&&candidate.elapsedSeconds()==0
                    &&candidate.difficulty()==row.difficulty()).findFirst().orElseThrow();
                assertTrue(row.enemyHealth()>start.enemyHealth());assertTrue(row.idealWeaponHits()>=start.idealWeaponHits());
            }
        }
    }
    @Test void theDevelopedProfileUsesActualPermanentBonusesAndRareWeaponWithoutFreeTemporaryBoons(){
        var newer=find(Profession.CACADOR,WeaponType.BOW,ClassBalanceProbe.Progression.NEW,GameDifficulty.MEDIUM,EnemyType.TANK,0);
        var evolved=find(Profession.CACADOR,WeaponType.BOW,ClassBalanceProbe.Progression.DEVELOPED_R3_RARE,GameDifficulty.MEDIUM,EnemyType.TANK,0);
        assertEquals(5,newer.damage());assertEquals(7.1875f,evolved.damage(),.00001);
        assertEquals(130,evolved.health(),.0001);assertEquals(9,evolved.armorPercent(),.0001);
        assertEquals(65.4f,evolved.walkingSpeed(),.0001);assertEquals(218,evolved.dashSpeed(),.0001);
        assertTrue(evolved.idealDirectKillSeconds()<newer.idealDirectKillSeconds());
        assertEquals(WeaponType.SCYTHE,ClassBalanceProbe.player(Profession.FAZENDEIRO,WeaponType.SCYTHE,ClassBalanceProbe.Progression.NEW).getWeapon());
        assertThrows(IllegalArgumentException.class,()->ClassBalanceProbe.player(Profession.CACADOR,WeaponType.SCYTHE,ClassBalanceProbe.Progression.NEW));
    }
    @Test void classSpecificEffectsAreMeasuredSeparatelyAndDoNotLeakIntoTheDirectKillColumn(){
        var wand=find(Profession.FEITICEIRA,WeaponType.WAND,ClassBalanceProbe.Progression.NEW,GameDifficulty.MEDIUM,EnemyType.COMMON,0);
        assertEquals(1,wand.splashDamage(),.0001);assertEquals(2,wand.idealWeaponHits());assertEquals(.75f,wand.idealDirectKillSeconds());
        var priest=find(Profession.SACERDOTE,WeaponType.CURSE,ClassBalanceProbe.Progression.NEW,GameDifficulty.MEDIUM,EnemyType.COMMON,0);
        assertEquals(2.25f,priest.curseDps());assertEquals(4,priest.idealWeaponHits());
        var necromancer=find(Profession.NECROMANTE,WeaponType.SOUL,ClassBalanceProbe.Progression.NEW,GameDifficulty.MEDIUM,EnemyType.COMMON,0);
        assertEquals(35,necromancer.weakResurrectionPercent(),.0001);assertEquals(0,wand.weakResurrectionPercent());
        var shield=find(Profession.GUERREIRO,WeaponType.SHIELD,ClassBalanceProbe.Progression.NEW,GameDifficulty.MEDIUM,EnemyType.COMMON,0);
        assertEquals(27.5f,shield.shieldWalkingSpeed());assertEquals(0,wand.shieldWalkingSpeed());
        var scythe=find(Profession.FAZENDEIRO,WeaponType.SCYTHE,ClassBalanceProbe.Progression.NEW,GameDifficulty.MEDIUM,EnemyType.COMMON,0);
        assertEquals(8,scythe.meleeRingTargets());
    }
    @Test void csvIsDeterministicAndHasOneHeaderAndOneRowPerCombination(){
        String csv=ClassBalanceProbe.csv();assertEquals(csv,ClassBalanceProbe.csv());assertEquals(361,csv.lines().count());
        assertTrue(csv.startsWith("profession,weapon,progression,difficulty,enemy,elapsed_seconds,"));
        assertTrue(ClassBalanceProbe.notes().contains("primeiro acerto em t=0"));
        assertTrue(ClassBalanceProbe.notes().contains("não classifica profissões"));
    }
}
