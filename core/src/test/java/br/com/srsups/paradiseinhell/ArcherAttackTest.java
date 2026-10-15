package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArcherAttackTest {
    private static final ArcherAttack.Target MISS = (ax, ay, bx, by, radius) -> false;
    private static EnemyNavigation open() {
        EnemyNavigation nav = new EnemyNavigation((x, y) -> false);
        nav.update(0, 108, 8); return nav;
    }
    private static void shoot(ArcherAttack attack, EnemyNavigation.Body body, EnemyNavigation nav) {
        attack.update(.01f, body, 108, 8, true, nav, 10);
        attack.update(1, body, 108, 8, true, nav, 10);
        assertEquals(ArcherAttack.Phase.RECOVERING, attack.phase());
    }
    private static ArcherAttack.Target circle(float x, float y) {
        return (ax, ay, bx, by, radius) -> CombatGeometry.sweptCircleHits(ax, ay, bx, by, x, y, radius + 6);
    }
    @Test void waitsOneSecondAndLocksThePositionBeforeThePlayerDashes() {
        ArcherAttack attack = new ArcherAttack(); var body = new EnemyNavigation.Body(0, 0); var nav = open();
        attack.update(.01f, body, 108, 8, true, nav, 10);
        attack.update(.99f, body, 108, 88, true, nav, 10);
        assertEquals(108, attack.aimX()); assertEquals(8, attack.aimY());
        assertTrue(attack.arrows().isEmpty()); assertEquals(0, body.x); assertEquals(0, body.y);
        attack.update(.011f, body, 108, 88, true, nav, 10);
        assertEquals(1, attack.arrows().size());
        assertEquals(0, attack.arrows().getFirst().directionY(), .0001f);
        assertTrue(attack.advanceArrows(1, nav, circle(108, 88)).isEmpty(), "A dash must not redirect the arrow");
    }
    @Test void sweptArrowsHitOnceWithScaledDamageAndAReportLabel() {
        ArcherAttack attack = new ArcherAttack(); var body = new EnemyNavigation.Body(0, 0); var nav = open();
        attack.update(.01f, body, 108, 8, true, nav, 15);
        attack.update(1, body, 108, 8, true, nav, 15);
        var hits = attack.advanceArrows(1, nav, circle(108, 8));
        assertEquals(1, hits.size()); assertEquals(15, hits.getFirst().damage());
        assertEquals(CombatHit.Kind.PROJECTILE, hits.getFirst().kind());
        assertEquals("Arqueiro do Estige / Flecha", hits.getFirst().label());
        assertTrue(hits.getFirst().x() < 108, "Shield direction must use the arrow's approach");
        assertTrue(attack.arrows().isEmpty()); assertTrue(attack.advanceArrows(1, nav, circle(108, 8)).isEmpty());
    }
    @Test void structuresBlockArrowsWithoutTunnellingAndTheNearestCollisionWins() {
        ArcherAttack attack = new ArcherAttack(); var body = new EnemyNavigation.Body(0, 0); var nav = open();
        shoot(attack, body, nav);
        EnemyNavigation wall = new EnemyNavigation((x,y) -> x >= 48 && x < 64);
        assertTrue(attack.advanceArrows(1, wall, circle(108, 8)).isEmpty());
        assertTrue(attack.arrows().isEmpty());
        attack.clear(); shoot(attack, body, nav);
        assertEquals(1, attack.advanceArrows(1, wall, circle(32, 8)).size(), "A player before the wall must still be hit");
    }
    @Test void cannotFireThroughWallsAndUsesTheSharedFieldToFindAnOpening() {
        EnemyNavigation nav = new EnemyNavigation((x,y) -> x >= 16 && x < 32 && y >= 0 && y < 16);
        nav.update(0, 72, 8);
        ArcherAttack attack = new ArcherAttack(); var body = new EnemyNavigation.Body(0, 0);
        for (int i = 0; i < 300 && attack.phase() != ArcherAttack.Phase.AIMING; i++) {
            float beforeX = body.x, beforeY = body.y;
            nav.update(.05f, 72, 8); attack.update(.05f, body, 72, 8, true, nav, 10);
            assertTrue(nav.canOccupy(body.x + 8, body.y + 8));
            assertTrue(Math.hypot(body.x - beforeX, body.y - beforeY) <= ArcherAttack.SPEED * .05f + .001);
        }
        assertEquals(ArcherAttack.Phase.AIMING, attack.phase());
        assertTrue(nav.clearProjectileLine(body.x + 8, body.y + 8, 72, 8, ArcherAttack.ARROW_RADIUS));
    }
    @Test void walksIntoViewAndCancelsAimWhenHiddenOrObstructed() {
        ArcherAttack attack = new ArcherAttack(); var body = new EnemyNavigation.Body(0, 0); var nav = open();
        attack.update(.5f, body, 108, 8, false, nav, 10);
        assertEquals(ArcherAttack.Phase.SEEKING, attack.phase()); assertTrue(body.x > 0);
        attack.update(.01f, body, 108, 8, true, nav, 10);
        attack.update(1, body, 108, 8, false, nav, 10);
        assertTrue(attack.arrows().isEmpty()); assertEquals(ArcherAttack.Phase.RECOVERING, attack.phase());
        attack.clear(); body.x=0;
        attack.update(.01f, body, 108, 8, true, nav, 10);
        var wall = new EnemyNavigation((x,y) -> x >= 48 && x < 64);
        attack.update(1, body, 108, 8, true, wall, 10);
        assertTrue(attack.arrows().isEmpty());
    }
    @Test void retreatsAlongASafeSideWhenTheWallBlocksBackingUp() {
        ArcherAttack attack = new ArcherAttack(); var body = new EnemyNavigation.Body(0, 0);
        EnemyNavigation nav = new EnemyNavigation((x,y) -> x < 0);
        nav.update(0, 40, 8);
        attack.update(.01f, body, 40, 8, true, nav, 10);
        attack.update(1, body, 40, 8, true, nav, 10);
        attack.update(.1f, body, 40, 8, true, nav, 10);
        assertEquals(0, body.x); assertTrue(body.y > 0); assertTrue(nav.canOccupy(body.x + 8, body.y + 8));
    }
    @Test void pausedAttackAndArrowsDoNotAdvanceAndCleanupRemovesAllDanger() {
        ArcherAttack attack = new ArcherAttack(); var body = new EnemyNavigation.Body(0, 0); var nav = open();
        shoot(attack, body, nav); var arrow=attack.arrows().getFirst();
        float x=arrow.x(),y=arrow.y();
        attack.update(0,body,108,8,true,nav,10); attack.advanceArrows(0,nav,MISS);
        assertEquals(x,arrow.x()); assertEquals(y,arrow.y());
        assertEquals(ArcherAttack.Phase.RECOVERING,attack.phase());
        attack.clear(); assertTrue(attack.arrows().isEmpty()); assertEquals(ArcherAttack.Phase.SEEKING,attack.phase());
    }
    @Test void projectilesHaveABoundedCountAndLifetime() {
        ArcherAttack attack = new ArcherAttack(); var body = new EnemyNavigation.Body(0, 0); var nav = open();
        shoot(attack,body,nav);
        for(int i=0;i<50;i++) {attack.update(2,body,108,8,true,nav,10); attack.update(1,body,108,8,true,nav,10);}
        assertEquals(2,attack.arrows().size());
        attack.advanceArrows(3,nav,MISS); assertTrue(attack.arrows().isEmpty());
    }
    @Test void narrowArrowPathDoesNotUseTheLargerWalkingFootprint() {
        EnemyNavigation nav=new EnemyNavigation((x,y)->y>=16);
        assertFalse(nav.clearLine(8,13,108,13));
        assertTrue(nav.clearProjectileLine(8,13,108,13,ArcherAttack.ARROW_RADIUS));
    }
    @Test void frontalShieldBlocksArrowsButRearHitsAndGroundRemainDangerous() {
        ArcherAttack attack = new ArcherAttack(); var body = new EnemyNavigation.Body(0, 0); var nav = open();
        shoot(attack,body,nav);
        CombatHit hit=attack.advanceArrows(1,nav,circle(108,8)).getFirst();
        PlayerState player=new PlayerState(Profession.GUERREIRO);
        player.setGuard(true,-1,0); float life=player.getVida(),stamina=player.getEstaminaAtual();
        assertTrue(player.receive(hit,108,8)); assertEquals(life,player.getVida());
        assertEquals(stamina-12,player.getEstaminaAtual());
        player.setGuard(true,1,0); assertFalse(player.receive(hit,108,8));
        assertEquals(life-10,player.getVida());
        player.setGuard(true,-1,0);
        assertFalse(player.receive(new CombatHit(10,hit.x(),hit.y(),CombatHit.Kind.GROUND),108,8));
        assertEquals(life-20,player.getVida());
    }
}

