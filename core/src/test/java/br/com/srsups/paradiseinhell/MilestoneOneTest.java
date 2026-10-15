package br.com.srsups.paradiseinhell;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MilestoneOneTest {
    @Test void xpBarKeepsFractionAndOverflowWithoutChangingProgress() {
        RunSession run=RunSession.demo();run.applyUpgrade(Upgrade.INTELIGENCIA);
        run.player().ganharXP(1);
        assertEquals(.25f,CombatHud.xpFraction(run.player()));
        assertEquals(2.5f,run.player().getXpAtual());
        run.player().ganharXP(12);
        assertEquals(1,CombatHud.xpFraction(run.player()));
        while(run.player().podeSubirDeNivel())run.player().subirDeNivel();
        assertEquals(3,run.player().getNivel());assertEquals(7.5f,run.player().getXpAtual());
        assertEquals(7.5f/22,CombatHud.xpFraction(run.player()));
        assertEquals(0,CombatHud.xpFraction(run.newAttempt().player()));
    }
    @Test void dashIndicatorMatchesTheActualActionAndRecoversWithoutAnotherInput() {
        PlayerState player=new PlayerState(Profession.GUERREIRO);
        assertTrue(player.canDash());player.setGuard(true,1,0);
        assertFalse(player.canDash());assertEquals("Solte o escudo",CombatHud.dashNotice(player));
        player.setGuard(false,1,0);player.tryDash();
        assertFalse(player.canDash());assertEquals(1,player.dashCooldownRemaining());
        player.advance(.2f);
        assertFalse(player.canDash());assertEquals("Dash recarregando",CombatHud.dashNotice(player));
        player.advance(.8f);assertTrue(player.canDash());assertEquals("Dash pronto",CombatHud.dashNotice(player));
    }
    @Test void visibilityAllowsPartialSpritesAndFollowsCameraSizeAndZoomRatherThanDistance() {
        assertTrue(VisibleArea.intersects(100,100,480,270,338,80,32,32));
        assertFalse(VisibleArea.intersects(100,100,480,270,340,80,32,32));
        assertTrue(VisibleArea.intersects(100,100,480,270,-170,80,32,32));
        assertFalse(VisibleArea.intersects(100,100,480,270,-172,80,32,32));
        assertFalse(VisibleArea.intersects(100,100,480,270,80,235,32,32));
        assertTrue(VisibleArea.intersects(100,100,960,540,340,80,32,32));
        assertFalse(VisibleArea.intersects(100,100,240,135,338,80,32,32));
        assertFalse(VisibleArea.intersects(100,100,0,270,80,80,32,32));
    }
    @Test void transparentBordersDoNotMakeAnOffscreenBodyAudibleAndFlipsUseTheDrawnFrame() {
        var mask=SpriteHitbox.Shape.fromAlpha(8,8,(x,y)->x>=4 && y>=4?255:0);
        SpriteHitbox sprite=new SpriteHitbox(mask).place(0,0,8,8,false,false);
        assertFalse(sprite.visibleWithin(0,4,4,8));
        assertFalse(sprite.visibleWithin(0,0,4,4));
        assertTrue(sprite.visibleWithin(4,0,5,1));
        sprite.place(0,0,8,8,true,false);
        assertTrue(sprite.visibleWithin(0,0,1,1));
        assertFalse(sprite.visibleWithin(4,0,8,8));
    }
    @Test void stoppingOneWorldSourceKeepsOtherWarningsPlayerSoundsAndMusic() {
        TestAudioBackend backend=new TestAudioBackend();
        GameAudio audio=new GameAudio(new AudioSettingsService(AudioSettingsStore.memory()),backend);
        Object carrasco=new Object(),guardiao=new Object();
        audio.music(AudioBackend.Track.COMBAT);
        assertTrue(audio.play(SoundCue.BOSS_WARNING,carrasco));audio.advance(.4f);
        assertTrue(audio.play(SoundCue.BOSS_WARNING,guardiao));assertTrue(audio.play(SoundCue.PLAYER_HURT));
        audio.stopEmitter(carrasco);
        assertEquals(List.of(1L),backend.stopped);assertEquals(2,audio.activeVoices());
        assertEquals(AudioBackend.Track.COMBAT,backend.track);
        audio.stopEmitter(carrasco);assertEquals(2,audio.activeVoices());
        audio.advance(1);assertEquals(0,audio.activeVoices());audio.dispose();
    }
}

