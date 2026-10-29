package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/** Loaded only for a cinematic, then released with its screen instead of occupying combat memory. */
public final class OpeningArt implements Disposable {
    private final TextureAtlas atlas=new TextureAtlas(Gdx.files.internal("cutscenes/opening.atlas"));
    private final TextureRegion[] portal=new TextureRegion[4];
    private boolean disposed;
    public OpeningArt() {
        for(Texture texture:atlas.getTextures())texture.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
        for(String name:new String[]{"background-home","background-styx","foreground-home","foreground-styx",
            "hero-idle","hero-reach","wife-idle","wife-resist","zeus-idle","zeus-cast","charon"})required(name);
        for(int index=0;index<portal.length;index++)portal[index]=required("portal-"+index);
        required("charon").flip(true,false); // He stands to the right of the protagonist in the river conversation.
    }
    private TextureRegion required(String name) {
        TextureRegion region=atlas.findRegion(name);
        if(region==null)throw new IllegalStateException("Missing opening illustration: "+name);
        return region;
    }
    public TextureRegion region(String name) {
        if(disposed)throw new IllegalStateException("Opening art already disposed");
        return required(name);
    }
    public TextureRegion portal(float progress) {
        if(disposed)throw new IllegalStateException("Opening art already disposed");
        if(!Float.isFinite(progress))throw new IllegalArgumentException("Invalid cinematic effect time");
        return portal[Math.max(0,Math.min(portal.length-1,(int)(progress*portal.length)))];
    }
    public boolean disposed(){return disposed;}
    @Override public void dispose(){if(disposed)return;disposed=true;atlas.dispose();}
}
