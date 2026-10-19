package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** One atlas for the application. Screens/actors borrow regions; Main owns disposal. */
public final class PilotArt implements com.badlogic.gdx.utils.Disposable {
    private final TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("visual/pilot.atlas"));
    private final java.util.Map<String,TextureRegion> regions = new java.util.HashMap<>();
    private final TextureRegion[][] actors = new TextureRegion[2][16];
    private final SpriteHitbox.Shape[][] masks = new SpriteHitbox.Shape[2][16];
    private boolean disposed;
    private final ExpandedArt expanded=new ExpandedArt();
    public ExpandedArt expanded() { return expanded; }
    private final EquippedArt equipped=new EquippedArt();
    public EquippedArt equipped() { return equipped; }
    public TextureRegion boonIcon(Upgrade boon) { return expanded.boon(boon); }
    public PilotArt() {
        for(TextureAtlas.AtlasRegion region:atlas.getRegions())regions.put(region.name,region);
        var texture=atlas.getTextures().first(); var data=texture.getTextureData();
        if(!data.isPrepared())data.prepare(); Pixmap pixels=data.consumePixmap();
        try {
            for(int actor=0;actor<2;actor++)for(int i=0;i<16;i++) {
                String name=(actor==0?"hero":"common")+"-"+ActorAnimation.Direction.values()[i/4].name().toLowerCase(java.util.Locale.ROOT)+"-"+i%4;
                TextureRegion frame=actors[actor][i]=region(name);
                int x=frame.getRegionX(),y=frame.getRegionY();
                masks[actor][i]=SpriteHitbox.Shape.fromAlpha(frame.getRegionWidth(),frame.getRegionHeight(),
                    (px,py)->pixels.getPixel(x+px,y+py)&255);
            }
        } finally {if(data.disposePixmap())pixels.dispose();}
    }
    public TextureRegion region(String name) {
        if(disposed)throw new IllegalStateException("Disposed visuals");
        TextureRegion region=regions.get(name);if(region==null)throw new IllegalArgumentException("Missing visual "+name);
        return region;
    }
    public TextureRegion actorFrame(boolean hero,ActorAnimation animation) {
        return actors[hero?0:1][animation.direction().cardinal().ordinal()*4+animation.frame()];
    }
    public SpriteHitbox[] actorMasks(boolean hero) {
        SpriteHitbox[] result=new SpriteHitbox[16];
        for(int i=0;i<16;i++)result[i]=new SpriteHitbox(masks[hero?0:1][i]);return result;
    }
    public SpriteHitbox actorMask(SpriteHitbox[] masks,ActorAnimation animation,float x,float y) {
        return masks[animation.direction().cardinal().ordinal()*4+animation.frame()].place(x-1,y-1,18,18,false,false);
    }
    public TextureRegion effect(String name,float progress) {return region(name+"-"+Math.min(3,Math.max(0,(int)(progress*4))));}
    public boolean disposed() { return disposed; }
    @Override public void dispose() {if(!disposed){disposed=true;atlas.dispose();expanded.dispose();equipped.dispose();regions.clear();}}
}

