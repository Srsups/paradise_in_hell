package br.com.srsups.paradiseinhell;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VisualPilotTest {
    private static DialoguePlayback text(String value) {
        return new DialoguePlayback(new CaronteDialogue.Conversation(null,List.of(
            new CaronteDialogue.Line("Caronte",value),new CaronteDialogue.Line("Protagonista","É claro."))),false);
    }
    @Test void oneActionCompletesTextAndTheNextActionAdvancesExactlyOnce() {
        DialoguePlayback playback=text("Uma travessia, não uma mudança de endereço.");
        playback.advance(.1f); assertFalse(playback.complete()); assertEquals(4,playback.visibleText().length());
        assertEquals(DialoguePlayback.Action.REVEALED,playback.accept()); assertEquals(0,playback.line());
        assertTrue(playback.complete()); assertTrue(playback.visibleText().endsWith("endereço."));
        assertEquals(DialoguePlayback.Action.ADVANCED,playback.accept()); assertEquals(1,playback.line());
        assertEquals("",playback.visibleText()); playback.accept();
        assertEquals(DialoguePlayback.Action.FINISHED,playback.accept());
    }
    @Test void revealUsesCodePointsAndInstantModeDoesNotSkipOrAutoCompleteTheConversation() {
        DialoguePlayback playback=text("Já 🏹 é?");
        playback.advance(4f/42);
        assertEquals("Já 🏹",playback.visibleText());
        playback.instant(true);assertTrue(playback.complete());assertEquals(0,playback.line());
        assertEquals(DialoguePlayback.Action.ADVANCED,playback.accept());
        assertEquals("É claro.",playback.visibleText());
        playback.instant(false);assertTrue(playback.complete());assertEquals("É claro.",playback.visibleText());
        playback.advance(30);assertTrue(playback.complete());assertEquals(1,playback.line());
        assertThrows(IllegalArgumentException.class,()->playback.advance(Float.NaN));
    }
    @Test void turningOffInstantTextKeepsTheReadLineButTheNextLineTypesNormally() {
        DialoguePlayback playback=new DialoguePlayback(List.of(
            new DialogueLine("Caronte","Uma fala já lida.",DialoguePortrait.CHARON_AMUSED),
            new DialogueLine("Protagonista","Outra fala.")),true);
        assertEquals(2,playback.lineCount());assertEquals("Uma fala já lida.",playback.visibleText());
        playback.instant(false);assertTrue(playback.complete());assertEquals("Uma fala já lida.",playback.visibleText());
        assertEquals(DialoguePlayback.Action.ADVANCED,playback.accept());assertEquals("",playback.visibleText());
        playback.advance(2f/42);assertEquals("Ou",playback.visibleText());
        playback.instant(true);assertEquals("Outra fala.",playback.visibleText());
        playback.instant(false);assertEquals("Outra fala.",playback.visibleText());assertEquals(1,playback.line());
    }
    @Test void cancellingAndReopeningDoesNotCreditOrMarkTheNarrativeEvent() {
        CampaignProfile profile=new CampaignProfile();
        var first=CaronteDialogue.select(profile);
        DialoguePlayback playback=new DialoguePlayback(first,true);playback.accept();
        assertEquals(CampaignProfile.Event.CARONTE_MET,CaronteDialogue.select(profile).event());
        assertEquals(0,profile.obols());assertFalse(profile.has(CampaignProfile.Event.CARONTE_MET));
        profile.finishConversation(first.event());profile.finishConversation(first.event());assertEquals(5,profile.obols());
        assertNull(CaronteDialogue.select(profile).event());
    }
    @Test void dialogueDataRetainsBranchesAndSubstitutesOnlyTheReturnCount() {
        CampaignProfile profile=new CampaignProfile();
        assertEquals(4,CaronteDialogue.select(profile).lines().size());
        profile.finishConversation(CampaignProfile.Event.CARONTE_MET);profile.recordReturn(RunOutcome.DEATH,0);
        assertEquals(CampaignProfile.Event.BRIBE_JOKE,CaronteDialogue.select(profile).event());
        profile.finishConversation(CampaignProfile.Event.BRIBE_JOKE);profile.recordReturn(RunOutcome.VICTORY,0);
        assertEquals(CampaignProfile.Event.GUARDIAN_RECOGNIZED,CaronteDialogue.select(profile).event());
        profile.finishConversation(CampaignProfile.Event.GUARDIAN_RECOGNIZED);
        assertTrue(CaronteDialogue.select(profile).lines().getFirst().text().contains("2 retornos"));
        assertThrows(RuntimeException.class,()->CaronteDialogue.parse("{}"));
    }
    @Test void turningPreservesGaitPhaseAndStoppingKeepsTheLastFacing() {
        ActorAnimation motion=new ActorAnimation();
        motion.advance(.25f,1,0);assertEquals(ActorAnimation.Direction.RIGHT,motion.direction());
        int frame=motion.frame();motion.advance(0,0,1);
        assertEquals(ActorAnimation.Direction.UP,motion.direction());assertEquals(frame,motion.frame());
        float phase=motion.phase();motion.advance(5,0,0);
        assertFalse(motion.moving());assertEquals(ActorAnimation.Direction.UP,motion.direction());
        assertEquals(phase,motion.phase());assertEquals(1,motion.frame());
        motion.advance(.12f,0,-1);assertEquals(ActorAnimation.Direction.DOWN,motion.direction());
        assertTrue(motion.moving());assertTrue(motion.phase()>phase);
    }
    @Test void diagonalNoiseDoesNotSwitchFacingUntilItCrossesTheHysteresisThreshold() {
        ActorAnimation motion=new ActorAnimation();motion.advance(.1f,1,1);
        for(int i=0;i<100;i++)motion.advance(.01f,1,i%2==0?.99f:1.01f);
        assertEquals(ActorAnimation.Direction.UP_RIGHT,motion.direction());
        motion.advance(.01f,0,1);assertEquals(ActorAnimation.Direction.UP,motion.direction());
        assertThrows(IllegalArgumentException.class,()->motion.advance(-1,1,0));
        assertThrows(IllegalArgumentException.class,()->motion.advance(1,Float.NaN,0));
    }
}

