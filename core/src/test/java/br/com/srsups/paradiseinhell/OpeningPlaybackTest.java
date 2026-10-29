package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpeningPlaybackTest {
    private static OpeningPlayback cues() {
        var source=OpeningStory.script();var shots=new ArrayList<>(source.shots());var home=shots.getFirst();
        shots.set(0,new OpeningStory.Shot(home.id(),home.duration(),home.minReadSeconds(),home.startZoom(),home.endZoom(),
            home.layers(),home.subtitles(),List.of(new OpeningStory.Cue(0,SoundCue.XP),new OpeningStory.Cue(1,SoundCue.DASH),
                new OpeningStory.Cue(2,SoundCue.PASSAGE))));
        return new OpeningPlayback(new OpeningStory.Script(source.id(),source.title(),shots));
    }

    @Test void autoplayCrossesAllSixShotBoundariesAndFinishesAtFortyFiveSeconds() {
        OpeningPlayback playback=new OpeningPlayback();assertEquals(OpeningStory.ShotId.HOME,playback.current().id());
        float elapsed=0;
        for(var shot:OpeningStory.shots()) {
            assertEquals(shot.id(),playback.current().id());playback.advance(shot.duration());elapsed+=shot.duration();
            assertEquals(elapsed,playback.elapsedTotal());
        }
        assertTrue(playback.finished());assertFalse(playback.skipped());assertEquals(45,playback.elapsedTotal());
        assertEquals(1,playback.progress());assertEquals(1,playback.shotProgress());
        playback.advance(100);assertEquals(45,playback.elapsedTotal());assertFalse(playback.nextShot());
    }

    @Test void pauseFreezesTimeSubtitlesAndCueDeliveryWithoutReplayingConsumedSounds() {
        var playback=cues();assertEquals(SoundCue.XP,playback.pollSound());assertNull(playback.pollSound());
        playback.advance(1);assertEquals(SoundCue.DASH,playback.pollSound());
        playback.pause(true);var subtitle=playback.subtitle();playback.advance(100);
        assertEquals(1,playback.elapsedTotal());assertEquals(subtitle,playback.subtitle());assertNull(playback.pollSound());assertFalse(playback.next());
        playback.togglePause();playback.advance(0);assertNull(playback.pollSound());playback.advance(1);
        playback.paused(true);assertNull(playback.pollSound());playback.pause(false);
        assertEquals(SoundCue.PASSAGE,playback.pollSound());assertNull(playback.pollSound());
        playback.advance(0);assertNull(playback.pollSound());
    }

    @Test void manualNextRequiresActualShotReadingAndClearsStaleSoundCues() {
        var playback=cues();assertFalse(playback.nextShot());playback.advance(1.49f);assertFalse(playback.next());
        playback.advance(.02f);assertTrue(playback.nextShot());assertEquals(OpeningStory.ShotId.ARRIVAL,playback.current().id());
        assertEquals(0,playback.elapsedInShot());assertEquals(6,playback.elapsedTotal());assertNull(playback.pollSound());
        playback.advance(.4f);assertEquals(SoundCue.BOSS_WARNING,playback.pollSound());assertNull(playback.pollSound());
        assertFalse(playback.next());assertEquals(.4f,playback.elapsedInShot());
    }

    @Test void explicitSkipTerminatesAndDiscardsPendingAudioWithoutAdvancingAnyOtherModel() {
        var playback=cues();playback.pause(true);playback.skip();
        assertTrue(playback.finished());assertTrue(playback.skipped());assertEquals(1,playback.progress());
        playback.pause(false);assertNull(playback.pollSound());playback.advance(100);assertEquals(0,playback.elapsedInShot());
        playback.skip();assertTrue(playback.skipped());assertFalse(playback.next());
    }

    @Test void subtitleCheckpointsUseTheCurrentShotsUiClock() {
        var playback=new OpeningPlayback();assertNull(playback.subtitle());assertEquals("",playback.text());assertEquals("",playback.speaker());
        playback.advance(.3f);assertEquals("Narrador",playback.speaker());
        playback.advance(35.95f);assertEquals(OpeningStory.ShotId.CHARON,playback.current().id());assertEquals("Caronte",playback.speaker());
        playback.advance(3.25f);assertEquals("Protagonista",playback.speaker());assertEquals("Quero voltar.",playback.text());
        playback.advance(2.31f);assertEquals("Caronte",playback.speaker());assertEquals("Pelo jeito, você vai.",playback.text());
    }

    @Test void completingTheLastShotManuallyDoesNotTurnACompletedOpeningIntoASkip() {
        var playback=new OpeningPlayback();
        for(int shot=0;shot<6;shot++){playback.advance(1.5f);assertTrue(playback.nextShot());}
        assertTrue(playback.finished());assertFalse(playback.skipped());assertEquals(45,playback.elapsedTotal());
        playback.skip();assertFalse(playback.skipped());
    }

    @Test void largeUiFrameIsBoundedAndInvalidTimeCannotCorruptTheTimeline() {
        var playback=new OpeningPlayback();
        for(float invalid:new float[]{-1,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->playback.advance(invalid));
        assertEquals(0,playback.elapsedTotal());playback.advance(Float.MAX_VALUE);
        assertTrue(playback.finished());assertEquals(45,playback.elapsedTotal());assertEquals(1,playback.progress());
    }
}
