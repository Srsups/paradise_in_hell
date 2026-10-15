package br.com.srsups.paradiseinhell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cosmetic only: bounded pulses never alter collision, damage, aim, navigation or the run clock. */
public final class CombatFeedback {
    public static final int LIMIT = 96;
    public enum Kind { ATTACK, HIT, TRAIL, DASH, BLOCK, PROTECTED, RETALIATION, XP, RESURRECTION }
    public static final class Pulse {
        public final Kind kind;
        public final WeaponType weapon;
        public final float x,y,ox,oy,duration;
        public float remaining;
        private Pulse(Kind kind,WeaponType weapon,float x,float y,float ox,float oy,float duration) {
            this.kind=kind;this.weapon=weapon;this.x=x;this.y=y;this.ox=ox;this.oy=oy;this.remaining=this.duration=duration;
        }
        public float progress() { return 1-remaining/duration; }
    }
    private final GameAudio audio;
    private final List<Pulse> pulses=new ArrayList<>(LIMIT);
    private final List<Pulse> view=Collections.unmodifiableList(pulses);
    private int trailsThisFrame;
    private float hurtFlash,trailCooldown,protectionCooldown;
    public CombatFeedback(GameAudio audio) {this.audio=audio;}
    public List<Pulse> pulses(){return view;}
    public float hurtFlash(){return hurtFlash;}
    public void advance(float delta) {
        if(!Float.isFinite(delta) || delta<0)throw new IllegalArgumentException("Invalid feedback time");
        trailsThisFrame=0;
        hurtFlash=Math.max(0,hurtFlash-delta);trailCooldown=Math.max(0,trailCooldown-delta);
        protectionCooldown=Math.max(0,protectionCooldown-delta);
        pulses.removeIf(pulse->{pulse.remaining-=delta;return pulse.remaining<=0;});
    }
    private void add(Kind kind,WeaponType weapon,float x,float y,float ox,float oy,float duration) {
        if(pulses.size()>=LIMIT) {
            Pulse lowest=pulses.stream().min(java.util.Comparator.comparingInt(p->priority(p.kind))).orElseThrow();
            if(priority(kind)<priority(lowest.kind))return;
            pulses.remove(lowest);
        }
        pulses.add(new Pulse(kind,weapon,x,y,ox,oy,duration));
    }
    private static int priority(Kind kind) {return switch(kind) {
        case TRAIL,XP->0;case ATTACK,HIT->1;case DASH->2;case BLOCK,PROTECTED,RETALIATION->3;case RESURRECTION->4;
    };}
    public void attack(WeaponType weapon,float x,float y,float dx,float dy) {
        audio.play(SoundCue.attack(weapon));
        float len=Math.max(.001f,(float)Math.sqrt(dx*dx+dy*dy));
        add(Kind.ATTACK,weapon,x+dx/len*9,y+dy/len*9,dx/len,dy/len,.12f);
    }
    public void hit(WeaponType weapon,float x,float y,float actual) {
        if(actual<=0)return;
        audio.play(SoundCue.HIT);add(Kind.HIT,weapon,x,y,0,0,.18f);
    }
    public void trail(WeaponType weapon,float x,float y,float ox,float oy) {
        if(x==ox && y==oy || trailsThisFrame>=16)return;
        trailsThisFrame++;
        add(Kind.TRAIL,weapon,x,y,ox,oy,.08f);
    }
    public void dash(WeaponType weapon,float beforeX,float beforeY,float x,float y,boolean started,boolean dashing) {
        if(started)audio.play(SoundCue.DASH);
        if(dashing && trailCooldown==0 && (beforeX!=x || beforeY!=y)) {
            add(Kind.DASH,weapon,beforeX,beforeY,x,y,.18f);trailCooldown=.025f;
        }
    }
    public void receive(CombatHit hit,boolean blocked,float actual,boolean retaliated,
                        WeaponType weapon,float x,float y,float sourceX,float sourceY) {
        if(blocked) {audio.play(SoundCue.BLOCK);add(Kind.BLOCK,weapon,x,y,hit.x(),hit.y(),.24f);}
        else if(actual>0) {
            pulses.removeIf(pulse->pulse.kind==Kind.PROTECTED);
            audio.play(SoundCue.PLAYER_HURT);hurtFlash=.18f;
        }
        else if(hit.damage()>0 && protectionCooldown==0) {
            audio.play(SoundCue.IMMUNE);add(Kind.PROTECTED,weapon,x,y,0,0,.3f);protectionCooldown=.3f;
        }
        if(retaliated) {audio.play(SoundCue.RETALIATION);add(Kind.RETALIATION,weapon,x,y,sourceX,sourceY,.3f);}
    }
    public void xp(float x,float y) {audio.play(SoundCue.XP);add(Kind.XP,WeaponType.SOUL,x,y,0,0,.22f);}
    public void resurrect(float x,float y) {audio.play(SoundCue.RESURRECTION);add(Kind.RESURRECTION,WeaponType.SOUL,x,y,0,0,.6f);}
    public void clear(){pulses.clear();hurtFlash=trailCooldown=protectionCooldown=0;trailsThisFrame=0;}
}

