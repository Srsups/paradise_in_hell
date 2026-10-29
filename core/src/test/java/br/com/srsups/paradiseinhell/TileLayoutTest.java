package br.com.srsups.paradiseinhell;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TileLayoutTest {
    private static boolean legacy(long seed, int x, int y) {
        Random random = new Random(seed ^ (x * 73856093L) ^ (y * 19349663L));
        random.nextInt(3);
        return random.nextFloat() < .012f;
    }
    @Test void obstacleDecisionsMatchTheLegacyRandomSequenceAcrossThousandsOfTiles() {
        for (long seed : new long[]{0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE, 0x123456789ABCDEFL}) {
            int solid = 0;
            for (int x = -64; x <= 64; x++) for (int y = -64; y <= 64; y++) {
                boolean expected = legacy(seed, x, y);
                assertEquals(expected, TileLayout.solid(seed, x, y), "seed=" + seed + ", tile=" + x + "," + y);
                if (expected) solid++;
            }
            assertTrue(solid > 0 && solid < 129 * 129, "The comparison includes both scenery and obstacles");
        }
    }
    @Test void largeNegativeAndPositiveCoordinatesRetainTheSameOverflowAndSeedSemantics() {
        int[] coordinates = {Integer.MIN_VALUE, -1000000000, -1, 0, 1, 1000000000, Integer.MAX_VALUE};
        for (long seed : new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE})
            for (int x : coordinates) for (int y : coordinates)
                assertEquals(legacy(seed, x, y), TileLayout.solid(seed, x, y));
    }
    @Test void nextIntRejectionStillConsumesTheExtraDrawBeforeTestingTheObstacle() {
        // This seed starts nextInt(3) with 2147483647, which Java Random must reject.
        long seed = 168171419115068L;
        Random reference = new Random(seed);
        reference.nextInt(3);
        assertEquals(.0034228563f, reference.nextFloat());
        assertTrue(TileLayout.solid(seed, 0, 0));
        assertEquals(legacy(seed, 0, 0), TileLayout.solid(seed, 0, 0));
    }
    @Test void visualThemesAndRepeatedQueriesNeverAlterCollisionDecisions() {
        long seed = -5823459012L;
        for (int x = -32; x <= 32; x++) for (int y = -32; y <= 32; y++) {
            boolean before = TileLayout.solid(seed, x, y);
            for (SceneryTheme theme : SceneryTheme.values()) {
                theme.floorVariant(seed, x, y);theme.obstacleVariant(seed, x, y);
            }
            assertEquals(before, TileLayout.solid(seed, x, y));
            assertEquals(legacy(seed, x, y), before);
        }
    }
}
