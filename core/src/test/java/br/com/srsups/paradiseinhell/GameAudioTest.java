package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameAudioTest {
    private final TestAudioBackend driver=new TestAudioBackend();
    private final AudioSettingsService settings=new AudioSettingsService(AudioSettingsStore.memory());
    private final GameAudio audio=new GameAudio(settings,driver);
    @Test void repeatedHitsAndXpUseCooldownsInsteadOfCreatingUnlimitedVoices() {
        for(int i=0;i<1000;i++){audio.play(SoundCue.HIT);audio.play(SoundCue.XP);}
        assertEquals(2,driver.played.size());assertEquals(2,audio.activeVoices());
        audio.advance(.10f);assertTrue(audio.play(SoundCue.HIT));assertEquals(3,audio.activeVoices());
        audio.advance(.3f);assertEquals(0,audio.activeVoices());assertEquals(3,driver.stopped.size());
    }
    @Test void allWeaponsHaveDistinctAttacksAndCriticalWarningsReplaceAttackVoices() {
        for(WeaponType weapon:WeaponType.values())assertTrue(audio.play(SoundCue.attack(weapon)));
        assertEquals(GameAudio.MAX_VOICES,audio.activeVoices());
        assertFalse(audio.play(SoundCue.HIT));
        assertTrue(audio.play(SoundCue.BOSS_WARNING));
        assertEquals(8,audio.activeVoices());assertEquals(1,driver.stopped.size());
        assertEquals(SoundCue.BOSS_WARNING,driver.played.getLast().cue());
    }
    @Test void volumeAppliesToActiveEffectsAndMusicAndMuteDoesNotDiscardTheChosenVolumes() {
        audio.music(AudioBackend.Track.COMBAT);audio.play(SoundCue.BLOCK);
        settings.change(new AudioSettings(.4f,.6f,false));audio.refreshVolume();
        assertEquals(.4f*SoundCue.BLOCK.gain,driver.effectVolume,.0001f);assertEquals(.6f,driver.musicVolume);
        settings.change(new AudioSettings(.4f,.6f,true));audio.refreshVolume();
        assertEquals(0,audio.activeVoices());assertEquals(0,driver.musicVolume);assertFalse(audio.play(SoundCue.XP));
        settings.change(new AudioSettings(.4f,.6f,false));audio.refreshVolume();assertEquals(.6f,driver.musicVolume);
        assertTrue(audio.play(SoundCue.BLOCK));
        assertEquals(.4f*SoundCue.BLOCK.gain,driver.played.getLast().volume(),.0001f);
    }
    @Test void voiceExpirationStopsTheExactSoundInstanceAndSwitchingScreensClearsEffects() {
        audio.play(SoundCue.ATTACK_BOW);audio.play(SoundCue.VICTORY);audio.advance(.3f);
        assertEquals(java.util.List.of(1L),driver.stopped);assertEquals(1,audio.activeVoices());
        audio.stopEffects();assertEquals(java.util.List.of(1L,2L),driver.stopped);assertEquals(0,audio.activeVoices());
        assertTrue(audio.play(SoundCue.ATTACK_BOW));
    }
    @Test void deviceFailureAndMissingAudioCannotInterruptGameplayAndDisposalIsIdempotent() {
        audio.play(SoundCue.HIT);driver.fail=true;
        assertFalse(audio.play(SoundCue.BOSS_WARNING));assertFalse(audio.available());assertEquals(0,audio.activeVoices());
        audio.dispose();audio.dispose();assertEquals(1,driver.disposals);
        driver.ready=false;
        assertFalse(new GameAudio(settings,driver).play(SoundCue.BLOCK));
        assertThrows(IllegalArgumentException.class,()->audio.advance(Float.NaN));
    }
}

