package br.com.srsups.paradiseinhell;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MilestoneFourBoonsTest {
    private static final CombatEffects.LineOfSight CLEAR=AdvancedCombatTest.CLEAR;
    private static AdvancedCombatTest.Target target(float x,float y){return new AdvancedCombatTest.Target(x,y);}
    @Test void amparoProtectsSubsequentDamageWithoutNegatingTheTriggerOrRefreshingOnEveryHit(){
        PlayerState p=new PlayerState();Upgrade.AMPARO.apply(p);
        p.sofrerDano(10);assertEquals(90,p.getVida());assertTrue(p.boons().armorActive());
        p.sofrerDano(10);assertEquals(83,p.getVida(),.001f);
        p.advance(2);assertFalse(p.boons().armorActive());
        p.sofrerDano(10);assertEquals(73,p.getVida());assertFalse(p.boons().armorActive());
        p.advance(6);p.sofrerDano(10);assertEquals(63,p.getVida());assertTrue(p.boons().armorActive());
    }
    @Test void blocksAndInvulnerabilityDoNotTriggerAmparo(){
        PlayerState p=new PlayerState(Profession.GUERREIRO);Upgrade.AMPARO.apply(p);p.setGuard(true,1,0);
        assertTrue(p.receive(new CombatHit(20,10,0,CombatHit.Kind.PROJECTILE),0,0));assertFalse(p.boons().armorActive());
        p.ativarHabilidadeEgide();p.advance(0);p.sofrerDano(20);assertFalse(p.boons().armorActive());assertEquals(150,p.getVida());
    }
    @Test void furorUsesEachClassBaseAndLeavesWhenHealed(){
        for(Profession profession:Profession.values()){
            PlayerState p=new PlayerState(profession);p.aumentarDano(.4f);p.forgeWeapon();float base=p.getDano();
            Upgrade.FUROR.apply(p);assertEquals(base,p.getDano(),.001f);
            float forgedBase=profession.weapon.damage*p.getWeaponRarity().damageMultiplier;
            p.sofrerDano(p.getVidaMaxima()*.7f);assertEquals(base+forgedBase*.25f,p.getDano(),.001f);
            Upgrade.FUROR.apply(p);assertEquals(base+forgedBase*.5f,p.getDano(),.001f);
            p.curar(1000);assertEquals(base,p.getDano(),.001f);
        }
    }
    @Test void staminaPerKillHasOneGlobalCooldownAndCannotHealADeadPlayer(){
        PlayerState p=new PlayerState(Profession.GUERREIRO);Upgrade.FOLEGO.apply(p);
        p.setGuard(true,1,0);p.advance(6);p.setGuard(false,1,0);assertEquals(52,p.getEstaminaAtual(),.001f);
        p.onKill();assertEquals(58,p.getEstaminaAtual(),.001f);
        for(int i=0;i<50;i++)p.onKill();assertEquals(58,p.getEstaminaAtual(),.001f);
        p.advance(2);p.onKill();assertEquals(84,p.getEstaminaAtual(),.001f);
        p.ativarCuraPorAbate(100);p.sofrerDano(1000);p.onKill();assertTrue(p.estaMorto());
    }
    @Test void collectionBurstStacksWithMagnetButCannotBeKeptUpByEveryDash(){
        PlayerState p=new PlayerState();p.aumentarAtracaoXp();Upgrade.RASTRO.apply(p);
        assertEquals(68,p.getRaioAtracaoXp());p.tryDash();assertEquals(164,p.getRaioAtracaoXp());assertEquals(280,p.getVelocidadeAtracaoXp());
        p.advance(1);p.tryDash();p.advance(.51f);
        assertEquals(68,p.getRaioAtracaoXp());assertFalse(p.boons().collectionActive());
    }
    @Test void ambushHasEqualDpsBudgetAcrossClassesAndOnlyOneTargetPerAttack(){
        for(Profession profession:Profession.values()){
            PlayerState p=new PlayerState(profession);p.aumentarDano(.4f);p.aumentarCadencia();p.forgeWeapon();
            Upgrade.EMBOSCADA.apply(p);p.tryDash();
            WeaponBonus bonus=p.boons().beginAttack();CombatEffects fx=new CombatEffects();var a=target(0,0);var b=target(5,0);
            fx.weaponBonus(a,bonus);fx.weaponBonus(b,bonus);
            assertEquals(p.dps()*.45f,1000-a.hp,.001f);assertEquals(1000,b.hp);
            assertEquals(0,p.boons().beginAttack().take());assertFalse(p.boons().ambushReady());
        }
    }
    @Test void projectilesCarryOneBonusThroughPiercingAndMissingDoesNotArmTheNextShot(){
        PlayerState p=new PlayerState();p.forgeWeapon();Upgrade.EMBOSCADA.apply(p);p.tryDash();
        Projetil shot=new Projetil(0,0,new com.badlogic.gdx.math.Vector2(1,0),new com.badlogic.gdx.graphics.g2d.TextureRegion());
        shot.configure(p);assertTrue(shot.canHit("a"));shot.hit("a");
        CombatEffects fx=new CombatEffects();var a=target(0,0);var b=target(10,0);
        fx.weaponBonus(a,shot.bonus);fx.weaponBonus(b,shot.bonus);
        assertTrue(a.hp<1000);assertEquals(1000,b.hp);
        Projetil missed=new Projetil(0,0,new com.badlogic.gdx.math.Vector2(1,0),new com.badlogic.gdx.graphics.g2d.TextureRegion());
        missed.configure(p);assertEquals(0,missed.bonus.take());
        p.advance(2);p.tryDash();p.advance(2.01f);assertEquals(0,p.boons().beginAttack().take());
    }
    @Test void convergenceRequiresAnExistingStatusRespectsWallsAndCannotChainOrProcFromDots(){
        PlayerState p=new PlayerState(Profession.SACERDOTE);Upgrade.CONVERGENCIA.apply(p);
        var a=target(0,0);var near=target(28,0);var far=target(29,0);var blocked=target(0,20);
        var targets=List.of(a,near,far,blocked);EnumMap<DamageSource,Float> damage=new EnumMap<>(DamageSource.class);
        CombatEffects fx=new CombatEffects((t,amount,source)->{t.hurt(amount);damage.merge(source,amount,Float::sum);});
        CombatEffects.LineOfSight line=(x,y,ox,oy)->oy==0;
        fx.onHit(p,a,targets,-20,0,line,true);assertFalse(damage.containsKey(DamageSource.CONVERGENCE));
        fx.onHit(p,a,targets,-20,0,line,true);float budget=p.dps()*.3f*2;
        assertEquals(budget,damage.get(DamageSource.CONVERGENCE),.001f);assertEquals(1000,blocked.hp);assertEquals(1000,far.hp);
        for(int i=0;i<50;i++)fx.onHit(p,a,targets,-20,0,line,true);
        fx.update(1,targets,line);assertEquals(budget,damage.get(DamageSource.CONVERGENCE),.001f);
        p.advance(1.5f);fx.onHit(p,a,targets,-20,0,line,true);
        assertEquals(budget*2,damage.get(DamageSource.CONVERGENCE),.001f);
    }
    @Test void echoIsDelayedStationaryHitsOnceAndSkipsWallsOutOfRangeAndUntargetableBodies(){
        PlayerState p=new PlayerState();Upgrade.ECO_DO_ESTIGE.apply(p);p.tryDash();
        var edge=target(54,20);var far=target(55,20);var blocked=target(10,30);
        var airborne=new AdvancedCombatTest.Target(10,20){@Override public boolean targetable(){return false;}};
        var targets=List.of(edge,far,blocked,airborne);CombatEffects fx=new CombatEffects();fx.dash(p,10,20);
        assertEquals(1,fx.echoes().size());assertEquals(10,fx.echoes().getFirst().x);assertEquals(20,fx.echoes().getFirst().y);
        fx.update(.34f,targets,(x,y,ox,oy)->oy==20);assertEquals(1000,edge.hp);
        fx.update(.02f,targets,(x,y,ox,oy)->oy==20);
        assertEquals(p.dps()*.6f,1000-edge.hp,.001f);assertEquals(1000,far.hp);assertEquals(1000,blocked.hp);assertEquals(1000,airborne.hp);
        fx.update(5,targets,CLEAR);assertEquals(p.dps()*.6f,1000-edge.hp,.001f);assertTrue(fx.echoes().isEmpty());
        p.advance(3);p.tryDash();fx.dash(p,10,20);fx.clear();fx.update(5,targets,CLEAR);
        assertEquals(p.dps()*.6f,1000-edge.hp,.001f);
    }
    @Test void regionalSourceAndSynergyPrerequisiteApplyToOffersAndKeepExistingLuckRules(){
        RegionDefinition subworld=new RegionDefinition(RegionDefinition.PROTOTYPE_ID,"Submundo","fight",
            CampaignProfileTest.region().arenas());
        RunSession local=RunSession.campaign(subworld,Profession.SACERDOTE),other=RunSession.campaign(CampaignProfileTest.region());
        RunSession demo=RunSession.demo();Random rng=new Random(91);boolean found=false;
        for(int i=0;i<1000;i++){
            var offers=UpgradeChoices.draw(local,3,rng,BoonRarity.EPIC);found|=offers.contains(Upgrade.ECO_DO_ESTIGE);
            assertFalse(UpgradeChoices.draw(other,3,rng).contains(Upgrade.ECO_DO_ESTIGE));
            assertFalse(UpgradeChoices.draw(demo,3,rng).contains(Upgrade.ECO_DO_ESTIGE));
            assertFalse(Upgrade.CONVERGENCIA.availableIn(demo));
        }
        assertTrue(found);assertTrue(Upgrade.CONVERGENCIA.availableIn(local));
        demo.applyUpgrade(Upgrade.QUEIMADURA);assertTrue(Upgrade.CONVERGENCIA.availableIn(demo));
        local.applyUpgrade(Upgrade.ECO_DO_ESTIGE);local.recordKill(EnemyType.COMMON);local.enterArena("boss");
        assertEquals(1,local.rank(Upgrade.ECO_DO_ESTIGE));local.player().tryDash();assertTrue(local.player().boons().takeEcho()>0);
        local.end();RunSession fresh=local.newAttempt();
        for(Upgrade boon:new Upgrade[]{Upgrade.AMPARO,Upgrade.FUROR,Upgrade.FOLEGO,Upgrade.RASTRO,Upgrade.EMBOSCADA,Upgrade.CONVERGENCIA,Upgrade.ECO_DO_ESTIGE})
            assertEquals(0,fresh.player().boons().rank(boon));
        assertEquals(0,fresh.player().boons().takeEcho());
    }
}
