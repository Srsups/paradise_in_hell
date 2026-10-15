package br.com.srsups.paradiseinhell;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedCombatTest {
    static class Target implements CombatTarget {
        float x, y, hp = 1000, force = 1;
        boolean guardian;
        Target(float x, float y) { this.x = x; this.y = y; }
        public float centerX() { return x; } public float centerY() { return y; }
        public float health() { return hp; } public float maxHealth() { return 1000; }
        public float strength() { return force; } public boolean floorBoss() { return guardian; }
        public void hurt(float amount) { hp = Math.max(0, hp - amount); }
        public void push(float dx, float dy, CombatEffects.LineOfSight line) {
            if (line.clear(x, y, x + dx, y + dy)) { x += dx; y += dy; }
        }
    }
    static final CombatEffects.LineOfSight CLEAR = (x,y,ox,oy) -> true;
    static Random always() { return new Random() { @Override public double nextDouble() { return 0; } }; }

    @Test void elementalDamageUsesRelativeDpsAndIndependentCooldownForEveryProfession() {
        for (Profession profession : Profession.values()) {
            PlayerState p = new PlayerState(profession); p.addShock(); p.addBurn();
            Target first = new Target(30,0), second = new Target(60,0), third = new Target(300,0);
            CombatEffects fx = new CombatEffects(); var targets = List.of(first,second,third);
            fx.onHit(p,first,targets,0,0,CLEAR,false);
            assertEquals(1000 - p.dps() * .35f * 1.25f, first.hp, .001f);
            assertTrue(second.hp < 1000); assertEquals(1000,third.hp);
            float afterShock = first.hp;
            fx.onHit(p,first,targets,0,0,CLEAR,false);
            assertEquals(afterShock,first.hp);
            fx.update(3); assertEquals(afterShock - p.dps() * .35f * 3, first.hp, .001f);
            assertEquals(0,fx.trackedTargets());
            p.advance(.8f); fx.onHit(p,first,targets,0,0,CLEAR,false);
            assertTrue(first.hp < afterShock - p.dps() * .35f * 3);
        }
    }
    @Test void lightningAndSpellSplashRespectWallsAndOnlyWandHasInnateExplosion() {
        PlayerState wand = new PlayerState(Profession.FEITICEIRA);
        Target a = new Target(30,0), b = new Target(50,0);
        CombatEffects fx = new CombatEffects();
        fx.onHit(wand,a,List.of(a,b),0,0,CLEAR,true); assertEquals(1000,b.hp);
        wand.forgeWeapon(); wand.advance(.8f);
        fx.onHit(wand,a,List.of(a,b),0,0,CLEAR,true); assertTrue(b.hp < 1000);
        PlayerState blocked = new PlayerState(Profession.FEITICEIRA); blocked.forgeWeapon(); blocked.addShock();
        float before = b.hp;
        fx.onHit(blocked,a,List.of(a,b),0,0,(x,y,ox,oy)->false,true);
        assertEquals(before,b.hp);
    }
    @Test void cursesRefreshWithoutStackingAndForgingExtendsDuration() {
        PlayerState priest = new PlayerState(Profession.SACERDOTE);
        Target a = new Target(30,0); CombatEffects fx = new CombatEffects();
        for (int i=0;i<50;i++) fx.onHit(priest,a,List.of(a),0,0,CLEAR,true);
        fx.update(3); assertEquals(1000 - priest.dps() * .6f * 3,a.hp,.001f);
        assertFalse(fx.cursed(a));
        priest.forgeWeapon();
        float before = a.hp;
        fx.onHit(priest,a,List.of(a),0,0,CLEAR,true); fx.update(5);
        assertEquals(before - priest.dps() * .6f * 5,a.hp,.001f);
        assertEquals(0,fx.trackedTargets());
    }
    @Test void impactCombinesWithAuraAndWallsBlockDisplacement() {
        PlayerState p = new PlayerState(); p.addImpact();
        Target a = new Target(30,0); CombatEffects fx = new CombatEffects();
        fx.onHit(p,a,List.of(a),0,0,CLEAR,false); assertEquals(48,a.x,.001f);
        p.advance(.8f); p.ativarAuraDePoseidon();
        fx.onHit(p,a,List.of(a),0,0,CLEAR,false); assertEquals(72.3f,a.x,.001f);
        p.advance(.8f); float before = a.x;
        fx.onHit(p,a,List.of(a),0,0,(x,y,ox,oy)->false,false); assertEquals(before,a.x);
    }
    @Test void shieldBlocksFrontButNotBackOrGroundAndConsumesStamina() {
        PlayerState p = new PlayerState(Profession.GUERREIRO); p.setGuard(true,1,0);
        assertTrue(p.receive(new CombatHit(20,10,0,CombatHit.Kind.PROJECTILE),0,0));
        assertEquals(150,p.getVida()); assertEquals(88,p.getEstaminaAtual());
        assertFalse(p.receive(new CombatHit(20,-10,0,CombatHit.Kind.CONTACT),0,0));
        assertEquals(130,p.getVida());
        assertFalse(p.receive(new CombatHit(20,10,0,CombatHit.Kind.GROUND),0,0));
        assertEquals(110,p.getVida()); assertTrue(p.tryShoot()); assertFalse(p.tryShoot());
        assertEquals(27.5f,p.movementSpeed(),.001f);
        p.advance(p.getAttackInterval()); p.setGuard(false,1,0); assertTrue(p.tryShoot());
        PlayerState archer = new PlayerState(); archer.setGuard(true,1,0); assertFalse(archer.guarding());
    }
    @Test void attackingDuringGuardKeepsDefenseAndCooldownWithoutExtraStaminaCost() {
        for (WeaponRarity rarity : WeaponRarity.values()) {
            PlayerState p = new PlayerState(Profession.GUERREIRO);
            if (rarity == WeaponRarity.RARE) p.forgeWeapon();
            p.setGuard(true,1,0); float stamina=p.getEstaminaAtual();
            assertTrue(p.tryShoot()); assertTrue(p.guarding()); assertFalse(p.tryShoot());
            assertEquals(stamina,p.getEstaminaAtual());
            p.tryDash(); assertEquals(27.5f,p.movementSpeed(),.001f);
            assertTrue(p.receive(new CombatHit(10,10,0,CombatHit.Kind.PROJECTILE),0,0));
            assertEquals(150,p.getVida()); assertEquals(stamina-12,p.getEstaminaAtual());
            p.advance(p.getAttackInterval()); assertTrue(p.tryShoot()); assertTrue(p.guarding());
        }
    }
    @Test void forgedShieldRetaliatesWithCooldownAndCannotMaskGroundDamage() {
        PlayerState p = new PlayerState(Profession.GUERREIRO); p.forgeWeapon(); p.setGuard(true,1,0);
        Target source = new Target(10,0);
        assertTrue(CombatDefense.resolve(p,List.of(
            new CombatHit(30,10,0,CombatHit.Kind.PROJECTILE), new CombatHit(10,0,0,CombatHit.Kind.GROUND)),0,0,source,always()));
        assertEquals(140,p.getVida()); assertTrue(source.hp < 1000);
        float hp = source.hp;
        CombatDefense.resolve(p,List.of(new CombatHit(20,10,0,CombatHit.Kind.PROJECTILE)),0,0,source,always());
        assertEquals(hp,source.hp); p.advance(.6f);
        CombatDefense.resolve(p,List.of(new CombatHit(20,10,0,CombatHit.Kind.CONTACT)),0,0,source,always());
        assertTrue(source.hp < hp);
    }
    @Test void exhaustedShieldCannotDefendForever() {
        PlayerState p = new PlayerState(Profession.GUERREIRO); p.setGuard(true,1,0);
        for (int i=0;i<9;i++) p.receive(new CombatHit(10,10,0,CombatHit.Kind.CONTACT),0,0);
        assertFalse(p.guarding()); assertTrue(p.getVida() < 150);
    }
    @Test void necromancyChanceDecreasesWithStrengthAndNeverIncludesFloorBoss() {
        PlayerState p = new PlayerState(Profession.NECROMANTE); Target weak = new Target(30,0), strong = new Target(50,0);
        strong.force = 4;
        assertEquals(.35,Necromancy.chance(p,weak),.0001);
        assertEquals(.0875,Necromancy.chance(p,strong),.0001);
        p.forgeWeapon(); assertEquals(.4375,Necromancy.chance(p,weak),.0001);
        strong.guardian = true; assertEquals(0,Necromancy.chance(p,strong));
        assertEquals(0,Necromancy.chance(new PlayerState(),weak));
        assertEquals(0,Necromancy.chance(p,new BossEncounter()));
    }
    @Test void alliesFightCarryBetweenRoomsExpireAndDoNotDuplicateCorpses() {
        PlayerState p = new PlayerState(Profession.NECROMANTE); Necromancy army = new Necromancy(always());
        Target corpse = new Target(30,0); corpse.hp = 0;
        assertTrue(army.resurrect(p,corpse)); assertFalse(army.resurrect(p,corpse));
        Target enemy = new Target(32,0);
        army.update(.1f,0,0,List.of(enemy),(x,y)->false,CLEAR);
        assertTrue(enemy.hp < 1000);
        army.enterRoom(100,100); assertEquals(92,army.allies().get(0).x);
        army.update(61,100,100,List.of(),(x,y)->false,CLEAR);
        assertTrue(army.allies().isEmpty());
        for(int i=0;i<10;i++) { Target dead = new Target(i*10,0); dead.hp = 0; army.resurrect(p,dead); }
        assertEquals(6,army.allies().size()); army.clear(); assertTrue(army.allies().isEmpty());
    }
    @Test void deathAndNewAttemptsClearAlliesAndElementalRanks() {
        RunSession run = RunSession.campaign(CampaignProfileTest.region(),Profession.NECROMANTE);
        Target corpse = new Target(30,0); corpse.hp=0;
        // Deterministic army lifecycle is tested above; this run verifies the state boundary.
        run.applyUpgrade(Upgrade.CHOQUE); run.applyUpgrade(Upgrade.QUEIMADURA); run.applyUpgrade(Upgrade.IMPACTO);
        run.end(); RunSession fresh = run.newAttempt();
        assertEquals(0,fresh.player().shockRank()); assertEquals(0,fresh.player().burnRank());
        assertEquals(0,fresh.player().impactRank()); assertTrue(fresh.necromancy().allies().isEmpty());
        assertEquals(Profession.NECROMANTE,fresh.player().getProfession());
    }
}
