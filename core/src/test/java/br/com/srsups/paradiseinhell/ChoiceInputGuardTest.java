package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChoiceInputGuardTest {
    @Test void everyChoiceWaitsOneAndAHalfSeconds() {
        ChoiceInputGuard guard = new ChoiceInputGuard();
        guard.advance(1,false); assertFalse(guard.ready());
        guard.advance(.49f,false); assertFalse(guard.ready());
        guard.advance(.02f,false); assertTrue(guard.ready());
        guard.reset(); assertFalse(guard.ready()); assertEquals(1.5f,guard.remaining());
    }
    @Test void holdingAttackNeverSelectsAndRequiresReleaseAfterTheWait() {
        ChoiceInputGuard guard = new ChoiceInputGuard();
        guard.advance(.2f,false); guard.advance(5,true);
        assertFalse(guard.ready()); assertTrue(guard.notice().contains("Solte"));
        guard.advance(0,false); assertTrue(guard.ready());
    }
    @Test void openingTheNextChoiceDoesNotReuseThePreviousRelease() {
        ChoiceInputGuard guard = new ChoiceInputGuard();
        guard.advance(2,false); assertTrue(guard.ready());
        guard.reset(); guard.advance(2,true); assertFalse(guard.ready());
        guard.advance(0,false); assertTrue(guard.ready());
    }
}

