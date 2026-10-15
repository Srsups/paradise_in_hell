package br.com.srsups.paradiseinhell;

import java.util.Locale;

/** Rate limits, priority, amplitude and clip duration are bounded independently of attack speed. */
public enum SoundCue {
    ATTACK_BOW(.12f,.22f,1,.5f), ATTACK_SWORD(.12f,.25f,1,.5f),
    ATTACK_PISTOL(.10f,.14f,1,.35f), ATTACK_FISTS(.10f,.18f,1,.5f),
    ATTACK_WAND(.12f,.32f,1,.45f), ATTACK_CURSE(.15f,.40f,1,.45f),
    ATTACK_SOUL(.12f,.35f,1,.45f), ATTACK_SHIELD(.12f,.28f,1,.5f),
    HIT(.08f,.18f,1,.4f), BLOCK(.08f,.28f,2,.65f), IMMUNE(.25f,.25f,1,.35f),
    RETALIATION(.15f,.35f,2,.65f), DASH(.12f,.25f,2,.5f), XP(.09f,.22f,0,.35f),
    PLAYER_HURT(.20f,.35f,3,.7f), BOSS_WARNING(.35f,.65f,4,.75f),
    ARCHER_WARNING(.35f,.30f,2,.45f), RED_LOCK(.35f,.55f,4,.8f),
    LEVEL_UP(.25f,.70f,3,.65f), CHEST(.15f,.50f,2,.6f),
    PASSAGE(.5f,.80f,3,.6f), RESURRECTION(.5f,1.2f,4,.7f),
    DEATH(1,1.4f,5,.75f), VICTORY(1,1.8f,5,.7f);
    public final float interval, duration, gain;
    public final int priority;
    SoundCue(float interval,float duration,int priority,float gain) {
        this.interval=interval; this.duration=duration; this.priority=priority; this.gain=gain;
    }
    public String asset() { return "audio/" + name().toLowerCase(Locale.ROOT) + ".wav"; }
    public static SoundCue attack(WeaponType weapon) { return switch(weapon) {
        case BOW -> ATTACK_BOW; case SWORD -> ATTACK_SWORD; case PISTOL -> ATTACK_PISTOL;
        case FISTS -> ATTACK_FISTS; case WAND -> ATTACK_WAND; case CURSE -> ATTACK_CURSE;
        case SOUL -> ATTACK_SOUL; case SHIELD -> ATTACK_SHIELD;
    }; }
}

