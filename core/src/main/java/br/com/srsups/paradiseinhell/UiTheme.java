package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Shared typography and panel styling; each returned font belongs to its screen. */
public final class UiTheme {
    public static final Color INK = new Color(.035f,.052f,.075f,1);
    public static final Color PANEL = new Color(.075f,.115f,.15f,1);
    public static final Color BRONZE = new Color(.62f,.44f,.22f,1);
    public static final Color IVORY = new Color(.94f,.91f,.81f,1);
    public static final Color TEAL = new Color(.26f,.64f,.67f,1);
    private UiTheme() {}
    public static BitmapFont bodyFont() { return font("body"); }
    public static BitmapFont titleFont() { return font("title"); }
    private static BitmapFont font(String name) {
        BitmapFont font = new BitmapFont(Gdx.files.internal("ui/fonts/" + name + ".fnt"));
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
        font.setUseIntegerPositions(false);
        return font;
    }
    /** Filled pass: thin bronze frame and inset slate, without translucent text backgrounds. */
    public static void panel(ShapeRenderer shapes,float x,float y,float width,float height,boolean selected) {
        shapes.setColor(selected ? BRONZE : new Color(.22f,.29f,.31f,1));
        shapes.rect(x,y,width,height);
        shapes.setColor(PANEL); shapes.rect(x+2,y+2,width-4,height-4);
        shapes.setColor(selected ? TEAL : BRONZE); shapes.rect(x+9,y+height-5,width-18,1);
    }
}

