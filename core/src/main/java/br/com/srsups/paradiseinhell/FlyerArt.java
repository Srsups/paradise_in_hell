package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Authored wingbeats and central-body contact, shared by every Stygian bat. */
public final class FlyerArt implements com.badlogic.gdx.utils.Disposable {
    public static final int FLIGHT_FRAMES=6;
    public static final float FRAME_SECONDS=.11f;
    private final TextureAtlas atlas=new TextureAtlas(Gdx.files.internal("visual/m7-flyer.atlas"));
    private final TextureRegion[] frames=new TextureRegion[8*FLIGHT_FRAMES];
    private final SpriteHitbox.Shape[] masks=new SpriteHitbox.Shape[frames.length];
    public FlyerArt() {
        var pixels=new java.util.IdentityHashMap<com.badlogic.gdx.graphics.Texture,Pixmap>();
        try {
            for(var texture:atlas.getTextures()) {
                var data=texture.getTextureData();if(!data.isPrepared())data.prepare();pixels.put(texture,data.consumePixmap());
            }
            for(var direction:ActorAnimation.Direction.values())for(int index=0;index<FLIGHT_FRAMES;index++) {
                TextureRegion source=atlas.findRegion("flyer-walk-"+direction.authored().key()+"-"+index);
                if(source==null)throw new IllegalStateException("Missing bat flight frame");
                TextureRegion frame=source;
                if(direction.mirrored()){frame=new TextureRegion(source);frame.flip(true,false);}
                int offset=direction.ordinal()*FLIGHT_FRAMES+index;frames[offset]=frame;
                Pixmap image=pixels.get(source.getTexture());
                masks[offset]=SpriteHitbox.Shape.fromAlpha(source.getRegionWidth(),source.getRegionHeight(),
                    (x,y)->bodyPixel(x,y,source.getRegionWidth(),source.getRegionHeight())
                        ?image.getPixel(source.getRegionX()+x,source.getRegionY()+y)&255:0);
            }
        } finally {for(var entry:pixels.entrySet())if(entry.getKey().getTextureData().disposePixmap())entry.getValue().dispose();}
    }
    /** Wings remain cosmetic; visible fur/head define contact and projectile impact. */
    public static boolean bodyPixel(int x,int y,int width,int height) {
        return x>=width*.34f&&x<width*.66f&&y>=height*.18f&&y<height*.83f;
    }
    TextureAtlas atlas(){return atlas;}
    public TextureRegion frame(ActorAnimation.Direction direction,int index){return frames[direction.ordinal()*FLIGHT_FRAMES+Math.floorMod(index,FLIGHT_FRAMES)];}
    public Body body(){return new Body();}
    public final class Body {
        private final SpriteHitbox[] hitboxes=new SpriteHitbox[masks.length];
        private Body(){for(int i=0;i<hitboxes.length;i++)hitboxes[i]=new SpriteHitbox(masks[i]);}
        private int index(float elapsed){return Math.floorMod((int)(elapsed/FRAME_SECONDS),FLIGHT_FRAMES);}
        public SpriteHitbox hitbox(ActorAnimation motion,float elapsed,float x,float y,float bodySize) {
            var direction=motion.dominantDirection();int index=index(elapsed);float size=bodySize*EquippedArt.DRAW_SCALE;
            return hitboxes[direction.ordinal()*FLIGHT_FRAMES+index].place(x-size/2,y-size/2,size,size,direction.mirrored(),false);
        }
        public void draw(SpriteBatch batch,ActorAnimation motion,float elapsed,float x,float y,float bodySize) {
            float size=bodySize*EquippedArt.DRAW_SCALE,blend=motion.turnBlend();int index=index(elapsed);
            var color=batch.getColor();float r=color.r,g=color.g,b=color.b,a=color.a;
            if(blend<1) {
                batch.setColor(r,g,b,a*(1-blend));batch.draw(frame(motion.previousDirection(),index),x-size/2,y-size/2,size,size);
            }
            batch.setColor(r,g,b,a*blend);batch.draw(frame(motion.direction(),index),x-size/2,y-size/2,size,size);batch.setColor(r,g,b,a);
        }
    }
    @Override public void dispose(){atlas.dispose();}
}
