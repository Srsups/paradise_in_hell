package br.com.srsups.paradiseinhell;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossLeapTest {
    private static BossEncounter leap(boolean phaseTwo) {
        BossEncounter boss = new BossEncounter();
        if (phaseTwo) boss.hurt(boss.maxHealth() / 2);
        for (int frame = 0; frame < 3000; frame++) {
            if (boss.airborne()) return boss;
            boss.update(.01f, 180, 100);
        }
        throw new AssertionError("Leap must occur after marks, charge and volley");
    }
    private static void lock(BossEncounter boss) {
        for (int frame = 0; frame < 400 && !boss.leapLocked(); frame++) boss.update(.01f, 180, 100);
        assertTrue(boss.leapLocked());
    }
    private static float land(BossEncounter boss, float playerX, float playerY) {
        float damage = 0;
        for (int frame = 0; frame < 400 && boss.airborne(); frame++) damage = Math.max(damage, boss.update(.01f, playerX, playerY));
        assertFalse(boss.airborne()); assertEquals(BossEncounter.State.ACTIVE, boss.state());
        return damage;
    }
    @Test void leapStartsAtPlayerSizeAndLeavesTheBossOutOfReach() {
        BossEncounter boss = leap(false);
        assertEquals(BossEncounter.Attack.LEAP, boss.attack());
        assertEquals(BossEncounter.PLAYER_RADIUS, boss.leapRadius(), .0001f);
        assertTrue(boss.shots().isEmpty()); assertFalse(boss.targetable());
        float hp = boss.health();
        boss.hurt(100); assertEquals(hp,boss.health());
        assertEquals(0,boss.update(.25f,boss.x(),boss.y()),"Airborne boss cannot deal contact damage");
        assertFalse(boss.spriteVisible());
        assertTrue(boss.leapRadius()>BossEncounter.PLAYER_RADIUS);
    }
    @Test void chasingHasConstantSpeedAndNeverTeleportsWithADash() {
        BossEncounter normal=leap(false), powered=leap(true);
        float nx=normal.leapX(), ny=normal.leapY(), px=powered.leapX(), py=powered.leapY();
        normal.update(.1f,400,180); powered.update(.1f,400,180);
        assertEquals(BossEncounter.LEAP_TRACK_SPEED*.1f,Math.hypot(normal.leapX()-nx,normal.leapY()-ny),.001);
        assertEquals(BossEncounter.ENRAGED_LEAP_TRACK_SPEED*.1f,Math.hypot(powered.leapX()-px,powered.leapY()-py),.001);
        assertTrue(powered.leapX()>normal.leapX());
        assertTrue(powered.leapX()<400,"Marker has a speed cap even if the player position jumps");
        assertTrue(normal.leapX()<400);
    }
    @Test void markerTurnsRedAndStopsExactlyOneSecondBeforeImpactButKeepsGrowing() {
        BossEncounter boss=leap(false); lock(boss);
        assertEquals(1,boss.leapSecondsRemaining(),.015f);
        float x=boss.leapX(), y=boss.leapY(), radius=boss.leapRadius();
        assertEquals(0,boss.update(.5f,420,190)); assertTrue(boss.airborne()); assertTrue(boss.leapLocked());
        assertEquals(x,boss.leapX()); assertEquals(y,boss.leapY()); assertTrue(boss.leapRadius()>radius);
        assertEquals(0,boss.update(.45f,420,190)); assertTrue(boss.airborne());
        assertEquals(0,land(boss,420,190));
        assertEquals(x,boss.x()); assertEquals(y,boss.y());
        assertEquals(BossEncounter.DRAW_SIZE*1.5f,boss.leapRadius()*2,.0001f);
        assertTrue(boss.targetable()); assertTrue(boss.spriteVisible());
    }
    @Test void landingDealsScaledGroundDamageOnceAndBypassesDirectionalShield() {
        BossEncounter boss=leap(true); lock(boss); float x=boss.leapX(), y=boss.leapY();
        assertEquals(BossEncounter.ENRAGED_LEAP_DAMAGE,land(boss,x,y));
        assertEquals(1,boss.hits().size()); assertEquals(CombatHit.Kind.GROUND,boss.hits().get(0).kind());
        PlayerState shield=new PlayerState(Profession.GUERREIRO); shield.setGuard(true,1,0);
        assertFalse(shield.receive(boss.hits().get(0),x,y));
        assertEquals(150-BossEncounter.ENRAGED_LEAP_DAMAGE,shield.getVida());
        assertEquals(0,boss.update(.2f,x,y),"The landing is a single hit, not continuous AoE");
        BossEncounter scaled=new BossEncounter(new EnemyDifficulty(2,1.5f));
        for(int i=0;i<3000 && !scaled.airborne();i++)scaled.update(.01f,180,100);
        lock(scaled); assertEquals(BossEncounter.LEAP_DAMAGE*1.5f,land(scaled,scaled.leapX(),scaled.leapY()));
    }
    @Test void actualDamageUsesTheCircleAndPlayerBodyAtItsBorder() {
        BossEncounter hit=leap(false); lock(hit);
        assertEquals(BossEncounter.LEAP_DAMAGE,land(hit,hit.leapX()+BossEncounter.LEAP_MAX_RADIUS+BossEncounter.PLAYER_RADIUS-.1f,hit.leapY()));
        BossEncounter miss=leap(false); lock(miss);
        assertEquals(0,land(miss,miss.leapX()+BossEncounter.LEAP_MAX_RADIUS+BossEncounter.PLAYER_RADIUS+.1f,miss.leapY()));
    }
    @Test void secondPhaseNeedsDashWithBaseAndFullyUpgradedSpeedForEveryClass() {
        for(Profession profession:Profession.values()) for(boolean upgraded:List.of(false,true)) {
            BossEncounter walking=leap(true); lock(walking);
            PlayerState player=new PlayerState(profession);
            if(upgraded) player.aumentarVelocidadeMovimento(.55f);
            player.setWalkingSpeedLimit(walking.walkingSpeedLimit());
            float x=walking.leapX(), y=walking.leapY(), damage=0;
            for(int i=0;i<20;i++) {
                player.advance(.05f); x+=player.movementSpeed()*.05f;
                damage=Math.max(damage,walking.update(.05f,x,y));
            }
            assertEquals(BossEncounter.ENRAGED_LEAP_DAMAGE,damage,profession+" cannot outrun the red circle by walking");
            BossEncounter dodging=leap(true); lock(dodging);
            player=new PlayerState(profession);
            if(upgraded)player.aumentarVelocidadeMovimento(.55f);
            player.setWalkingSpeedLimit(dodging.walkingSpeedLimit());
            x=dodging.leapX(); y=dodging.leapY(); damage=0;
            for(int i=0;i<20;i++) {
                player.advance(.05f); if(i==0)player.tryDash();
                x+=player.movementSpeed()*.05f;
                damage=Math.max(damage,dodging.update(.05f,x,y));
            }
            assertEquals(0,damage,profession+" must be able to escape with a dash when the circle turns red");
            assertEquals(80,player.getEstaminaAtual(),11f);
            assertEquals(Float.POSITIVE_INFINITY,dodging.walkingSpeedLimit());
        }
    }
    @Test void firstPhaseCanStillBeDodgedByWalkingAndOnlyRedPhaseTwoAppliesPressure() {
        BossEncounter boss=leap(false); lock(boss);
        assertEquals(Float.POSITIVE_INFINITY,boss.walkingSpeedLimit());
        float x=boss.leapX(), y=boss.leapY(), damage=0;
        for(int i=0;i<20;i++) { x+=Profession.GUERREIRO.baseSpeed*.05f; damage=Math.max(damage,boss.update(.05f,x,y)); }
        assertEquals(0,damage);
        BossEncounter powered=leap(true); assertEquals(Float.POSITIVE_INFINITY,powered.walkingSpeedLimit());
        PlayerState state=new PlayerState(); float normal=state.movementSpeed();
        state.setWalkingSpeedLimit(24); assertEquals(24,state.movementSpeed());
        state.tryDash(); assertEquals(Profession.CACADOR.baseDashSpeed,state.movementSpeed());
        state.advance(.2f); state.setWalkingSpeedLimit(Float.POSITIVE_INFINITY); assertEquals(normal,state.movementSpeed());
    }
    @Test void landingAndMarkerStayInsideRoomAndRecoveryReturnsToExistingCycle() {
        BossEncounter boss=leap(false);
        while(!boss.leapLocked())boss.update(.01f,2000,-2000);
        assertTrue(boss.leapX()<=BossEncounter.MAX_X-BossEncounter.RADIUS);
        assertTrue(boss.leapY()>=BossEncounter.MIN_Y+BossEncounter.RADIUS);
        land(boss,120,160);
        assertEquals(boss.leapX(),boss.x()); assertEquals(boss.leapY(),boss.y());
        for(int i=0;i<300 && boss.attack()==BossEncounter.Attack.LEAP;i++)boss.update(.01f,120,160);
        assertNotEquals(BossEncounter.Attack.LEAP,boss.attack()); assertFalse(boss.leapVisible());
    }
    @Test void existingCurseAndBurnExpireWithoutDamagingTheAirborneBoss() {
        BossEncounter boss = new BossEncounter();
        for (int frame = 0; frame < 3000 && !(boss.attack() == BossEncounter.Attack.VOLLEY
                && boss.state() == BossEncounter.State.ACTIVE); frame++) boss.update(.01f, 180, 100);
        assertEquals(BossEncounter.Attack.VOLLEY, boss.attack());
        PlayerState priest = new PlayerState(Profession.SACERDOTE); priest.addBurn();
        CombatEffects effects = new CombatEffects();
        effects.onHit(priest, boss, List.of(boss), 180, 100, (x,y,ox,oy) -> true, true);
        assertTrue(effects.cursed(boss)); assertTrue(effects.burning(boss));
        for (int frame = 0; frame < 400 && !boss.airborne(); frame++) {
            effects.update(.01f); boss.update(.01f, 180, 100);
        }
        assertTrue(boss.airborne());
        float hp = boss.health();
        effects.update(.4f);
        assertEquals(hp, boss.health()); assertTrue(effects.cursed(boss));
        effects.update(3);
        assertEquals(hp, boss.health()); assertEquals(0, effects.trackedTargets());
        land(boss, 420, 190); effects.update(1);
        assertEquals(hp, boss.health(), "Expired damage must not be queued for the landing");
    }
    @Test void resurrectedAlliesDoNotAttackTheOldPositionWhileBossIsAirborne() {
        BossEncounter boss=leap(false); boss.update(.25f,180,100);
        Necromancy army=new Necromancy(new Random(){@Override public double nextDouble(){return 0;}});
        AdvancedCombatTest.Target corpse=new AdvancedCombatTest.Target(boss.x(),boss.y()); corpse.hp=0;
        assertTrue(army.resurrect(new PlayerState(Profession.NECROMANTE),corpse));
        float hp=boss.health();
        army.update(.05f,180,100,List.of(boss),(x,y)->false,(x,y,ox,oy)->true);
        assertEquals(hp,boss.health()); assertEquals(0,army.allies().get(0).cooldown);
    }
}

