package br.com.srsups.paradiseinhell;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SceneryThemeTest {
    private static RegionDefinition region() throws Exception {
        try (var source = SceneryThemeTest.class.getResourceAsStream("/campaign/submundo-prototype.json")) {
            assertNotNull(source);
            return RegionDefinition.fromJson(new String(source.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
    @Test void productionArenaIdsSelectTheFiveStableArtKeys() throws Exception {
        RegionDefinition region = region();
        assertEquals(SceneryTheme.STYX, SceneryTheme.forArena(region.arena("estige")));
        assertEquals(SceneryTheme.ASHES, SceneryTheme.forArena(region.arena("cinzas")));
        assertEquals(SceneryTheme.SHADOWS, SceneryTheme.forArena(region.arena("sombras")));
        assertEquals(SceneryTheme.GUARDIAN, SceneryTheme.forArena(region.arena("portoes")));
        assertEquals(List.of("refuge", "styx", "ashes", "shadows", "guardian"),
            java.util.Arrays.stream(SceneryTheme.values()).map(SceneryTheme::key).toList());
        assertEquals(5, java.util.Arrays.stream(SceneryTheme.values()).map(SceneryTheme::title).distinct().count());
    }
    @Test void demoUsesStyxAndCampaignChangesThemeWithTheActualRoute() throws Exception {
        assertEquals(SceneryTheme.STYX, SceneryTheme.forSession(RunSession.demo()));
        for (String route : List.of("cinzas", "sombras", "portoes")) {
            RunSession session = RunSession.campaign(region());
            assertEquals(SceneryTheme.STYX, SceneryTheme.forSession(session));
            for (var goal : session.arena().killGoals().entrySet())
                for (int kill = 0; kill < goal.getValue(); kill++) session.recordKill(goal.getKey());
            session.enterArena(route);
            assertEquals(SceneryTheme.forArena(session.arena()), SceneryTheme.forSession(session));
            assertEquals(0, session.arenaElapsed());
        }
    }
    @Test void unknownCombatContentFallsBackSafelyAndBossKindAlwaysSelectsTheGuardian() {
        ArenaDefinition unknown = new ArenaDefinition("future", "Future", "", ArenaDefinition.Kind.COMBAT, 0,
            Map.of(EnemyType.COMMON, 1), List.of("boss"));
        ArenaDefinition boss = new ArenaDefinition("cinzas", "New boss", "", ArenaDefinition.Kind.BOSS_ROOM, 0, Map.of(), List.of());
        assertEquals(SceneryTheme.STYX, SceneryTheme.forArena(unknown));
        assertEquals(SceneryTheme.GUARDIAN, SceneryTheme.forArena(boss));
        assertThrows(NullPointerException.class, () -> SceneryTheme.forArena(null));
        assertThrows(NullPointerException.class, () -> SceneryTheme.forSession(null));
    }
    @Test void fixedCoordinatesHaveStableVariantsIndependentOfEnumOrdinals() {
        assertEquals(1, SceneryTheme.REFUGE.variant(123456789, -17, 23, 7));
        assertEquals(3, SceneryTheme.STYX.variant(123456789, -17, 23, 7));
        assertEquals(5, SceneryTheme.ASHES.variant(123456789, -17, 23, 7));
        assertEquals(4, SceneryTheme.SHADOWS.variant(123456789, -17, 23, 7));
        assertEquals(0, SceneryTheme.GUARDIAN.variant(123456789, -17, 23, 7));
    }
    @Test void cacheVisitOrderAndExtremeCoordinatesCannotChangeOrEscapeTheVariantPool() {
        int[] coordinates = {Integer.MIN_VALUE, -100000, -1, 0, 1, 100000, Integer.MAX_VALUE};
        for (SceneryTheme theme : SceneryTheme.values()) for (long seed : new long[]{0, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
            int initial = theme.floorVariant(seed, -32, 71);
            for (int x : coordinates) for (int y : coordinates) {
                assertTrue(theme.floorVariant(seed, x, y) >= 0 && theme.floorVariant(seed, x, y) < 6);
                assertTrue(theme.obstacleVariant(seed, x, y) >= 0 && theme.obstacleVariant(seed, x, y) < 3);
                assertEquals(0, theme.variant(seed, x, y, 1));
                int large = theme.variant(seed, x, y, Integer.MAX_VALUE);
                assertTrue(large >= 0 && large < Integer.MAX_VALUE);
            }
            assertEquals(initial, theme.floorVariant(seed, -32, 71));
            assertThrows(IllegalArgumentException.class, () -> theme.variant(seed, 0, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> theme.variant(seed, 0, 0, -1));
        }
    }
    @Test void floorsRemainQuietWithAtLeastNinetyFivePercentBaseTilesAndAllAccentsPresent() {
        for (SceneryTheme theme : SceneryTheme.values()) {
            int[] counts = new int[6];HashSet<Integer> obstacles = new HashSet<>();
            for (int x = -100; x <= 100; x++) for (int y = -100; y <= 100; y++) {
                counts[theme.floorVariant(123456789, x, y)]++;
                obstacles.add(theme.obstacleVariant(123456789, x, y));
            }
            int total = 201 * 201, accents = counts[3] + counts[4] + counts[5];
            assertTrue(accents > total * .025f && accents <= total * .05f, theme.name());
            for (int variant = 0; variant < 3; variant++) assertTrue(counts[variant] > total * .25f);
            for (int variant = 3; variant < 6; variant++) assertTrue(counts[variant] > 0);
            assertEquals(java.util.Set.of(0, 1, 2), obstacles);
        }
    }
}
