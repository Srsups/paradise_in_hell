package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Keeps the already-decided safe transition alive until its write succeeds. */
public final class CheckpointSaveErrorScreen extends ScreenAdapter {
    private final Main game;
    private final String message;
    private final Runnable retry;
    private final FitViewport viewport=new FitViewport(960,540);
    private final SpriteBatch batch=new SpriteBatch();
    private final ShapeRenderer shapes=new ShapeRenderer();
    private final BitmapFont font=UiTheme.bodyFont(),title=UiTheme.titleFont();
    private final Rectangle button=new Rectangle(280,125,400,54);
    private float delay=.3f;
    private boolean mouseReleased;
    public CheckpointSaveErrorScreen(Main game,String message,Runnable retry) {
        this.game=game;this.message=message;this.retry=retry;
    }
    @Override public void show(){Gdx.input.setInputProcessor(null);}
    @Override public void resize(int w,int h){viewport.update(w,h,true);}
    @Override public void render(float delta) {
        delay=Math.max(0,delay-Math.max(0,delta));
        mouseReleased|=!Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        Vector2 pointer=viewport.unproject(new Vector2(Gdx.input.getX(),Gdx.input.getY()));
        if(delay==0&&(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)||Gdx.input.isKeyJustPressed(Input.Keys.S)
            ||mouseReleased&&Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)&&button.contains(pointer))) {
            retry.run();return;
        }
        ScreenUtils.clear(UiTheme.INK);viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);shapes.begin(ShapeRenderer.ShapeType.Filled);
        UiTheme.panel(shapes,100,85,760,370,false);
        UiTheme.panel(shapes,button.x,button.y,button.width,button.height,button.contains(pointer));shapes.end();
        batch.setProjectionMatrix(viewport.getCamera().combined);batch.begin();
        title.getData().setScale(1);title.setColor(UiTheme.IVORY);title.draw(batch,"GRAVAÇÃO PENDENTE",150,411);
        font.getData().setScale(1.05f);font.setColor(Color.SALMON);font.draw(batch,message,150,358,660,8,true);
        String error=!game.profiles().error().isEmpty()?game.profiles().error():game.shared().error();
        font.getData().setScale(.95f);font.setColor(Color.LIGHT_GRAY);font.draw(batch,error,150,309,660,8,true);
        font.setColor(UiTheme.IVORY);
        font.draw(batch,"Sua tentativa está pausada. Tente gravar novamente para continuar.\nSe fechar o jogo, somente o último checkpoint gravado estará disponível.",150,247,660,8,true);
        font.draw(batch,"Tentar novamente [ENTER]",button.x+82,button.y+34);batch.end();
    }
    @Override public void dispose(){batch.dispose();shapes.dispose();font.dispose();title.dispose();}
}
