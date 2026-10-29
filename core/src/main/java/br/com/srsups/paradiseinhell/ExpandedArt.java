package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.*;
import java.util.*;

/** Current roster. Frames and alpha shapes load once; placement belongs to each actor. */
public final class ExpandedArt implements com.badlogic.gdx.utils.Disposable {
    public static final String[] ENEMIES={"common","fast","tank","archer","carrasco","guardian"};
    private final TextureAtlas atlas=new TextureAtlas(Gdx.files.internal("visual/expanded.atlas"));
    private final TextureAtlas milestoneFour=new TextureAtlas(Gdx.files.internal("visual/m4.atlas"));
    private final TextureAtlas dialoguePortraits=new TextureAtlas(Gdx.files.internal("visual/m6-dialogue.atlas"));
    private final FarmerArt farmer=new FarmerArt();
    public FarmerArt farmer(){return farmer;}
    private final Map<String,TextureRegion> regions=new HashMap<>();
    private final Map<String,Frames> actors=new HashMap<>();
    private record Frames(TextureRegion[] frames,SpriteHitbox.Shape[] masks,int count) {}
    public ExpandedArt() {
        for(var region:atlas.getRegions())regions.put(region.name,region);
        for(var region:milestoneFour.getRegions())regions.put(region.name,region);
        for(var region:dialoguePortraits.getRegions())regions.put(region.name,region);
        farmer.appendTo(regions);farmer.appendLegacyTo(regions);
        var pixels=new IdentityHashMap<com.badlogic.gdx.graphics.Texture,Pixmap>();
        try {
            for(var texture:atlas.getTextures()){
                var data=texture.getTextureData();if(!data.isPrepared())data.prepare();pixels.put(texture,data.consumePixmap());
            }
            for(var texture:farmer.atlas().getTextures()){
                var data=texture.getTextureData();if(!data.isPrepared())data.prepare();pixels.put(texture,data.consumePixmap());
            }
            for(Profession profession:Profession.values())readActor(actor(profession),6,pixels);
            for(String enemy:ENEMIES)readActor(enemy,4,pixels);
        } finally {for(var entry:pixels.entrySet())if(entry.getKey().getTextureData().disposePixmap())entry.getValue().dispose();}
    }
    public static String actor(Profession profession) { return "class-"+profession.name().toLowerCase(Locale.ROOT); }
    private void readActor(String name,int count,Map<com.badlogic.gdx.graphics.Texture,Pixmap> pixels) {
        TextureRegion[] frames=new TextureRegion[4*count];SpriteHitbox.Shape[] shapes=new SpriteHitbox.Shape[frames.length];
        for(int i=0;i<frames.length;i++) {
            var frame=frames[i]=region(name+"-"+ActorAnimation.Direction.values()[i/count].name().toLowerCase(Locale.ROOT)+"-"+i%count);
            int x=frame.getRegionX()-(frame.isFlipX()?frame.getRegionWidth():0),y=frame.getRegionY();
            Pixmap source=pixels.get(frame.getTexture());
            shapes[i]=SpriteHitbox.Shape.fromAlpha(frame.getRegionWidth(),frame.getRegionHeight(),(px,py)->source.getPixel(x+px,y+py)&255);
        }
        actors.put(name,new Frames(frames,shapes,count));
    }
    public TextureRegion region(String name) {
        TextureRegion region=regions.get(name);if(region==null)throw new IllegalArgumentException("Missing expanded sprite "+name);return region;
    }
    public Body body(String name) {
        Frames frames=actors.get(name);if(frames==null)throw new IllegalArgumentException("Missing actor "+name);return new Body(frames);
    }
    public TextureRegion frame(String name,int direction,int index) {
        Frames frames=actors.get(name);return frames.frames[direction*frames.count+index];
    }
    public TextureRegion effect(String name,float progress) {
        String actual=switch(name){case "attack-rake"->"fx-rake";case "attack-scythe"->"fx-scythe";default->name;};
        return region(actual+"-"+Math.min(3,Math.max(0,(int)(progress*4))));
    }
    public TextureRegion boon(Upgrade boon) {return region(boon==null?"boon-currency":"boon-"+boon.name().toLowerCase(Locale.ROOT));}
    public static final class Body {
        private final Frames source;
        private final SpriteHitbox[] masks;
        private Body(Frames source) {
            this.source=source;masks=new SpriteHitbox[source.masks.length];
            for(int i=0;i<masks.length;i++)masks[i]=new SpriteHitbox(source.masks[i]);
        }
        public SpriteHitbox hitbox(ActorAnimation motion,float x,float y,float size) {
            int index=motion.dominantDirection().cardinal().ordinal()*source.count+motion.frame(source.count);
            return masks[index].place(x-size/2,y-size/2,size,size,source.frames[index].isFlipX(),false);
        }
        public void draw(SpriteBatch batch,ActorAnimation motion,float x,float y,float size) {
            var color=batch.getColor();float r=color.r,g=color.g,b=color.b,a=color.a,blend=motion.turnBlend();
            int index=motion.frame(source.count);
            if(blend<1) {
                batch.setColor(r,g,b,a*(1-blend));
                batch.draw(source.frames[motion.previousDirection().cardinal().ordinal()*source.count+index],x-size/2,y-size/2,size,size);
            }
            batch.setColor(r,g,b,a*blend);
            batch.draw(source.frames[motion.direction().cardinal().ordinal()*source.count+index],x-size/2,y-size/2,size,size);
            batch.setColor(r,g,b,a);
        }
    }
    @Override public void dispose() {atlas.dispose();milestoneFour.dispose();dialoguePortraits.dispose();farmer.dispose();actors.clear();regions.clear();}
}

