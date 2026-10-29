package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Boundary blocks stay entirely outside the already solid arena/refuge bounds. */
public final class SceneryVisuals {
    private SceneryVisuals() {}
    public static void boundary(SpriteBatch batch,SceneryArt art,SceneryTheme theme,float x,float y,float width,float height) {
        float size=Tile.TILE_SIZE;batch.setColor(Color.WHITE);
        for(float px=x-size;px<x+width+size;px+=size) {
            batch.draw(art.obstacle(theme,0),px,y-size,size,size);
            batch.draw(art.obstacle(theme,0),px,y+height,size,size);
        }
        for(float py=y;py<y+height;py+=size) {
            float span=Math.min(size,y+height-py);
            batch.draw(art.obstacle(theme,1),x-size,py,size,span);
            batch.draw(art.obstacle(theme,1),x+width,py,size,span);
        }
    }
    /** Borrow a centered crop, preserving the scene's aspect ratio instead of stretching it. */
    public static TextureRegion cover(TextureRegion image,float width,float height) {
        float target=width/height,source=image.getRegionWidth()/(float)image.getRegionHeight();
        int w=image.getRegionWidth(),h=image.getRegionHeight();
        if(source>target)w=Math.max(1,Math.round(h*target));
        else h=Math.max(1,Math.round(w/target));
        return new TextureRegion(image,(image.getRegionWidth()-w)/2,(image.getRegionHeight()-h)/2,w,h);
    }
}
