package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.HashMap;
import java.util.Map;

/** Authored reactions share one atlas. Living bodies own their mutable hitbox placements. */
public final class ReactionArt implements com.badlogic.gdx.utils.Disposable {
    public static final String[] ACTORS={"cacador","ferreiro","vigilante","apostador","feiticeira","sacerdote",
        "necromante","guerreiro","farmer-rake","farmer-scythe","common","fast","tank","archer","carrasco","guardian"};
    private final TextureAtlas atlas=new TextureAtlas(Gdx.files.internal("visual/m6-reactions.atlas"));
    private final Map<String,Frames> clips=new HashMap<>();
    private record Frames(TextureRegion[] frames,SpriteHitbox.Shape[] masks,int count) {}

    public ReactionArt() {
        var pixels=new java.util.IdentityHashMap<Texture,Pixmap>();
        try {
            for(Texture texture:atlas.getTextures()) {
                var data=texture.getTextureData();if(!data.isPrepared())data.prepare();pixels.put(texture,data.consumePixmap());
            }
            for(String actor:ACTORS)for(ActorReaction.State state:new ActorReaction.State[]{ActorReaction.State.HURT,ActorReaction.State.DEATH}) {
                int count=count(state);var frames=new TextureRegion[count*8];
                var masks=state==ActorReaction.State.HURT?new SpriteHitbox.Shape[frames.length]:null;
                var shapes=new HashMap<String,SpriteHitbox.Shape>();
                for(var direction:ActorAnimation.Direction.values())for(int frame=0;frame<count;frame++) {
                    String name=actor+"-"+action(state)+"-"+direction.authored().key()+"-"+frame;
                    var region=atlas.findRegion(name);
                    if(region==null)throw new IllegalStateException("Missing reaction sprite "+name);
                    TextureRegion sampled=region;
                    if(direction.mirrored()){sampled=new TextureRegion(region);sampled.flip(true,false);}
                    int index=direction.ordinal()*count+frame;frames[index]=sampled;
                    if(masks!=null) {
                        Pixmap source=pixels.get(region.getTexture());
                        boolean unarmed=actor.equals("common")||actor.equals("fast")||actor.equals("tank");
                        masks[index]=shapes.computeIfAbsent(name,ignored->SpriteHitbox.Shape.fromAlpha(region.getRegionWidth(),region.getRegionHeight(),
                            (x,y)->unarmed||EquippedArt.bodyPixel(x,y,region.getRegionWidth(),region.getRegionHeight())
                                ? source.getPixel(region.getRegionX()+x,region.getRegionY()+y)&255:0));
                    }
                }
                clips.put(actor+"-"+action(state),new Frames(frames,masks,count));
            }
        } finally {for(var entry:pixels.entrySet())if(entry.getKey().getTextureData().disposePixmap())entry.getValue().dispose();}
    }
    private static String action(ActorReaction.State state) {
        return switch(state){case HURT->"hurt";case DEATH->"death";case NONE->throw new IllegalArgumentException("No reaction pose");};
    }
    public static int count(ActorReaction.State state){return switch(state){case HURT->3;case DEATH->4;case NONE->0;};}
    private Frames clip(String actor,ActorReaction.State state) {
        Frames result=clips.get(actor+"-"+action(state));
        if(result==null)throw new IllegalArgumentException("Unknown reaction actor "+actor);return result;
    }
    private static int index(Frames clip,ActorAnimation.Direction direction,int frame) {
        if(frame<0||frame>=clip.count)throw new IllegalArgumentException("Invalid reaction frame");
        return direction.ordinal()*clip.count+frame;
    }
    public TextureRegion frame(String actor,ActorReaction.State state,ActorAnimation.Direction direction,int frame) {
        Frames clip=clip(actor,state);return clip.frames[index(clip,direction,frame)];
    }
    private static int frame(ActorReaction.State state,float progress) {
        if(!Float.isFinite(progress))throw new IllegalArgumentException("Invalid reaction progress");
        return Math.min(count(state)-1,Math.max(0,(int)(progress*count(state))));
    }
    public void draw(SpriteBatch batch,String actor,ActorReaction.State state,ActorAnimation.Direction direction,float progress,float x,float y,float bodySize) {
        if(state==ActorReaction.State.NONE)return;
        float size=bodySize*EquippedArt.DRAW_SCALE;
        batch.draw(frame(actor,state,direction,frame(state,progress)),x-size/2,y-size/2,size,size);
    }
    public SpriteHitbox hitbox(String actor,ActorReaction.State state,ActorAnimation.Direction direction,int frame,float x,float y,float bodySize) {
        if(state!=ActorReaction.State.HURT)throw new IllegalArgumentException("Only living hurt poses have contact");
        Frames clip=clip(actor,state);int index=index(clip,direction,frame);
        return place(new SpriteHitbox(clip.masks[index]),clip.frames[index],x,y,bodySize);
    }
    private static SpriteHitbox place(SpriteHitbox hitbox,TextureRegion region,float x,float y,float bodySize) {
        float size=bodySize*EquippedArt.DRAW_SCALE;return hitbox.place(x-size/2,y-size/2,size,size,region.isFlipX(),false);
    }
    public Body body(String actor){return new Body(actor);}
    public final class Body {
        private final String actor;
        private final SpriteHitbox[] hurt;
        private Body(String actor) {
            this.actor=actor;Frames clip=clip(actor,ActorReaction.State.HURT);hurt=new SpriteHitbox[clip.masks.length];
            for(int i=0;i<hurt.length;i++)hurt[i]=new SpriteHitbox(clip.masks[i]);
        }
        public SpriteHitbox hitbox(ActorReaction reaction,ActorAnimation.Direction direction,float x,float y,float bodySize) {
            Frames clip=clip(actor,ActorReaction.State.HURT);int index=index(clip,direction,reaction.frame(clip.count));
            return place(hurt[index],clip.frames[index],x,y,bodySize);
        }
        public void draw(SpriteBatch batch,ActorReaction reaction,ActorAnimation.Direction direction,float x,float y,float bodySize) {
            ReactionArt.this.draw(batch,actor,reaction.state(),direction,reaction.progress(),x,y,bodySize);
        }
    }
    @Override public void dispose(){atlas.dispose();clips.clear();}
}
