package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.Objects;

/** A settled attempt's final pose. The arena has already released its gameplay resources. */
public final class RunEndingScreen extends ScreenAdapter {
    public record Frame(TextureRegion background,String actor,ActorAnimation.Direction direction,
                        float x,float y,float bodySize,int width,int height) {}
    private final Main game;
    private final Frame frame;
    private final float duration;
    private final Screen destination;
    private final FitViewport viewport;
    private SpriteBatch batch;
    private BitmapFont font;
    private float elapsed;
    private boolean paused,delivered,disposed;

    /** Render only the surviving scenery before copying it; no entity or simulation is retained. */
    public static Frame capture(FitViewport world,Runnable drawBackground,String actor,
                                ActorAnimation.Direction direction,float x,float y,float bodySize) {
        Objects.requireNonNull(world);Objects.requireNonNull(drawBackground);
        Objects.requireNonNull(actor);Objects.requireNonNull(direction);
        drawBackground.run();
        int width=Gdx.graphics.getBackBufferWidth(),height=Gdx.graphics.getBackBufferHeight();
        float scaleX=width/(float)Gdx.graphics.getWidth(),scaleY=height/(float)Gdx.graphics.getHeight();
        Vector2 center=world.project(new Vector2(x,y));
        Vector2 edge=world.project(new Vector2(x+bodySize,y));
        TextureRegion background=ScreenUtils.getFrameBufferTexture(0,0,width,height);
        background.getTexture().setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        return new Frame(background,actor,direction,center.x*scaleX,center.y*scaleY,
            Math.abs(edge.x-center.x)*scaleX,width,height);
    }

    public RunEndingScreen(Main game,Frame frame,float duration,Screen destination) {
        this.game=Objects.requireNonNull(game);this.frame=Objects.requireNonNull(frame);
        if(!Float.isFinite(duration)||duration<=0)throw new IllegalArgumentException("Invalid ending duration");
        this.duration=duration;this.destination=Objects.requireNonNull(destination);
        viewport=new FitViewport(frame.width(),frame.height());
    }
    @Override public void show() {
        Gdx.input.setInputProcessor(null);
        batch=new SpriteBatch();font=UiTheme.bodyFont();
    }
    @Override public void resize(int width,int height){viewport.update(width,height,true);}
    @Override public void render(float delta) {
        if(disposed)return;
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))paused=!paused;
        if(!paused && Float.isFinite(delta) && delta>0)elapsed=Math.min(duration,elapsed+delta);
        ScreenUtils.clear(0.045f,0.055f,0.085f,1);
        viewport.apply();batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();batch.setColor(Color.WHITE);
        batch.draw(frame.background(),0,0,frame.width(),frame.height());
        game.visuals().reactions().draw(batch,frame.actor(),ActorReaction.State.DEATH,frame.direction(),
            elapsed/duration,frame.x(),frame.y(),frame.bodySize());
        if(paused) {
            font.getData().setScale(frame.width()/960f);font.setColor(Color.GOLD);
            font.draw(batch,"PAUSA — ESC: continuar",frame.width()*.38f,frame.height()*.92f);
        }
        batch.end();
        if(elapsed>=duration && !paused) {
            delivered=true;
            game.setScreen(destination);
        }
    }
    @Override public void pause(){paused=true;}
    @Override public void dispose() {
        if(disposed)return;
        disposed=true;
        if(batch!=null)batch.dispose();
        if(font!=null)font.dispose();
        frame.background().getTexture().dispose();
        if(!delivered)destination.dispose();
    }
}
