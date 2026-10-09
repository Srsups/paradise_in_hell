package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RunProgressTest {
    @Test void survivalEndsAtFiveMinutesWithoutOvershooting() {
        RunProgress run = new RunProgress();
        run.advance(299.9f);
        assertFalse(run.won());
        run.advance(1);
        assertTrue(run.won());
        assertEquals(300, run.elapsed());
        assertEquals("05:00", RunProgress.formatTime(run.elapsed()));
    }

    @Test void laterWavesAreHarderAndHaveAFiniteSpawnInterval() {
        RunProgress run = new RunProgress();
        float firstInterval = run.spawnInterval();
        assertEquals(1, run.waveSize());
        run.advance(120);
        assertEquals(3, run.waveSize());
        assertEquals(3, run.phase());
        assertTrue(run.spawnInterval() < firstInterval);
        run.advance(180);
        assertTrue(run.spawnInterval() >= 0.32f);
        assertEquals(5, run.phase());
    }

    @Test void rejectsInvalidSimulationTime() {
        RunProgress run = new RunProgress();
        assertThrows(IllegalArgumentException.class, () -> run.advance(-1));
        assertThrows(IllegalArgumentException.class, () -> run.advance(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> run.advance(Float.POSITIVE_INFINITY));
        assertEquals(0, run.elapsed());
    }
}
