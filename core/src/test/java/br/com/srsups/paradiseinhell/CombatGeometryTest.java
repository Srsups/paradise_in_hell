package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatGeometryTest {
    @Test void sweptHitDetectsTargetEvenWhenNeitherEndpointOverlaps() {
        assertTrue(CombatGeometry.sweptCircleHits(0, 0, 100, 0, 50, 0, 3));
        assertFalse(CombatGeometry.sweptCircleHits(0, 0, 100, 0, 50, 4, 3));
        assertTrue(CombatGeometry.sweptCircleHits(5, 5, 5, 5, 5, 5, 0));
        assertFalse(CombatGeometry.sweptCircleHits(0, 0, 5, 0, 8, 0, 2));
    }
}
