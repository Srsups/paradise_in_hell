package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;

/** Bounded visual remnants; never retain combat targets, collision, loot or resurrection callbacks. */
public final class DeathVisuals {
    public static final int MAX_CORPSES=48;
    public static final float FADE_SECONDS=.25f,LIFETIME=ActorReaction.DEATH_SECONDS+FADE_SECONDS;
    public static final class Corpse {
        public final String actor;
        public final ActorAnimation.Direction direction;
        public final float x,y,bodySize;
        private float age;
        private Corpse(String actor,ActorAnimation.Direction direction,float x,float y,float bodySize) {
            this.actor=actor;this.direction=direction;this.x=x;this.y=y;this.bodySize=bodySize;
        }
        public float age(){return age;}
        public float progress(){return Math.min(1,age/ActorReaction.DEATH_SECONDS);}
        public float opacity(){return Math.min(1,Math.max(0,(LIFETIME-age)/FADE_SECONDS));}
    }
    private final List<Corpse> corpses=new ArrayList<>();
    public void add(String actor,ActorAnimation.Direction direction,float x,float y,float bodySize) {
        if(actor==null||actor.isBlank()||direction==null||!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(bodySize)||bodySize<=0)
            throw new IllegalArgumentException("Invalid death visual");
        if(corpses.size()==MAX_CORPSES)corpses.removeFirst();
        corpses.add(new Corpse(actor,direction,x,y,bodySize));
    }
    public void advance(float delta) {
        if(!Float.isFinite(delta)||delta<0)throw new IllegalArgumentException("Invalid death visual time");
        for(Corpse corpse:corpses)corpse.age+=delta;
        corpses.removeIf(corpse->corpse.age>=LIFETIME);
    }
    public List<Corpse> corpses(){return List.copyOf(corpses);}
    public int size(){return corpses.size();}
    public void clear(){corpses.clear();}
    public void draw(SpriteBatch batch,ReactionArt art) {
        var color=batch.getColor();float r=color.r,g=color.g,b=color.b,a=color.a;
        for(Corpse corpse:corpses) {
            batch.setColor(r,g,b,a*corpse.opacity());
            art.draw(batch,corpse.actor,ActorReaction.State.DEATH,corpse.direction,corpse.progress(),corpse.x,corpse.y,corpse.bodySize);
        }
        batch.setColor(r,g,b,a);
    }
}
