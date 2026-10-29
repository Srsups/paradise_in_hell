package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.function.Supplier;

/** Standalone menu entry. Active rooms keep their own screen and embed the panel. */
public final class ControlsSettingsScreen extends ScreenAdapter {
    private final Main game;
    private final Supplier<Screen> returnTo;
    private final ControlsPanel panel;
    private final FitViewport viewport=new FitViewport(960,540);
    private final SpriteBatch batch=new SpriteBatch();
    private final ShapeRenderer shapes=new ShapeRenderer();
    private final BitmapFont font=UiTheme.bodyFont();
    public ControlsSettingsScreen(Main game,Supplier<Screen> returnTo) {
        this.game=game;this.returnTo=returnTo;panel=new ControlsPanel(game);panel.show();
    }
    @Override public void show(){Gdx.input.setInputProcessor(null);}
    @Override public void resize(int width,int height){viewport.update(width,height,true);}
    @Override public void render(float delta) {
        panel.input(delta,viewport.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY())));
        if(!panel.open()){game.setScreen(returnTo.get());return;}
        ScreenUtils.clear(UiTheme.INK);viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);shapes.setProjectionMatrix(viewport.getCamera().combined);
        panel.draw(batch,shapes,font);
    }
    @Override public void dispose(){panel.close();batch.dispose();shapes.dispose();font.dispose();}
}
