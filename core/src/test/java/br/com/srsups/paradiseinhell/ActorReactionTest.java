package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActorReactionTest {
    @Test void startsWithoutAReactionAndIdleTimeDoesNotCreateOne() {
        ActorReaction reaction = new ActorReaction();
        reaction.advance(10);
        assertEquals(ActorReaction.State.NONE, reaction.state());
        assertFalse(reaction.active());
        assertFalse(reaction.deathFinished());
        assertEquals(0, reaction.progress());
        assertEquals(0, reaction.frame(3));
    }
    @Test void nonlethalRecoilVisitsItsFramesThenReturnsToNormalAnimation() {
        ActorReaction reaction = new ActorReaction();
        assertTrue(reaction.hurt());
        assertEquals(ActorReaction.State.HURT, reaction.state());
        assertEquals(0, reaction.frame(3));
        reaction.advance(ActorReaction.HURT_SECONDS / 2);
        assertEquals(.5f, reaction.progress());
        assertEquals(1, reaction.frame(3));
        reaction.advance(ActorReaction.HURT_SECONDS / 4);
        assertEquals(2, reaction.frame(3));
        reaction.advance(ActorReaction.HURT_SECONDS / 4);
        assertFalse(reaction.active());
        assertEquals(ActorReaction.State.NONE, reaction.state());
        assertTrue(reaction.hurt());
        assertEquals(0, reaction.frame(3));
    }
    @Test void rapidHitsAndDamageOverTimeCannotKeepRestartingRecoil() {
        ActorReaction reaction = new ActorReaction();
        reaction.hurt();reaction.advance(.09f);
        for (int hit = 0; hit < 100; hit++) assertFalse(reaction.hurt());
        assertEquals(.5f, reaction.progress());
        reaction.advance(.09f);
        assertFalse(reaction.active());
    }
    @Test void deathImmediatelyReplacesHurtAndStartsAtItsOwnFirstFrame() {
        ActorReaction reaction = new ActorReaction();
        reaction.hurt();reaction.advance(.09f);
        assertTrue(reaction.die());
        assertEquals(ActorReaction.State.DEATH, reaction.state());
        assertEquals(0, reaction.progress());
        assertEquals(0, reaction.frame(4));
        assertFalse(reaction.hurt());
        reaction.advance(ActorReaction.DEATH_SECONDS / 2);
        assertEquals(.5f, reaction.progress());
        assertEquals(2, reaction.frame(4));
    }
    @Test void laterHitsAndRepeatedDeathEventsNeverRewindTheOneShot() {
        ActorReaction reaction = new ActorReaction();
        reaction.die();reaction.advance(.3f);
        float progress = reaction.progress();
        assertFalse(reaction.die());
        assertFalse(reaction.hurt());
        assertEquals(progress, reaction.progress());
        reaction.advance(1);
        assertTrue(reaction.deathFinished());
        assertTrue(reaction.active());
        assertEquals(1, reaction.progress());
        assertEquals(3, reaction.frame(4));
        assertFalse(reaction.die());
        reaction.advance(10);
        assertEquals(3, reaction.frame(4));
        assertFalse(reaction.hurt());
    }
    @Test void guardianCanUseAFullSecondWithoutChangingUnitDefaults() {
        ActorReaction guardian = new ActorReaction(.18f, 1);
        guardian.die();guardian.advance(.65f);
        assertFalse(guardian.deathFinished());
        assertEquals(.65f, guardian.progress());
        guardian.advance(.35f);
        assertTrue(guardian.deathFinished());
        ActorReaction unit = new ActorReaction();unit.die();unit.advance(.65f);
        assertTrue(unit.deathFinished());
    }
    @Test void readingPosesOrAdvancingZeroTimeCannotAdvanceTheReaction() {
        ActorReaction reaction = new ActorReaction();reaction.die();reaction.advance(.2f);
        float progress = reaction.progress();int frame = reaction.frame(4);
        for (int read = 0; read < 100; read++) {
            reaction.advance(0);
            assertEquals(progress, reaction.progress());
            assertEquals(frame, reaction.frame(4));
            assertFalse(reaction.deathFinished());
        }
    }
    @Test void onlyAnExplicitNewLifeResetAllowsAnotherDeathOrHurtReaction() {
        ActorReaction reaction = new ActorReaction();reaction.die();reaction.advance(1);reaction.clear();
        assertEquals(ActorReaction.State.NONE, reaction.state());
        assertFalse(reaction.deathFinished());
        assertEquals(0, reaction.progress());
        assertTrue(reaction.hurt());reaction.clear();
        assertFalse(reaction.active());
        assertTrue(reaction.die());
        assertEquals(0, reaction.frame(4));
    }
    @Test void invalidTimesAreRejectedWithoutChangingTheCurrentPose() {
        ActorReaction reaction = new ActorReaction();reaction.hurt();reaction.advance(.05f);
        float progress = reaction.progress();
        for (float invalid : new float[]{-1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> reaction.advance(invalid));
            assertEquals(progress, reaction.progress());
            assertEquals(ActorReaction.State.HURT, reaction.state());
        }
    }
    @Test void durationsMustBePositiveFiniteValues() {
        for (float invalid : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new ActorReaction(invalid, 1));
            assertThrows(IllegalArgumentException.class, () -> new ActorReaction(.18f, invalid));
        }
    }
    @Test void framesAreBoundedForAnyValidClipAndInvalidCountsAreRejected() {
        ActorReaction reaction = new ActorReaction();reaction.die();
        assertEquals(0, reaction.frame(1));
        reaction.advance(.65f);
        for (int count : new int[]{1, 3, 4, 6, Integer.MAX_VALUE}) assertEquals(count - 1, reaction.frame(count));
        assertThrows(IllegalArgumentException.class, () -> reaction.frame(0));
        assertThrows(IllegalArgumentException.class, () -> reaction.frame(-1));
    }
    @Test void veryLargeFiniteStepsFinishWithoutNaNOrLooping() {
        ActorReaction hurt = new ActorReaction();hurt.hurt();hurt.advance(Float.MAX_VALUE);
        assertFalse(hurt.active());assertEquals(0, hurt.progress());
        ActorReaction death = new ActorReaction();death.die();death.advance(Float.MAX_VALUE);
        assertTrue(death.deathFinished());assertEquals(1, death.progress());assertEquals(3, death.frame(4));
    }
}
