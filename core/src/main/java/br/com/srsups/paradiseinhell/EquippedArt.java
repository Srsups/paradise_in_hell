package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import java.util.*;

/** Authored bodies include equipment. Only the central opaque body participates in contact. */
public final class EquippedArt implements com.badlogic.gdx.utils.Disposable {
    public static final float DRAW_SCALE=1.6f;
    private final TextureAtlas atlas=new TextureAtlas(Gdx.files.internal("visual/equipped.atlas"));
    private final Map<String,TextureRegion> regions=new HashMap<>();
    private final Map<String,Clip> clips=new HashMap<>();
    private record Clip(TextureRegion[] frames,SpriteHitbox.Shape[] masks,int count,int directions) {}
    public record Pose(String clip,ActorAnimation.Direction direction,int frame) {}
    public EquippedArt() {
        for(var region:atlas.getRegions())regions.put(region.name,region);
        var pixels=new IdentityHashMap<Texture,Pixmap>();
        try {
            for(Texture texture:atlas.getTextures()){
                var data=texture.getTextureData();if(!data.isPrepared())data.prepare();pixels.put(texture,data.consumePixmap());
            }
            for(Profession p:Profession.values()){
                String actor=actor(p);read(actor+"-walk",6,5,pixels);read(actor+"-idle",4,5,pixels);read(actor+"-attack",4,5,pixels);
            }
            read("archer-walk",4,8,pixels);read("archer-attack",6,8,pixels);
            read("carrasco-walk",4,8,pixels);read("carrasco-spin",6,4,pixels);read("carrasco-stones",6,4,pixels);
            read("guardian-walk",6,8,pixels);read("guardian-idle",4,8,pixels);read("guardian-charge",6,8,pixels);
            read("guardian-cast",6,5,pixels);read("guardian-land",6,4,pixels);read("guardian-jump",6,4,pixels);
        } finally {
            for(var entry:pixels.entrySet())if(entry.getKey().getTextureData().disposePixmap())entry.getValue().dispose();
        }
    }
    public static String actor(Profession profession){return profession.name().toLowerCase(Locale.ROOT);}
    /** Sprite pixels outside this torso/legs window are equipment; never enlarge body contact. */
    public static boolean bodyPixel(int x,int y,int width,int height){
        return x>=width*.30f&&x<width*.70f&&y>=height*.16f&&y<height*.86f;
    }
    private void read(String name,int count,int authoredDirections,Map<Texture,Pixmap> pixels){
        int directions=authoredDirections==5?8:authoredDirections;
        var frames=new TextureRegion[count*directions];var masks=new SpriteHitbox.Shape[frames.length];
        var forms=new HashMap<String,SpriteHitbox.Shape>();
        for(int i=0;i<frames.length;i++){
            var direction=ActorAnimation.Direction.values()[i/count];
            var authored=authoredDirections==5?direction.authored():direction;
            String key=name+"-"+authored.key()+"-"+i%count;var original=region(key);
            TextureRegion frame=original;
            if(authoredDirections==5&&direction.mirrored()){frame=new TextureRegion(original);frame.flip(true,false);}
            frames[i]=frame;Pixmap image=pixels.get(original.getTexture());
            int x=original.getRegionX(),y=original.getRegionY(),w=original.getRegionWidth(),h=original.getRegionHeight();
            masks[i]=forms.computeIfAbsent(key,ignored->SpriteHitbox.Shape.fromAlpha(w,h,
                (px,py)->bodyPixel(px,py,w,h)?image.getPixel(x+px,y+py)&255:0));
        }
        clips.put(name,new Clip(frames,masks,count,directions));
    }
    public TextureRegion prop(String name,float time){return region(name+"-"+Math.floorMod((int)(time/.28f),4));}
    public TextureRegion region(String name){
        TextureRegion result=regions.get(name);if(result==null)throw new IllegalArgumentException("Missing equipped sprite "+name);return result;
    }
    public TextureRegion effect(String name,float progress){return region(name+"-"+Math.min(7,Math.max(0,(int)(progress*8))));}
    public TextureRegion frame(String clip,ActorAnimation.Direction direction,int frame){return source(clip).frames[index(source(clip),direction,frame)];}
    private Clip source(String name){Clip clip=clips.get(name);if(clip==null)throw new IllegalArgumentException("Missing clip "+name);return clip;}
    private static int index(Clip clip,ActorAnimation.Direction direction,int frame){
        return (clip.directions==4?direction.cardinal():direction).ordinal()*clip.count+Math.floorMod(frame,clip.count);
    }
    public Body body(String actor){return new Body(this,actor);}
    public static final class Body {
        private final EquippedArt art;
        private final String actor;
        private final Map<String,SpriteHitbox[]> masks=new HashMap<>();
        private Body(EquippedArt art,String actor){
            this.art=art;this.actor=actor;
            for(var entry:art.clips.entrySet())if(entry.getKey().startsWith(actor+"-")){
                var shapes=entry.getValue().masks;var positions=new SpriteHitbox[shapes.length];
                for(int i=0;i<shapes.length;i++)positions[i]=new SpriteHitbox(shapes[i]);masks.put(entry.getKey(),positions);
            }
            if(masks.isEmpty())throw new IllegalArgumentException("Unknown equipped actor "+actor);
        }
        public Pose walk(ActorAnimation motion){return new Pose(actor+"-walk",motion.dominantDirection(),motion.frame(art.source(actor+"-walk").count));}
        public Pose pose(String action,ActorAnimation.Direction direction,float progress){
            var clip=art.source(actor+"-"+action);return new Pose(actor+"-"+action,direction,Math.min(clip.count-1,Math.max(0,(int)(progress*clip.count))));
        }
        public SpriteHitbox hitbox(Pose pose,float x,float y,float bodySize){
            Clip clip=art.source(pose.clip);float size=bodySize*DRAW_SCALE;
            return masks.get(pose.clip)[index(clip,pose.direction,pose.frame)].place(x-size/2,y-size/2,size,size,clip.frames[index(clip,pose.direction,pose.frame)].isFlipX(),false);
        }
        public SpriteHitbox hitbox(ActorAnimation motion,float x,float y,float size){return hitbox(walk(motion),x,y,size);}
        public void draw(SpriteBatch batch,Pose pose,float x,float y,float bodySize){
            float size=bodySize*DRAW_SCALE;batch.draw(art.frame(pose.clip,pose.direction,pose.frame),x-size/2,y-size/2,size,size);
        }
        public void draw(SpriteBatch batch,Pose pose,ActorAnimation facing,float x,float y,float bodySize){
            var c=batch.getColor();float r=c.r,g=c.g,b=c.b,a=c.a,blend=facing.turnBlend();
            if(blend<1){batch.setColor(r,g,b,a*(1-blend));draw(batch,new Pose(pose.clip,facing.previousDirection(),pose.frame),x,y,bodySize);}
            batch.setColor(r,g,b,a*blend);draw(batch,new Pose(pose.clip,facing.direction(),pose.frame),x,y,bodySize);batch.setColor(r,g,b,a);
        }
        public void draw(SpriteBatch batch,ActorAnimation motion,float x,float y,float bodySize){
            var clip=art.source(actor+"-walk");int index=motion.frame(clip.count);
            var c=batch.getColor();float r=c.r,g=c.g,b=c.b,a=c.a,blend=motion.turnBlend();
            if(blend<1){batch.setColor(r,g,b,a*(1-blend));draw(batch,new Pose(actor+"-walk",motion.previousDirection(),index),x,y,bodySize);}
            batch.setColor(r,g,b,a*blend);draw(batch,new Pose(actor+"-walk",motion.direction(),index),x,y,bodySize);batch.setColor(r,g,b,a);
        }
    }
    @Override public void dispose(){atlas.dispose();regions.clear();clips.clear();}
}

