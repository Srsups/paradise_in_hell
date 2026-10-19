package br.com.srsups.paradiseinhell;

/** Read-only presentation of encounter clocks. Selecting a pose cannot resolve damage. */
public final class EncounterAnimation {
    private EncounterAnimation(){}
    private static EquippedArt.Pose pose(String clip,ActorAnimation.Direction direction,int frames,float progress){
        return new EquippedArt.Pose(clip,direction,Math.max(0,Math.min(frames-1,(int)(progress*frames))));
    }
    private static ActorAnimation.Direction facing(float angle){
        return ActorAnimation.facing((float)Math.cos(angle),(float)Math.sin(angle));
    }
    public static EquippedArt.Pose boss(BossEncounter boss,ActorAnimation motion) {
        var direction=facing(boss.aimAngle());
        if(boss.state()==BossEncounter.State.RECOVERY&&boss.attack()==BossEncounter.Attack.CHARGE&&boss.recoveryElapsed()<.20f)
            return new EquippedArt.Pose("guardian-charge",direction,5);
        if(boss.state()==BossEncounter.State.INTRO||boss.state()==BossEncounter.State.RECOVERY)
            return pose("guardian-idle",motion.direction(),4,(boss.elapsed()/1.4f)%1);
        if(boss.attack()==BossEncounter.Attack.LEAP){
            if(boss.airborne()){
                float elapsed=BossEncounter.LEAP_DURATION-boss.leapSecondsRemaining();
                if(elapsed<.18f)return pose("guardian-jump",direction,6,elapsed/.18f);
                return pose("guardian-land",direction,6,0);
            }
            return pose("guardian-land",direction,6,1f/6+boss.activeProgress()*5f/6);
        }
        if(boss.attack()==BossEncounter.Attack.CHARGE){
            if(boss.state()==BossEncounter.State.TELEGRAPH)return pose("guardian-charge",direction,6,4f/6);
            float progress=boss.activeProgress();
            int frame=progress<.75f?(int)(boss.elapsed()/.065f)%4:progress<.9f?4:5;
            return new EquippedArt.Pose("guardian-charge",direction,frame);
        }
        float progress=boss.state()==BossEncounter.State.TELEGRAPH?boss.warningProgress()*4f/6:4f/6+boss.activeProgress()*2f/6;
        return pose("guardian-cast",direction,6,progress);
    }
    public static EquippedArt.Pose mini(MiniBossEncounter mini,ActorAnimation motion) {
        if(mini.state()==MiniBossEncounter.State.RECOVERY)
            return new EquippedArt.Pose("carrasco-walk",motion.dominantDirection(),motion.frame(4));
        var direction=facing(mini.aimAngle());
        if(mini.attack()==MiniBossEncounter.Attack.SPIN)
            return pose("carrasco-spin",direction,6,mini.state()==MiniBossEncounter.State.TELEGRAPH?mini.warningProgress()*2f/6:mini.activeProgress());
        float progress=mini.state()==MiniBossEncounter.State.TELEGRAPH?mini.warningProgress()*4f/6:4f/6+mini.activeProgress()*2f/6;
        return pose("carrasco-stones",direction,6,progress);
    }
}

