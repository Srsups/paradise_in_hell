package br.com.srsups.paradiseinhell;

import java.util.Objects;

/** Logical HUD geometry. Scaling never changes the world, viewport or pointer coordinates. */
public final class HudLayout {
    public static final float WIDTH=960,HEIGHT=540,LEFT=14,RIGHT=946,TOP=526;
    public enum Anchor { LEFT,RIGHT }
    public record Bounds(float x,float y,float width,float height) {
        public Bounds {
            if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(width)||!Float.isFinite(height)||width<=0||height<=0)
                throw new IllegalArgumentException("Invalid HUD bounds");
        }
        public float right(){return x+width;}
        public float top(){return y+height;}
        public boolean overlaps(Bounds other) {
            Objects.requireNonNull(other);
            return x<other.right()&&right()>other.x&&y<other.top()&&top()>other.y;
        }
    }
    public static final Bounds PLAYER=new Bounds(14,432,250,94);
    public static final Bounds GOALS=new Bounds(594,484,352,42);
    public static final Bounds TIMER=new Bounds(817,491,129,35);
    public static final Bounds BOSS=new Bounds(524,484,422,42);
    public static final Bounds WARNING=new Bounds(524,432,422,42);

    private HudLayout() {}
    public static float anchorX(Anchor anchor){return Objects.requireNonNull(anchor)==Anchor.LEFT?LEFT:RIGHT;}
    public static float anchorY(){return TOP;}
    public static Bounds scaled(HudSettings settings,Anchor anchor,Bounds original) {
        Objects.requireNonNull(original);
        float scale=Objects.requireNonNull(settings).scale(),ax=anchorX(anchor);
        return new Bounds(ax+(original.x-ax)*scale,TOP+(original.y-TOP)*scale,original.width*scale,original.height*scale);
    }
    public static Bounds player(HudSettings settings){return scaled(settings,Anchor.LEFT,PLAYER);}
    public static Bounds goals(HudSettings settings,boolean demo){return scaled(settings,Anchor.RIGHT,demo?TIMER:GOALS);}
    public static Bounds boss(HudSettings settings){return scaled(settings,Anchor.RIGHT,BOSS);}
    public static Bounds warning(HudSettings settings){return scaled(settings,Anchor.RIGHT,WARNING);}
}
