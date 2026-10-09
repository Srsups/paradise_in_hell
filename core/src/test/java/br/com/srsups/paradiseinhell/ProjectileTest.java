package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectileTest {
    @Test void missedProjectilesExpireEvenWithoutReachingAnObstacle() {
        Vector2 direction = new Vector2(1, 0);
        Projetil projectile = new Projetil(0, 0, direction, null);
        assertEquals(new Vector2(1, 0), direction);
        assertFalse(projectile.deveSerRemovido);
        projectile.update(3, null);
        assertTrue(projectile.deveSerRemovido);
    }
}
