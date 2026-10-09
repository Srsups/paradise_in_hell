package br.com.srsups.paradiseinhell;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnemyNavigationTest {
    private static long key(int x, int y) { return ((long) x << 32) ^ (y & 0xffffffffL); }
    private static EnemyNavigation navigation(Set<Long> walls) {
        return new EnemyNavigation((x, y) -> walls.contains(key((int) Math.floor(x / 16), (int) Math.floor(y / 16))));
    }
    private static void chase(EnemyNavigation nav, EnemyNavigation.Body body, float targetX, float targetY, int frames) {
        for (int frame = 0; frame < frames; frame++) {
            nav.update(0.05f, targetX, targetY);
            float beforeX = body.x, beforeY = body.y;
            nav.move(body, 70, 0.05f);
            assertTrue(nav.canOccupy(body.x + 8, body.y + 8), "Movement must never cross a solid structure");
            assertTrue(Math.hypot(body.x - beforeX, body.y - beforeY) <= 3.501, "Navigation must not teleport");
        }
    }
    private static void reaches(EnemyNavigation.Body body, float x, float y) {
        assertEquals(x, body.x + 8, 0.1); assertEquals(y, body.y + 8, 0.1);
    }
    @Test void walksAroundPillarBlockingStraightPursuit() {
        EnemyNavigation nav = navigation(Set.of(key(1, 0)));
        EnemyNavigation.Body body = new EnemyNavigation.Body(0, 0);
        chase(nav, body, 72, 8, 120); reaches(body, 72, 8);
    }
    @Test void avoidsDiagonalCornerCutting() {
        EnemyNavigation nav = navigation(Set.of(key(1, 0), key(0, 1)));
        EnemyNavigation.Body body = new EnemyNavigation.Body(0, 0);
        chase(nav, body, 56, 56, 200); reaches(body, 56, 56);
    }
    @Test void exitsUShapeByInitiallyMovingAwayFromPlayer() {
        Set<Long> walls = new HashSet<>();
        for (int y = 0; y <= 3; y++) { walls.add(key(0, y)); walls.add(key(3, y)); }
        for (int x = 0; x <= 3; x++) walls.add(key(x, 3));
        EnemyNavigation nav = navigation(walls);
        EnemyNavigation.Body body = new EnemyNavigation.Body(16, 16);
        chase(nav, body, 24, 88, 240); reaches(body, 24, 88);
    }
    @Test void navigatesNarrowCorridorWithUnalignedStartingCoordinates() {
        Set<Long> walls = new HashSet<>();
        for (int x = -3; x <= 6; x++) { walls.add(key(x, -1)); walls.add(key(x, 1)); }
        EnemyNavigation nav = navigation(walls);
        EnemyNavigation.Body body = new EnemyNavigation.Body(2, 0);
        chase(nav, body, 72, 8, 100); reaches(body, 72, 8);
    }
    @Test void handlesNegativeCoordinatesAndMovingTargets() {
        EnemyNavigation nav = navigation(Set.of(key(-2, -2)));
        EnemyNavigation.Body body = new EnemyNavigation.Body(-64, -32);
        chase(nav, body, 24, -24, 30);
        chase(nav, body, -40, 40, 200); reaches(body, -40, 40);
    }
    @Test void excludesEnclosedSpawnPositionsFromReachableArea() {
        Set<Long> walls = new HashSet<>();
        for (int i = 0; i <= 2; i++) { walls.add(key(i, 0)); walls.add(key(i, 2)); walls.add(key(0, i)); walls.add(key(2, i)); }
        EnemyNavigation nav = navigation(walls);
        nav.update(0, 72, 8);
        assertFalse(nav.reachable(24, 24)); assertTrue(nav.reachable(72, 8));
    }
}
