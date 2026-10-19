package br.com.srsups.paradiseinhell;

/** Eight logical facings, independent aim, angular hysteresis and continuous gait. */
public final class ActorAnimation {
    public enum Direction {
        DOWN(-90), UP(90), RIGHT(0), LEFT(180), DOWN_RIGHT(-45), DOWN_LEFT(-135), UP_RIGHT(45), UP_LEFT(135);
        public final float angle;
        Direction(float angle) { this.angle=angle; }
        public Direction cardinal() {
            return switch(this) {case DOWN_RIGHT,DOWN_LEFT->DOWN;case UP_RIGHT,UP_LEFT->UP;default->this;};
        }
        /** Five authored directions; right views are exact horizontal mirrors. */
        public Direction authored() {
            return switch(this){case RIGHT->LEFT;case UP_RIGHT->UP_LEFT;case DOWN_RIGHT->DOWN_LEFT;default->this;};
        }
        public boolean mirrored(){return this!=authored();}
        public String key() { return name().toLowerCase(java.util.Locale.ROOT).replace('_','-'); }
    }
    private Direction direction=Direction.DOWN,previous=Direction.DOWN;
    private float phase,turn=TURN_SECONDS;
    private boolean moving;
    public static final float TURN_SECONDS=.10f;
    private static final float SECTOR_HALF=22.5f,HYSTERESIS=5;
    public static Direction facing(float dx,float dy) {
        if(!Float.isFinite(dx)||!Float.isFinite(dy))throw new IllegalArgumentException("Invalid facing");
        if(dx*dx+dy*dy<.000001f)return Direction.DOWN;
        float angle=(float)Math.toDegrees(Math.atan2(dy,dx));Direction nearest=Direction.DOWN;float best=361;
        for(Direction d:Direction.values()){float distance=distance(angle,d.angle);if(distance<best){best=distance;nearest=d;}}
        return nearest;
    }
    private static float distance(float a,float b) {
        float difference=Math.abs(a-b)%360;return Math.min(difference,360-difference);
    }
    public float turnBlend(){return Math.min(1,turn/TURN_SECONDS);}
    public Direction previousDirection(){return previous;}
    public Direction dominantDirection(){return turnBlend()<.5f?previous:direction;}
    public int frame(int count,float seconds){if(count<1||!Float.isFinite(seconds)||seconds<=0)throw new IllegalArgumentException("Invalid animation cadence");return moving?(int)(phase/seconds)%count:Math.min(1,count-1);}
    public int frame(int count){if(count<1)throw new IllegalArgumentException("Invalid frame count");return moving?(int)(phase/.09f)%count:Math.min(1,count-1);}
    public Direction direction(){return direction;}
    public float phase(){return phase;}
    public boolean moving(){return moving;}
    public void advance(float delta,float dx,float dy) {
        if(!Float.isFinite(delta)||delta<0||!Float.isFinite(dx)||!Float.isFinite(dy))
            throw new IllegalArgumentException("Invalid animation motion");
        turn=Math.min(TURN_SECONDS,turn+delta);
        moving=dx*dx+dy*dy>.000001f;
        if(!moving)return;
        float angle=(float)Math.toDegrees(Math.atan2(dy,dx));
        if(distance(angle,direction.angle)>SECTOR_HALF+HYSTERESIS) {
            Direction target=facing(dx,dy);
            if(target!=direction){previous=dominantDirection();direction=target;turn=0;}
        }
        phase=(phase+delta)%3600;
    }
    public int frame(){return moving?(int)(phase/.12f)%4:1;}
    /** Compatibility with the historical four-direction pilot. */
    public String frameName(String actor){return actor+"-"+direction.cardinal().name().toLowerCase(java.util.Locale.ROOT)+"-"+frame();}
}
