package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Shared presentation only: text, input and permanent rewards belong to their own models. */
public final class DialoguePanel {
    private static final float PADDING=14, GAP=18;
    private final Rectangle bounds;
    private final float portraitWidth, bodyScale;

    public DialoguePanel(float x,float y,float width,float height,float portraitWidth,float bodyScale) {
        this.bounds=new Rectangle(x,y,width,height);
        this.portraitWidth=portraitWidth;this.bodyScale=bodyScale;
    }
    public boolean contains(Vector2 point) { return bounds.contains(point); }
    public Rectangle bounds() { return new Rectangle(bounds); }
    public float textWidth() { return bounds.width-PADDING*2-portraitWidth-GAP; }
    public float textHeight() { return bounds.height-116; }
    public float bodyScale() { return bodyScale; }
    private float textX() { return bounds.x+PADDING+portraitWidth+GAP; }

    /** Caller owns the filled shape pass. */
    public void background(ShapeRenderer shapes) {
        UiTheme.panel(shapes,bounds.x,bounds.y,bounds.width,bounds.height,true);
        UiTheme.panel(shapes,bounds.x+PADDING,bounds.y+PADDING,portraitWidth,bounds.height-PADDING*2,false);
    }
    /** Caller owns the sprite pass and fonts; all portrait regions are borrowed. */
    public void draw(SpriteBatch batch,BitmapFont titleFont,BitmapFont font,PilotArt art,
                     DialoguePlayback playback,String hint,String footer) {
        var portrait=art.expanded().region(playback.current().portrait().regionName());
        float scale=Math.min((portraitWidth-12)/portrait.getRegionWidth(),
            (bounds.height-PADDING*2-12)/portrait.getRegionHeight());
        float width=portrait.getRegionWidth()*scale,height=portrait.getRegionHeight()*scale;
        batch.setColor(Color.WHITE);
        batch.draw(portrait,bounds.x+PADDING+(portraitWidth-width)/2,bounds.y+(bounds.height-height)/2,width,height);
        titleFont.getData().setScale(.65f);titleFont.setColor(UiTheme.IVORY);
        titleFont.draw(batch,playback.current().speaker(),textX(),bounds.y+bounds.height-25);
        font.getData().setScale(.85f);font.setColor(Color.LIGHT_GRAY);
        if(playback.lineCount()>1)font.draw(batch,(playback.line()+1)+" / "+playback.lineCount(),bounds.x+bounds.width-62,bounds.y+bounds.height-25);
        font.getData().setScale(bodyScale);font.setColor(UiTheme.IVORY);
        font.draw(batch,playback.visibleText(),textX(),bounds.y+bounds.height-61,textWidth(),8,true);
        font.getData().setScale(.86f);font.setColor(UiTheme.TEAL);
        font.draw(batch,hint,textX(),bounds.y+44,textWidth(),8,true);
        font.getData().setScale(.82f);font.setColor(Color.LIGHT_GRAY);
        font.draw(batch,footer,textX(),bounds.y+21,textWidth(),8,true);
        if(playback.complete()) {
            font.setColor(Color.GOLD);font.draw(batch,">",bounds.x+bounds.width-24,bounds.y+21);
        }
        batch.setColor(Color.WHITE);font.setColor(Color.WHITE);font.getData().setScale(1);
    }
}
