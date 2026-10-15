package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RunReportTest {
    @Test void reportsKeepRoomsRouteTimingBoonsAndActualDamageWithoutOverkill() {
        RunSession run=RunSession.campaign(CampaignProfileTest.region());
        run.advance(12); run.recordKill(EnemyType.COMMON); run.applyUpgrade(Upgrade.FORCA);
        AdvancedCombatTest.Target enemy=new AdvancedCombatTest.Target(0,0); enemy.hp=10;
        run.statistics().deal(enemy,100,DamageSource.WEAPON);
        run.player().aumentarResistencia(.1f);
        run.player().receive(new CombatHit(20,0,0,CombatHit.Kind.GROUND,"Marcas"),0,0);
        run.enterArena("boss"); run.advance(3);
        run.statistics().deal(new BossEncounter(),5,DamageSource.AURA);
        run.player().receive(new CombatHit(500,0,0,CombatHit.Kind.GROUND,"Queda do Tártaro"),0,0);
        run.end(); RunReport report=run.report(RunOutcome.DEATH,2);
        assertEquals(15,report.duration(),.001f); assertEquals(2,report.rooms().size());
        assertEquals(12,report.rooms().get(0).duration(),.001f); assertEquals(12,report.rooms().get(0).unlockedAt(),.001f);
        assertEquals(3,report.rooms().get(1).duration(),.001f); assertEquals(1,report.kills());
        assertEquals(10,report.dealt().get(DamageSource.WEAPON)); assertEquals(5,report.dealt().get(DamageSource.AURA));
        assertEquals(15,report.damageDealt()); assertEquals(100,report.received()); assertEquals("Queda do Tártaro",report.deathCause());
        assertEquals(1,report.boons().get(Upgrade.FORCA));
        assertEquals(report,RunReport.fromJson(new JsonReader().parse(report.toJson())));
        assertThrows(UnsupportedOperationException.class,()->report.boons().clear());
    }
    @Test void shieldsAndAirborneInvulnerabilityAreExcludedFromDamageTotals() {
        RunSession run=RunSession.demo(Profession.GUERREIRO); run.player().setGuard(true,1,0);
        run.player().receive(new CombatHit(50,10,0,CombatHit.Kind.PROJECTILE,"Pedras"),0,0);
        BossEncounter boss=new BossEncounter();
        for(int i=0;i<3000 && !boss.airborne();i++)boss.update(.01f,180,100);
        run.statistics().deal(boss,500,DamageSource.WEAPON);
        run.end(); RunReport report=run.report(RunOutcome.ABANDONED,0);
        assertEquals(0,report.received()); assertEquals(0,report.damageDealt());
    }
    @Test void curseBurnShockAndSplashAreAttributedToTheirActualOrigins() {
        RunSession run=RunSession.demo(Profession.SACERDOTE); run.player().addBurn(); run.player().addShock();
        AdvancedCombatTest.Target a=new AdvancedCombatTest.Target(0,0),b=new AdvancedCombatTest.Target(10,0);
        CombatEffects effects=new CombatEffects(run.statistics());
        effects.onHit(run.player(),a,java.util.List.of(a,b),0,0,AdvancedCombatTest.CLEAR,true);
        effects.update(1); run.end(); RunReport report=run.report(RunOutcome.ABANDONED,0);
        assertTrue(report.dealt().get(DamageSource.CURSE)>0); assertTrue(report.dealt().get(DamageSource.BURN)>0);
        assertTrue(report.dealt().get(DamageSource.SHOCK)>0);
        assertEquals(2000-a.hp-b.hp,report.damageDealt(),.001f);
    }
    @Test void incomingDamageAfterDeathDoesNotOverwriteTheKillingSourceAndResurrectionIsCounted() {
        RunSession run=RunSession.demo(); PlayerState player=run.player();
        player.ganharRessurreicao(); player.receive(new CombatHit(1000,0,0,CombatHit.Kind.GROUND,"Primeira queda"),0,0);
        player.usarRessurreicao(); player.advance(2.1f);
        player.receive(new CombatHit(1000,0,0,CombatHit.Kind.GROUND,"Segunda queda"),0,0);
        player.receive(new CombatHit(1000,0,0,CombatHit.Kind.GROUND,"Contato posterior"),0,0);
        run.end(); RunReport report=run.report(RunOutcome.DEATH,0);
        assertEquals(150,report.received()); assertEquals(1,report.resurrections()); assertEquals("Segunda queda",report.deathCause());
    }
    @Test void soulsReflectEnemyDifficultyMiniBossBossAndFarmingPressure() {
        RunSession run=RunSession.campaign(CampaignProfileTest.region());
        run.advance(45); run.markMiniBossSpawned(); run.recordMiniBossDefeat(); assertEquals(20,run.earnedSouls());
        run.recordKill(EnemyType.COMMON); run.recordKill(EnemyType.FAST); run.recordKill(EnemyType.TANK);
        assertEquals(28,run.earnedSouls());
        assertThrows(IllegalStateException.class,run::recordMiniBossDefeat);
        run.advance(120); int before=run.earnedSouls(); run.recordKill(EnemyType.TANK);
        assertEquals(2,run.earnedSouls()-before);
        run.enterArena("boss"); run.recordBossDefeat(); assertEquals(before+102,run.earnedSouls());
        assertThrows(IllegalStateException.class,run::recordBossDefeat);
        run.end(); assertEquals(run.earnedSouls(),run.report(RunOutcome.VICTORY,0).souls());
    }
    @Test void alliesReportOnlyTheRemainingHealthOfTheirVictim() {
        RunSession run=RunSession.demo(Profession.NECROMANTE);
        Necromancy army=new Necromancy(AdvancedCombatTest.always(),run.statistics());
        AdvancedCombatTest.Target corpse=new AdvancedCombatTest.Target(0,0); corpse.hp=0;
        assertTrue(army.resurrect(run.player(),corpse));
        AdvancedCombatTest.Target victim=new AdvancedCombatTest.Target(1,0); victim.hp=2;
        army.update(.01f,0,0,java.util.List.of(victim),(x,y)->false,AdvancedCombatTest.CLEAR);
        run.end(); RunReport report=run.report(RunOutcome.ABANDONED,0);
        assertEquals(2,report.dealt().get(DamageSource.ALLY)); assertEquals(2,report.damageDealt());
    }
    @Test void shieldRetaliationIsAttributedWithoutCountingBlockedDamage() {
        RunSession run=RunSession.demo(Profession.GUERREIRO); PlayerState player=run.player();
        player.forgeWeapon(); player.setGuard(true,1,0);
        AdvancedCombatTest.Target enemy=new AdvancedCombatTest.Target(10,0);
        CombatDefense.resolve(player,java.util.List.of(new CombatHit(20,10,0,CombatHit.Kind.PROJECTILE)),
            0,0,enemy,AdvancedCombatTest.always(),run.statistics());
        run.end(); RunReport report=run.report(RunOutcome.ABANDONED,0);
        assertEquals(player.getDano()*1.5f,report.dealt().get(DamageSource.RETALIATION)); assertEquals(0,report.received());
    }
    @Test void historicalAthenaRanksAllowTheFormerLimitButRejectImpossibleCounts() {
        RunSession run = RunSession.demo(); run.applyUpgrade(Upgrade.INTELIGENCIA); run.end();
        var report = run.report(RunOutcome.ABANDONED, 0).json();
        var ranks = report.get("boons");
        for (int count : new int[]{1, 2, 3}) {
            ranks.remove("INTELIGENCIA"); ranks.addChild("INTELIGENCIA", new com.badlogic.gdx.utils.JsonValue((long) count));
            assertEquals(count, RunReport.fromJson(report).boons().get(Upgrade.INTELIGENCIA));
        }
        for (int count : new int[]{0, 4}) {
            ranks.remove("INTELIGENCIA"); ranks.addChild("INTELIGENCIA", new com.badlogic.gdx.utils.JsonValue((long) count));
            assertThrows(IllegalArgumentException.class, () -> RunReport.fromJson(report));
        }
        RunSession next = run.newAttempt(); next.applyUpgrade(Upgrade.INTELIGENCIA);
        assertThrows(IllegalStateException.class, () -> next.applyUpgrade(Upgrade.INTELIGENCIA));
    }
    @Test void finishedReportsStayImmutableAndNewAttemptsStartWithEmptyMetrics() {
        RunSession run=RunSession.demo(); run.advance(10); run.applyUpgrade(Upgrade.FORCA); run.end();
        RunReport report=run.report(RunOutcome.ABANDONED,0);
        assertSame(report,run.report(RunOutcome.ABANDONED,0));
        assertThrows(IllegalStateException.class,()->run.report(RunOutcome.DEATH,0));
        RunSession next=run.newAttempt(); next.end(); RunReport empty=next.report(RunOutcome.ABANDONED,0);
        assertEquals(0,empty.duration()); assertTrue(empty.boons().isEmpty()); assertNotEquals(report.id(),empty.id());
    }
}

