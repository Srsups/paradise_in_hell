package br.com.srsups.paradiseinhell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatFeedbackTest {
    private final TestAudioBackend driver=new TestAudioBackend();
    private final GameAudio audio=new GameAudio(new AudioSettingsService(AudioSettingsStore.memory()),driver);
    private final CombatFeedback feedback=new CombatFeedback(audio);
    @Test void missesDoNotCreateImpactSoundsAndWeaponColorsUseTheActualAttackType() {
        feedback.attack(WeaponType.CURSE,0,0,1,0);feedback.hit(WeaponType.CURSE,10,0,0);
        assertEquals(1,driver.played.size());assertEquals(SoundCue.ATTACK_CURSE,driver.played.getFirst().cue());
        assertEquals(1,feedback.pulses().size());assertEquals(WeaponType.CURSE,feedback.pulses().getFirst().weapon);
        feedback.hit(WeaponType.CURSE,10,0,2);assertEquals(SoundCue.HIT,driver.played.getLast().cue());
    }
    @Test void aBlockedHitCannotProduceAHurtFlashAndRetaliationHasItsOwnCue() {
        var hit=new CombatHit(8,10,0,CombatHit.Kind.PROJECTILE);
        feedback.receive(hit,true,0,true,WeaponType.SHIELD,0,0,30,0);
        assertEquals(0,feedback.hurtFlash());
        assertEquals(java.util.List.of(SoundCue.BLOCK,SoundCue.RETALIATION),driver.played.stream().map(TestAudioBackend.Played::cue).toList());
        assertTrue(feedback.pulses().stream().anyMatch(p->p.kind==CombatFeedback.Kind.RETALIATION && p.ox==30));
        feedback.receive(hit,false,4,false,WeaponType.SHIELD,0,0,30,0);
        assertTrue(feedback.hurtFlash()>0);assertEquals(SoundCue.PLAYER_HURT,driver.played.getLast().cue());
    }
    @Test void invulnerabilityIsDistinctFromARealBlockAndRepeatedContactsAreBounded() {
        var hit=new CombatHit(8,10,0,CombatHit.Kind.CONTACT);
        for(int i=0;i<100;i++)feedback.receive(hit,false,0,false,WeaponType.BOW,0,0,10,0);
        assertEquals(1,feedback.pulses().size());assertEquals(CombatFeedback.Kind.PROTECTED,feedback.pulses().getFirst().kind);
        assertEquals(SoundCue.IMMUNE,driver.played.getFirst().cue());assertEquals(0,feedback.hurtFlash());
        feedback.receive(hit,false,8,false,WeaponType.BOW,0,0,10,0);
        assertTrue(feedback.pulses().stream().noneMatch(p->p.kind==CombatFeedback.Kind.PROTECTED),
            "An expired protection label must not remain visible over a real hurt flash");
    }
    @Test void trailSpamCannotHideDefenseAndAllCosmeticsExpireAndClear() {
        var hit=new CombatHit(8,10,0,CombatHit.Kind.CONTACT);
        feedback.receive(hit,true,0,false,WeaponType.SHIELD,0,0,10,0);
        for(int frame=0;frame<4;frame++) {
            feedback.advance(.001f);
            for(int i=0;i<1000;i++)feedback.trail(WeaponType.PISTOL,i,0,i+1,0);
        }
        for(int i=0;i<1000;i++)feedback.xp(0,0);
        assertTrue(feedback.pulses().size()<=CombatFeedback.LIMIT);
        assertTrue(feedback.pulses().stream().anyMatch(p->p.kind==CombatFeedback.Kind.BLOCK));
        feedback.advance(1);assertTrue(feedback.pulses().isEmpty());feedback.resurrect(0,0);
        feedback.clear();assertTrue(feedback.pulses().isEmpty());assertEquals(0,feedback.hurtFlash());
    }
    @Test void observerReportsEffectiveDamageWithoutOverkillAndKeepsGroundPriority() {
        PlayerState player=new PlayerState(Profession.GUERREIRO);player.setGuard(true,1,0);
        CombatTarget source=new CombatTarget(){
            public float centerX(){return 10;}public float centerY(){return 0;}
            public float health(){return 100;}public float maxHealth(){return 100;}
            public float strength(){return 1;}public void hurt(float amount){}
            public void push(float x,float y,CombatEffects.LineOfSight line){}
        };
        var hits=java.util.List.of(new CombatHit(10,10,0,CombatHit.Kind.PROJECTILE),new CombatHit(10000,0,0,CombatHit.Kind.GROUND));
        java.util.List<Float> actual=new java.util.ArrayList<>();java.util.List<Boolean> blocked=new java.util.ArrayList<>();
        CombatDefense.resolve(player,hits,0,0,source,new java.util.Random(),DamageSink.DIRECT,(hit,b,damage,counter)->{actual.add(damage);blocked.add(b);});
        assertEquals(java.util.List.of(150f),actual);assertEquals(java.util.List.of(false),blocked);
        assertTrue(player.estaMorto());
    }
}

