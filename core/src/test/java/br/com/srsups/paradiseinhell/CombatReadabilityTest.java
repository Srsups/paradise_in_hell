package br.com.srsups.paradiseinhell;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatReadabilityTest {
    @Test void attackRateReflectsClassCadenceAndPermanentForging() {
        for (Profession profession : Profession.values()) {
            PlayerState player = new PlayerState(profession);
            assertEquals(1/profession.weapon.interval,player.attacksPerSecond(),.0001f);
            float expected = player.attacksPerSecondAfterCadence();
            player.aumentarCadencia(); assertEquals(expected,player.attacksPerSecond(),.0001f);
            float before = player.attacksPerSecond(); player.forgeWeapon();
            assertTrue(player.attacksPerSecond() > before);
            before = player.attacksPerSecond();
            player.aumentarVelocidadeMovimento(.3f); assertEquals(before,player.attacksPerSecond(),.0001f);
            for (int i=0;i<50;i++) player.aumentarCadencia();
            assertTrue(player.attacksPerSecond() <= 1/profession.weapon.minimumInterval + .0001f);
            expected = player.attacksPerSecondAfterCadence();
            player.aumentarCadencia(); assertEquals(expected,player.attacksPerSecond(),.0001f);
        }
    }
    @Test void curseReachesNearbyTargetsButCannotCrossWallsOrExceedItsRadius() {
        PlayerState priest = new PlayerState(Profession.SACERDOTE);
        AdvancedCombatTest.Target hit = new AdvancedCombatTest.Target(0,0), near = new AdvancedCombatTest.Target(32,0),
            far = new AdvancedCombatTest.Target(33,0), wall = new AdvancedCombatTest.Target(0,20);
        CombatEffects effects = new CombatEffects();
        effects.onHit(priest,hit,List.of(hit,near,far,wall),-30,0,(x,y,ox,oy)->oy==0,true);
        assertTrue(effects.cursed(hit)); assertTrue(effects.cursed(near));
        assertFalse(effects.cursed(far)); assertFalse(effects.cursed(wall));
        effects.update(1); float damage = priest.dps()*.6f;
        assertEquals(1000-damage,hit.hp,.001f); assertEquals(1000-damage,near.hp,.001f);
        assertEquals(1000,far.hp); assertEquals(1000,wall.hp);
    }
    @Test void curseAreaStillWorksWhenTheFirstEnemyDiesAndDoesNotRecursivelySpread() {
        PlayerState priest = new PlayerState(Profession.SACERDOTE);
        AdvancedCombatTest.Target dead = new AdvancedCombatTest.Target(0,0), near = new AdvancedCombatTest.Target(25,0),
            outside = new AdvancedCombatTest.Target(50,0); dead.hp=0;
        CombatEffects effects = new CombatEffects();
        effects.onHit(priest,dead,List.of(dead,near,outside),-30,0,AdvancedCombatTest.CLEAR,true);
        assertFalse(effects.cursed(dead)); assertTrue(effects.cursed(near)); assertFalse(effects.cursed(outside));
    }
    @Test void curseDamageScalesWithDamageCadenceAndForgeWithoutStackingCopies() {
        PlayerState priest = new PlayerState(Profession.SACERDOTE);
        priest.aumentarDano(.4f); priest.aumentarCadencia(); priest.forgeWeapon();
        AdvancedCombatTest.Target a = new AdvancedCombatTest.Target(0,0), b = new AdvancedCombatTest.Target(10,0);
        CombatEffects effects = new CombatEffects();
        for(int i=0;i<50;i++) effects.onHit(priest,a,List.of(a,b),0,0,AdvancedCombatTest.CLEAR,true);
        effects.update(5);
        float damage = priest.getDano()*priest.attacksPerSecond()*.6f*5;
        assertEquals(1000-damage,a.hp,.001f); assertEquals(1000-damage,b.hp,.001f);
        assertEquals(0,effects.trackedTargets());
    }
    @Test void boonListPaginatesAcquiredRanksAndResetsWithTheAttempt() {
        RunSession run = RunSession.demo();
        BoonPanel panel = new BoonPanel(run); assertTrue(panel.acquired().isEmpty()); assertEquals(1,panel.pageCount());
        for(Upgrade upgrade : Upgrade.values()) run.applyUpgrade(upgrade);
        run.applyUpgrade(Upgrade.FORCA);
        panel.toggle(); assertTrue(panel.open()); assertEquals(22,panel.acquired().size());
        assertEquals(2,run.rank(Upgrade.FORCA)); assertEquals(4,panel.pageCount());
        panel.movePage(1); panel.movePage(1); panel.movePage(1);panel.movePage(1); assertEquals(3,panel.page());
        assertEquals(Upgrade.ECO_DO_ESTIGE,panel.acquired().getLast());
        panel.close(); assertFalse(panel.open());
        assertTrue(new BoonPanel(run.newAttempt()).acquired().isEmpty());
    }
}

