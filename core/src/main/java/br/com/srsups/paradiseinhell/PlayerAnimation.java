package br.com.srsups.paradiseinhell;

/** Cosmetic clocks only. Sampling a pose cannot fire a weapon or change combat cooldowns. */
public final class PlayerAnimation {
    public static final float WALK_FRAME_SECONDS=.135f,IDLE_FRAME_SECONDS=.40f,TRANSITION_SECONDS=.08f;
    private final String actor;
    private float elapsed,remaining,attackDuration,transition=TRANSITION_SECONDS;
    private EquippedArt.Pose current,previous;
    public PlayerAnimation(Profession profession){
        actor=EquippedArt.actor(profession);current=previous=new EquippedArt.Pose(actor+"-idle",ActorAnimation.Direction.DOWN,0);
    }
    public void advance(float delta){
        if(!Float.isFinite(delta)||delta<0)throw new IllegalArgumentException("Invalid animation time");
        elapsed=(elapsed+delta)%3600;remaining=Math.max(0,remaining-delta);transition=Math.min(TRANSITION_SECONDS,transition+delta);
    }
    public void attack(float attacksPerSecond){
        if(!Float.isFinite(attacksPerSecond)||attacksPerSecond<=0)throw new IllegalArgumentException("Invalid attack rate");
        attackDuration=Math.max(.20f,Math.min(.48f,.72f/attacksPerSecond));remaining=attackDuration;
    }
    public boolean attacking(){return remaining>0;}
    public float attackDuration(){return attackDuration;}
    public float blend(){return Math.min(1,transition/TRANSITION_SECONDS);}
    public EquippedArt.Pose current(){return current;}
    public EquippedArt.Pose previous(){return previous;}
    public EquippedArt.Pose dominant(){return blend()<.5f?previous:current;}
    public void select(ActorAnimation motion,ActorAnimation.Direction facing,boolean guarding){
        String action;int frame;
        if(attacking()||guarding){action="attack";frame=attacking()?Math.min(3,(int)((1-remaining/attackDuration)*4)):0;}
        else if(motion.moving()){action="walk";frame=motion.frame(6,WALK_FRAME_SECONDS);}
        else {action="idle";frame=(int)(elapsed/IDLE_FRAME_SECONDS)%4;}
        var next=new EquippedArt.Pose(actor+"-"+action,facing,frame);
        if(!next.clip().equals(current.clip())||next.direction()!=current.direction()){
            previous=dominant();transition=0;
        }
        current=next;
    }
}

