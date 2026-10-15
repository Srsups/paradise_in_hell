package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DivineFuryTest {
    @Test void noPenaltyBeforeExitAndGraceThenWarningHaveRealTimeMinimums() {
        DivineFury fury = new DivineFury();
        fury.update(1000,false,20,0,6); assertEquals(0,fury.farmingSeconds()); assertEquals(0,fury.level());
        fury.update(19,true,20,0,6); assertFalse(fury.warned());
        fury.update(1,true,20,0,6); assertTrue(fury.warned()); assertEquals(0,fury.level());
        fury.update(14,true,20,0,6); assertEquals(0,fury.level());
        fury.update(1,true,20,0,6); assertEquals(1,fury.level());
        assertTrue(fury.notice().contains("FÚRIA")); assertEquals(1.2f,fury.healthMultiplier(),.0001f);
    }
    @Test void strongerBuildOrArmyAcceleratesFuryAfterWarningAndPenaltyIsCapped() {
        DivineFury normal = new DivineFury(), strong = new DivineFury();
        normal.update(45,true,1,0,0); strong.update(45,true,10,0,6);
        assertTrue(strong.level() > normal.level());
        strong.update(1000,true,100,0,6); assertEquals(3,strong.level());
        assertEquals(1.6f,strong.healthMultiplier(),.0001f);
        assertEquals(1.45f,strong.damageMultiplier(),.0001f);
        assertTrue(strong.spawnMultiplier()<1); assertTrue(strong.xpMultiplier()<1);
    }
    @Test void baseClassesSharePowerBaselineAndPermanentAndBoonStatsContribute() {
        for(Profession profession : Profession.values()) {
            PlayerState player = new PlayerState(profession); assertEquals(1,player.powerRatio(),.0001f);
            player.forgeWeapon(); assertTrue(player.powerRatio()>1);
            float before = player.powerRatio(); player.aumentarDano(.2f); player.aumentarVidaMaxima(.2f);
            player.addShock(); player.addBurn(); assertTrue(player.powerRatio()>before);
        }
    }
    @Test void leavingRoomResetsPressureAndOrdinaryDifficultyStillIncreases() {
        RunSession run = RunSession.campaign(CampaignProfileTest.region());
        run.advance(200); assertEquals(0,run.fury().level());
        run.recordKill(EnemyType.COMMON); run.advance(120); assertEquals(3,run.fury().level());
        EnemyDifficulty boosted = run.enemyDifficulty();
        EnemyDifficulty base = EnemyDifficulty.at(run.progress().elapsed(),0);
        assertEquals(base.healthMultiplier()*1.6f,boosted.healthMultiplier(),.0001f);
        assertEquals(2,run.xpDrop(5));
        run.enterArena("boss"); assertEquals(0,run.fury().level());
        assertEquals(base.healthMultiplier(),run.enemyDifficulty().healthMultiplier(),.0001f);
        run.advance(1000); assertEquals(0,run.fury().level());
    }
    @Test void demoHasNoFuryAndFreshAttemptDoesNotInheritPressure() {
        RunSession demo = RunSession.demo(); demo.advance(200); assertEquals(0,demo.fury().level());
        RunSession campaign = RunSession.campaign(CampaignProfileTest.region());
        campaign.recordKill(EnemyType.COMMON); campaign.advance(100);
        assertEquals(3,campaign.fury().level()); campaign.end();
        assertEquals(0,campaign.newAttempt().fury().level());
    }
}

