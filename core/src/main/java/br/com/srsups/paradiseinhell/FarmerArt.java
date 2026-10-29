package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.Map;

/** Shared M5 drawings; the owner disposes the atlas once, borrowers only sample regions. */
public final class FarmerArt implements com.badlogic.gdx.utils.Disposable {
    private final TextureAtlas atlas=new TextureAtlas(Gdx.files.internal("visual/m5.atlas"));
    TextureAtlas atlas(){return atlas;}
    void appendTo(Map<String,TextureRegion> regions){
        for(var region:atlas.getRegions())regions.put(region.name,region);
    }
    void appendLegacyTo(Map<String,TextureRegion> regions){
        for(int direction=0;direction<4;direction++)for(int frame=0;frame<6;frame++){
            var facing=ActorAnimation.Direction.values()[direction];
            String source="farmer-rake-walk-"+facing.authored().key()+"-"+frame;
            var region=new TextureRegion(regions.get(source));
            if(facing.mirrored())region.flip(true,false);
            regions.put("class-fazendeiro-"+facing.key()+"-"+frame,region);
        }
    }
    @Override public void dispose(){atlas.dispose();}
}
