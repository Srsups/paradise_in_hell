package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TutorialGuideTest {
    private static TutorialGuide.Context combat(boolean active, boolean priority, boolean exit, boolean xp) {
        return new TutorialGuide.Context(TutorialGuide.Place.COMBAT, active, priority, exit, xp, false);
    }
    private static void basicActions(TutorialGuide guide) {
        guide.action(TutorialTopic.MOVE); guide.action(TutorialTopic.ATTACK); guide.action(TutorialTopic.DASH);
    }
    @Test void readingRequiresSevenVisibleActiveSecondsAndCanResumeAfterPause() {
        TutorialGuide guide = new TutorialGuide(false);
        guide.advance(3, combat(true, false, false, false));
        guide.advance(20, combat(false, false, false, false));
        guide.advance(20, combat(true, true, false, false));
        assertEquals(3, guide.readingSeconds(TutorialTopic.MOVE)); assertFalse(guide.seen(TutorialTopic.MOVE));
        guide.advance(3.99f, combat(true, false, false, false)); assertNull(guide.pollCompleted());
        guide.advance(.01f, combat(true, false, false, false));
        assertTrue(guide.seen(TutorialTopic.MOVE)); assertEquals(TutorialTopic.MOVE, guide.pollCompleted());
        assertNull(guide.pollCompleted()); assertEquals(TutorialTopic.ATTACK, guide.current(combat(true, false, false, false)));
    }
    @Test void actionsCompleteOnceWithoutWaitingForTheEarlierHints() {
        TutorialGuide guide = new TutorialGuide(false);
        guide.action(TutorialTopic.DASH); guide.action(TutorialTopic.DASH);
        assertEquals(TutorialTopic.DASH, guide.pollCompleted()); assertNull(guide.pollCompleted());
        assertEquals(0, guide.readingSeconds(TutorialTopic.DASH));
        assertEquals(TutorialTopic.MOVE, guide.current(combat(true, false, false, false)));
    }
    @Test void exitAndBossContextsReplaceBasicHintsAndDangerSuppressesAllReading() {
        TutorialGuide guide = new TutorialGuide(false);
        assertEquals(TutorialTopic.EXIT, guide.current(combat(true, false, true, false)));
        assertEquals(TutorialTopic.GUARDIAN, guide.current(new TutorialGuide.Context(TutorialGuide.Place.GUARDIAN, true, false, false, false, false)));
        guide.advance(100, combat(true, true, true, false)); assertFalse(guide.seen(TutorialTopic.EXIT));
        guide.action(TutorialTopic.EXIT);
        assertNull(guide.current(combat(true, false, true, true)), "An unlocked exit never competes with generic movement advice");
    }
    @Test void experienceNeedsARealWorldOpportunityAndBoonsFollowIt() {
        TutorialGuide guide = new TutorialGuide(true); basicActions(guide);
        assertNull(guide.current(combat(true, false, false, false)));
        assertEquals(TutorialTopic.EXPERIENCE, guide.current(combat(true, false, false, true)));
        guide.action(TutorialTopic.EXPERIENCE);
        assertEquals(TutorialTopic.BOONS, guide.current(combat(true, false, false, true)));
        guide.action(TutorialTopic.BOONS); assertNull(guide.current(combat(true, false, false, true)));
    }
    @Test void campaignGoalsAndReturnHintsRemainSpecificToTheirContext() {
        TutorialGuide guide = new TutorialGuide(false); basicActions(guide);
        assertEquals(TutorialTopic.GOALS, guide.current(combat(true, false, false, true)));
        assertEquals(TutorialTopic.REFUGE, guide.current(new TutorialGuide.Context(TutorialGuide.Place.REFUGE, true, false, false, false, false)));
        assertEquals(TutorialTopic.RETURN, guide.current(new TutorialGuide.Context(TutorialGuide.Place.REFUGE, true, false, false, false, true)));
        guide.action(TutorialTopic.RETURN);
        assertEquals(TutorialTopic.REFUGE, guide.current(new TutorialGuide.Context(TutorialGuide.Place.REFUGE, true, false, false, false, true)));
    }
    @Test void demoNeverImportsOrCompletesCampaignTopics() {
        TutorialGuide guide = new TutorialGuide(true); guide.synchronize(true, ignored -> true);
        assertFalse(guide.seen(TutorialTopic.MOVE));
        for (TutorialTopic topic : new TutorialTopic[]{TutorialTopic.GOALS, TutorialTopic.EXIT, TutorialTopic.REFUGE, TutorialTopic.RETURN, TutorialTopic.GUARDIAN}) {
            guide.action(topic); assertFalse(guide.seen(topic));
        }
        assertNull(guide.pollCompleted());
        assertNull(guide.current(new TutorialGuide.Context(TutorialGuide.Place.REFUGE, true, false, false, false, true)));
        assertEquals(TutorialTopic.MOVE, guide.current(combat(true, false, true, false)));
    }
    @Test void disablingHintsFreezesReadingAndReenablingDoesNotEraseProgress() {
        TutorialGuide guide = new TutorialGuide(false); guide.advance(2, combat(true, false, false, false));
        guide.synchronize(false, ignored -> false); guide.advance(100, combat(true, false, false, false));
        assertNull(guide.current(combat(true, false, false, false))); assertEquals(2, guide.readingSeconds(TutorialTopic.MOVE));
        guide.synchronize(true, topic -> topic == TutorialTopic.ATTACK);
        guide.advance(5, combat(true, false, false, false));
        assertEquals(TutorialTopic.DASH, guide.current(combat(true, false, false, false)));
        assertEquals(TutorialTopic.MOVE, guide.pollCompleted()); assertNull(guide.pollCompleted(), "Loading saved facts never emits save requests");
    }
    @Test void contextsAndElapsedTimeAreValidatedWithoutCorruptingTheGuide() {
        TutorialGuide guide = new TutorialGuide(false);
        for (float delta : new float[]{-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> guide.advance(delta, combat(true, false, false, false)));
        assertThrows(NullPointerException.class, () -> guide.current(null));
        assertThrows(NullPointerException.class, () -> new TutorialGuide.Context(null, true, false, false, false, false));
        assertFalse(guide.seen(TutorialTopic.MOVE));
    }
    @Test void changingArenaContextsRetainsTheSameReadingTimeAndFacts() {
        TutorialGuide guide = new TutorialGuide(false); guide.advance(4, combat(true, false, false, false));
        guide.advance(2, new TutorialGuide.Context(TutorialGuide.Place.GUARDIAN, true, false, false, false, false));
        assertEquals(4, guide.readingSeconds(TutorialTopic.MOVE)); assertEquals(2, guide.readingSeconds(TutorialTopic.GUARDIAN));
        guide.advance(3, combat(true, false, false, false)); assertTrue(guide.seen(TutorialTopic.MOVE));
    }
    @Test void realSessionRetainsTheGuideAcrossRoutesButCreatesANewOneForAnotherAttempt() throws Exception {
        RegionDefinition region;
        try(var source=getClass().getResourceAsStream("/campaign/submundo-prototype.json")) {
            assertNotNull(source);
            region=RegionDefinition.fromJson(new String(source.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        }
        RunSession session=RunSession.campaign(region);TutorialGuide guide=session.tutorial();
        guide.advance(4,combat(true,false,false,false));
        for(var goal:session.arena().killGoals().entrySet())
            for(int kill=0;kill<goal.getValue();kill++)session.recordKill(goal.getKey());
        session.enterArena("cinzas");assertSame(guide,session.tutorial());
        assertEquals(4,session.tutorial().readingSeconds(TutorialTopic.MOVE));
        RunSession next=session.newAttempt();assertNotSame(guide,next.tutorial());
        assertEquals(0,next.tutorial().readingSeconds(TutorialTopic.MOVE));
    }
    @Test void boonsGrantedOutsideTheCombatScreenStillProduceOneLearningFact() {
        RunSession session=RunSession.demo();
        assertNull(session.tutorial().pollCompleted());
        session.applyUpgrade(Upgrade.FORCA);
        assertTrue(session.tutorial().seen(TutorialTopic.BOONS));
        assertEquals(TutorialTopic.BOONS,session.tutorial().pollCompleted());
        session.applyUpgrade(Upgrade.VELOCIDADE);
        assertNull(session.tutorial().pollCompleted());
    }
}
